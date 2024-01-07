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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.NbBundle;
import se.trixon.almond.util.Dict;
import se.trixon.filebydate.core.parts.Command;
import se.trixon.filebydate.core.parts.NameCase;

/**
 *
 * @author Patrik Karlström
 */
public class Operation {

    private final ResourceBundle mBundle = NbBundle.getBundle(Task.class);
    private final List<File> mFiles = new ArrayList<>();
    private boolean mInterrupted;
    private final Printer mPrinter;
    private final ProgressHandle mProgressHandle;
    private final Task mTask;

    public Operation(Task task, Printer printer, ProgressHandle progressHandle) {
        mTask = task;
        mPrinter = printer;
        mProgressHandle = progressHandle;
    }

    public boolean isInterrupted() {
        return mInterrupted;
    }

    public void start() {

        if (!mInterrupted && !mFiles.isEmpty()) {
            mPrinter.outln(String.format(mBundle.getString("found_count"), mFiles.size()));
            mPrinter.outln("");
            mPrinter.outln(Dict.PROCESSING.toString());
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
                        mPrinter.outln(String.format(Dict.Dialog.ERROR_DEST_DIR_IS_FILE.toString(), destDir.getAbsolutePath()));
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
                        se.trixon.filebydate.core.parts.Command command = mTask.getCommand();
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

                    mPrinter.outln(getMessage(log));
                } catch (IOException | ImageProcessingException | NullPointerException ex) {
                    mPrinter.outln(getMessage(ex.getLocalizedMessage()));
                }

                mProgressHandle.progress(++progress);
            }
        }
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

}
