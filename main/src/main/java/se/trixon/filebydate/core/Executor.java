/*
 * Copyright 2024 Patrik Karlström <patrik@trixon.se>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.trixon.filebydate.core;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;
import org.openide.windows.FoldHandle;
import org.openide.windows.IOFolding;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import se.trixon.almond.nbp.output.OutputHelper;
import se.trixon.almond.nbp.output.OutputLineMode;
import se.trixon.almond.util.Dict;
import se.trixon.filebydate.core.parts.Command;
import static se.trixon.filebydate.core.parts.DateSource.EXIF_ORIGINAL;
import static se.trixon.filebydate.core.parts.DateSource.FILE_CREATED;
import static se.trixon.filebydate.core.parts.DateSource.FILE_MODIFIED;
import se.trixon.filebydate.core.parts.NameCase;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class Executor implements Runnable {

    private final ResourceBundle mBundle = NbBundle.getBundle(Task.class);

    private final boolean mDryRun;

    private String mDryRunIndicator = "";
    private Thread mExecutorThread;
    private final List<File> mFiles = new ArrayList<>();
    private final InputOutput mInputOutput;
    private boolean mInterrupted;
    private long mLastRun;
    private FoldHandle mMainFoldHandle;
    private OutputHelper mOutputHelper;
    private ProgressHandle mProgressHandle;
    private final StatusDisplayer mStatusDisplayer = StatusDisplayer.getDefault();
    private final Task mTask;

    public Executor(Task task, boolean dryRun) {
        mTask = task;
        mDryRun = dryRun;
        mInputOutput = IOProvider.getDefault().getIO(mTask.getName(), false);
        mInputOutput.select();

        if (mDryRun) {
            mDryRunIndicator = String.format(" (%s)", Dict.DRY_RUN.toString());
        }

        mOutputHelper = new OutputHelper(mTask.getName(), mInputOutput, mDryRun);
        mOutputHelper.reset();

        task.setOperation(task.getCommand().ordinal());
    }

    @Override
    public void run() {
        var allowToCancel = (Cancellable) () -> {
            mExecutorThread.interrupt();
            mInterrupted = true;
            mProgressHandle.finish();
            ExecutorManager.getInstance().getExecutors().remove(mTask.getId());
            jobEnded(OutputLineMode.WARNING, Dict.CANCELED.toString());

            return true;
        };

        mLastRun = System.currentTimeMillis();
        mProgressHandle = ProgressHandle.createHandle(mTask.getName(), allowToCancel);
        mProgressHandle.start();
        mProgressHandle.switchToIndeterminate();

        mExecutorThread = new Thread(() -> {
            mOutputHelper.start();
            mOutputHelper.printSectionHeader(OutputLineMode.INFO, Dict.START.toString(), Dict.TASK.toLower(), mTask.getName());
            mMainFoldHandle = IOFolding.startFold(mInputOutput, true);

            if (!mTask.isValid()) {
                mInputOutput.getErr().println(mTask.getValidationError());
                jobEnded(OutputLineMode.ERROR, Dict.INVALID_INPUT.toString());
                mInputOutput.getErr().println(String.format("\n\n%s", Dict.JOB_FAILED.toString()));

                return;
            }

            mInterrupted = !generateFileList();

            if (!mInterrupted && !mFiles.isEmpty()) {
                mOutputHelper.println(OutputLineMode.INFO, mBundle.getString("found_count").formatted(mFiles.size()));
                mInputOutput.getOut().println("");
                mOutputHelper.printSectionHeader(OutputLineMode.INFO, Dict.PROCESSING.toString(), null, null);

                mProgressHandle.switchToDeterminate(mFiles.size());
                int progress = 0;
                var taskDateFormat = mTask.getDateFormat();

                for (var sourceFile : mFiles) {
                    mProgressHandle.progress(sourceFile.getName());
                    try {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        } catch (InterruptedException ex) {
                            mInterrupted = true;
                            break;
                        }

                        var fileDate = taskDateFormat.format(getDate(sourceFile));
                        var destDir = new File(mTask.getDestDir(), fileDate);

                        if (destDir.isFile()) {
                            mInputOutput.getOut().println(String.format(Dict.Dialog.ERROR_DEST_DIR_IS_FILE.toString(), destDir.getAbsolutePath()));
                            break;
                        } else if (!destDir.exists() && !mDryRun) {
                            FileUtils.forceMkdir(destDir);
                        }

                        String destFilename = sourceFile.getName();
                        String base = FilenameUtils.getBaseName(destFilename);
                        String ext = FilenameUtils.getExtension(destFilename);
                        var caseBase = mTask.getCaseBase();
                        var caseExt = mTask.getCaseExt();

                        if (caseBase != NameCase.UNCHANGED || caseExt != NameCase.UNCHANGED) {
                            if (caseBase == NameCase.LOWER) {
                                base = base.toLowerCase();
                            } else if (caseBase == NameCase.UPPER) {
                                base = base.toUpperCase();
                            }

                            if (caseExt == NameCase.LOWER) {
                                ext = ext.toLowerCase();
                            } else if (caseBase == NameCase.UPPER) {
                                ext = ext.toUpperCase();
                            }

                            if (base.length() == 0) {
                                destFilename = String.format(".%s", ext);
                            } else if (ext.length() == 0) {
                                destFilename = base;
                            } else {
                                destFilename = String.format("%s.%s", base, ext);
                            }
                        }

                        var destFile = new File(destDir, destFilename);
                        String log;

                        if (destFile.exists() && !mTask.isReplaceExisting()) {
                            log = String.format(Dict.Dialog.ERROR_DEST_FILE_EXISTS.toString(), destFile.getAbsolutePath());
                        } else {
                            se.trixon.filebydate.core.parts.Command command = mTask.getCommand();
                            String cmd = command == Command.COPY ? "cp" : "mv";
                            log = String.format("%s %s  %s", cmd, sourceFile.getAbsolutePath(), destFile.toString());

                            if (destDir.canWrite()) {
                                if (!mDryRun) {
                                    if (command == Command.COPY) {
                                        FileUtils.copyFile(sourceFile, destFile);
                                    } else if (command == Command.MOVE) {
                                        if (File.listRoots().length > 1 || destFile.exists()) {
                                            FileUtils.copyFile(sourceFile, destFile);
                                            FileUtils.deleteQuietly(sourceFile);
                                        } else {
                                            FileUtils.moveFile(sourceFile, destFile);
                                        }
                                    }
                                }
                            } else if (!mDryRun) {
                                log = Dict.Dialog.ERROR_DEST_CANT_WRITE.toString();
                            }
                        }

                        mInputOutput.getOut().println(getMessage(log));
                    } catch (IOException | ImageProcessingException | NullPointerException ex) {
                        mInputOutput.getErr().println(getMessage(ex.getLocalizedMessage()));
                    }

                    mProgressHandle.progress(++progress);
                }
            }

            if (!mInterrupted) {
                jobEnded(OutputLineMode.OK, Dict.DONE.toString());

                if (!mDryRun) {
                    mTask.setLastRun(System.currentTimeMillis());
                    StorageManager.save();
                }
            }

            mProgressHandle.finish();
            ExecutorManager.getInstance().getExecutors().remove(mTask.getId());
        }, "Executor");

        mExecutorThread.start();
    }

    private boolean generateFileList() {
        mInputOutput.getOut().println();
        mOutputHelper.printSectionHeader(OutputLineMode.INFO, Dict.GENERATING_FILELIST.toString(), "", mTask.getSourceDirAsString());

        var fileVisitOptions = EnumSet.noneOf(FileVisitOption.class);
        if (mTask.isFollowLinks()) {
            fileVisitOptions = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        }

        var file = mTask.getSourceDir();
        if (file.isDirectory()) {
            var fileVisitor = new FileVisitor();
            try {
                if (mTask.isRecursive()) {
                    Files.walkFileTree(file.toPath(), fileVisitOptions, Integer.MAX_VALUE, fileVisitor);
                } else {
                    Files.walkFileTree(file.toPath(), fileVisitOptions, 1, fileVisitor);
                }

                if (fileVisitor.isInterrupted()) {
                    return false;
                }
            } catch (IOException ex) {
                mInputOutput.getErr().println(ex.getMessage());
            }
        } else if (file.isFile() && mTask.getPathMatcher().matches(file.toPath().getFileName())) {
            mFiles.add(file);
        }

        if (mFiles.isEmpty()) {
            mInputOutput.getOut().println(Dict.FILELIST_EMPTY.toString());
        } else {
            Collections.sort(mFiles);
        }

        return true;
    }

    private Date getDate(File sourceFile) throws IOException, ImageProcessingException {
        var date = new Date(System.currentTimeMillis());
        var dateSource = mTask.getDateSource();

        if (null != dateSource) {
            switch (dateSource) {
                case FILE_CREATED -> {
                    var attr = Files.readAttributes(sourceFile.toPath(), BasicFileAttributes.class);
                    date = new Date(attr.creationTime().toMillis());
                }
                case FILE_MODIFIED -> {
                    var attr = Files.readAttributes(sourceFile.toPath(), BasicFileAttributes.class);
                    date = new Date(attr.lastModifiedTime().toMillis());
                }
                case EXIF_ORIGINAL -> {
                    Metadata metadata;
                    Directory directory = null;
                    try {
                        metadata = ImageMetadataReader.readMetadata(sourceFile);
                        directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                        date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                    } catch (NullPointerException | ImageProcessingException ex) {
                        String message;
                        if (directory == null) {
                            message = String.format(Dict.Dialog.ERROR_EXIF_NOT_FOUND.toString(), sourceFile.getAbsolutePath());
                        } else {
                            message = String.format(Dict.Dialog.ERROR_FILE_FORMAT_NOT_SUPPORTED.toString(), sourceFile.getAbsolutePath());
                        }

                        throw new ImageProcessingException(message);
                    }
                }
            }
        }

        return date;
    }

    private String getMessage(String message) {
        if (mDryRun) {
            message = String.format("dry-run: %s", message);
        }

        return Objects.toString(message, "");
    }

    private void jobEnded(OutputLineMode outputLineMode, String action) {
        mMainFoldHandle.finish();
        mStatusDisplayer.setStatusText(action);
        mOutputHelper.printSummary(outputLineMode, action, Dict.TASK.toString());
    }

    public class FileVisitor extends SimpleFileVisitor<Path> {

        private boolean mInterrupted;

        public FileVisitor() {
        }

        public boolean isInterrupted() {
            return mInterrupted;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            try {
                TimeUnit.NANOSECONDS.sleep(10000000);
            } catch (InterruptedException ex) {
                mInterrupted = true;
                return FileVisitResult.TERMINATE;
            }

            mInputOutput.getOut().println(dir.toString());
            var filePaths = dir.toFile().list();

            if (filePaths != null && filePaths.length > 0) {
                for (var fileName : filePaths) {
                    try {
                        TimeUnit.NANOSECONDS.sleep(1);
                    } catch (InterruptedException ex) {
                        mInterrupted = true;
                        return FileVisitResult.TERMINATE;
                    }
                    var file = new File(dir.toFile(), fileName);
                    if (file.isFile() && mTask.getPathMatcher().matches(file.toPath().getFileName())) {
                        mFiles.add(file);
                    }
                }
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }

}
