/* 
 * Copyright 2023 Patrik Karlström <patrik@trixon.se>.
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
import org.openide.util.NbBundle;
import org.openide.windows.InputOutput;
import se.trixon.almond.util.Dict;

/**
 *
 * @author Patrik Karlström
 */
public class Operation {

    private final ResourceBundle mBundle = NbBundle.getBundle(Operation.class);
    private final List<Exception> mExceptions = new ArrayList<>();
    private final List<File> mFiles = new ArrayList<>();
    private boolean mInterrupted;
    private final ProgressHandle mProgressHandle;
    private final Task mTask;
    private final InputOutput mInputOutput;

    public Operation(Task task, InputOutput inputOutput, ProgressHandle progressHandle) {
        mTask = task;
        mInputOutput = inputOutput;
        mProgressHandle = progressHandle;
    }

    public void start() {
        long startTime = System.currentTimeMillis();

        mInterrupted = !generateFileList();
        String status;

        if (!mInterrupted && !mFiles.isEmpty()) {
            mInputOutput.getOut().println(String.format(mBundle.getString("found_count"), mFiles.size()));
            mInputOutput.getOut().println("");
            status = Dict.PROCESSING.toString();
            mInputOutput.getOut().println(status);
            mProgressHandle.switchToDeterminate(mFiles.size());
            int progress = 0;
            var taskDateFormat = mTask.getDateFormat();

            for (var sourceFile : mFiles) {
                mProgressHandle.progress(sourceFile.getName());
                try {
                    try {
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (InterruptedException ex) {
                        mInterrupted = true;
                        break;
                    }

                    var fileDate = taskDateFormat.format(getDate(sourceFile));
                    var destDir = new File(mTask.getDestDir(), fileDate);

                    if (destDir.isFile()) {
                        mInputOutput.getOut().println(String.format(Dict.Dialog.ERROR_DEST_DIR_IS_FILE.toString(), destDir.getAbsolutePath()));
                        break;
                    } else if (!destDir.exists() && !mTask.isDryRun()) {
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
                        var command = mTask.getCommand();
                        String cmd = command == Command.COPY ? "cp" : "mv";
                        log = String.format("%s %s  %s", cmd, sourceFile.getAbsolutePath(), destFile.toString());

                        if (destDir.canWrite()) {
                            if (!mTask.isDryRun()) {
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
                        } else if (!mTask.isDryRun()) {
                            log = Dict.Dialog.ERROR_DEST_CANT_WRITE.toString();
                        }
                    }

                    mInputOutput.getOut().println(getMessage(log));
                } catch (IOException | ImageProcessingException | NullPointerException ex) {
                    mInputOutput.getOut().println(getMessage(ex.getLocalizedMessage()));
                }

                mProgressHandle.progress(++progress);
            }
        }

        if (mInterrupted) {
            status = Dict.TASK_ABORTED.toString();
            mInputOutput.getErr().println("\n" + status);
        } else {
            mExceptions.forEach(exception -> {
                mInputOutput.getOut().println(String.format("#%s", exception.getLocalizedMessage()));
            });
            long millis = System.currentTimeMillis() - startTime;
            long min = TimeUnit.MILLISECONDS.toMinutes(millis);
            long sec = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
            status = String.format("%s (%d %s, %d %s)", Dict.TASK_COMPLETED.toString(), min, Dict.TIME_MIN.toString(), sec, Dict.TIME_SEC.toString());
            mInputOutput.getOut().println();
            mInputOutput.getOut().println(status);

            if (!mTask.isDryRun()) {
                mTask.setLastRun(System.currentTimeMillis());
                StorageManager.save();
            }
        }
    }

    private boolean generateFileList() {
        mInputOutput.getOut().println();
        mInputOutput.getOut().println(Dict.GENERATING_FILELIST.toString());

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
        if (mTask.isDryRun()) {
            message = String.format("dry-run: %s", message);
        }

        return Objects.toString(message, "");
    }

    public enum Command {

        COPY, MOVE;

        @Override
        public String toString() {
            return Dict.valueOf(name()).toString();
        }
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
                TimeUnit.NANOSECONDS.sleep(1);
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
