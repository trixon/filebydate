/*
 * Copyright 2018 Patrik Karlström.
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
package se.trixon.filebydate;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.Xlog;

/**
 *
 * @author Patrik Karlström
 */
public class Operation {

    private static final Logger LOGGER = Logger.getLogger(Operation.class.getName());

    private final ResourceBundle mBundle;
    private final List<Exception> mExceptions = new ArrayList<>();
    private final List<File> mFiles = new ArrayList<>();
    private boolean mInterrupted;
    private final OperationListener mListener;
    private final Profile mProfile;

    public Operation(OperationListener operationListener, Profile profile) {
        mListener = operationListener;
        mProfile = profile;
        mBundle = SystemHelper.getBundle(Operation.class, "Bundle");
    }

    public void start() {
        mListener.onOperationStarted();
        long startTime = System.currentTimeMillis();
        mInterrupted = !generateFileList();
        String status;

        if (!mInterrupted && !mFiles.isEmpty()) {
            mListener.onOperationLog(String.format(mBundle.getString("found_count"), mFiles.size()));
            mListener.onOperationLog("");
            status = Dict.PROCESSING.toString();
            mListener.onOperationLog(status);

            for (File sourceFile : mFiles) {
                try {
                    SimpleDateFormat simpleDateFormat = mProfile.getDateFormat();

                    String fileDate = simpleDateFormat.format(getDate(sourceFile));
                    File destDir = new File(mProfile.getDestDir(), fileDate);

                    if (destDir.isFile()) {
                        mListener.onOperationLog(String.format(Dict.Dialog.ERROR_DEST_DIR_IS_FILE.toString(), destDir.getAbsolutePath()));
                        break;
                    } else if (!destDir.exists() && !mProfile.isDryRun()) {
                        FileUtils.forceMkdir(destDir);
                    }

                    String destFilename = sourceFile.getName();
                    String base = FilenameUtils.getBaseName(destFilename);
                    String ext = FilenameUtils.getExtension(destFilename);
                    NameCase caseBase = mProfile.getCaseBase();
                    NameCase caseExt = mProfile.getCaseExt();

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

                    File destFile = new File(destDir, destFilename);
                    String log;
                    if (destFile.exists() && !mProfile.isReplaceExisting()) {
                        log = String.format(Dict.Dialog.ERROR_DEST_FILE_EXISTS.toString(), destFile.getAbsolutePath());
                    } else {
                        Command command = mProfile.getCommand();
                        String cmd = command == Command.COPY ? "cp" : "mv";
                        log = String.format("%s %s  %s", cmd, sourceFile.getAbsolutePath(), destFile.toString());

                        if (destDir.canWrite()) {
                            if (!mProfile.isDryRun()) {
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
                        } else if (!mProfile.isDryRun()) {
                            log = Dict.Dialog.ERROR_DEST_CANT_WRITE.toString();
                        }
                    }

                    mListener.onOperationLog(getMessage(log));
                } catch (IOException | ImageProcessingException | NullPointerException ex) {
                    mListener.onOperationLog(getMessage(ex.getLocalizedMessage()));
                }

                if (Thread.interrupted()) {
                    mInterrupted = true;
                    break;
                }
            }
        }

        if (mInterrupted) {
            status = Dict.TASK_ABORTED.toString();
            mListener.onOperationLog("\n" + status);
            mListener.onOperationInterrupted();
        } else {
            mExceptions.stream().forEach((exception) -> {
                mListener.onOperationLog(String.format("#%s", exception.getLocalizedMessage()));
            });
            long millis = System.currentTimeMillis() - startTime;
            long min = TimeUnit.MILLISECONDS.toMinutes(millis);
            long sec = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
            status = String.format("%s (%d %s, %d %s)", Dict.TASK_COMPLETED.toString(), min, Dict.TIME_MIN.toString(), sec, Dict.TIME_SEC.toString());
            mListener.onOperationFinished(status);

            if (!mProfile.isDryRun()) {
                mProfile.setLastRun(System.currentTimeMillis());
                try {
                    ProfileManager.getInstance().save();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private boolean generateFileList() {
        mListener.onOperationLog("");
        mListener.onOperationLog(Dict.GENERATING_FILELIST.toString());
        PathMatcher pathMatcher = mProfile.getPathMatcher();

        EnumSet<FileVisitOption> fileVisitOptions = EnumSet.noneOf(FileVisitOption.class);
        if (mProfile.isFollowLinks()) {
            fileVisitOptions = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        }

        File file = mProfile.getSourceDir();
        if (file.isDirectory()) {
            FileVisitor fileVisitor = new FileVisitor(pathMatcher, mFiles, this);
            try {
                if (mProfile.isRecursive()) {
                    Files.walkFileTree(file.toPath(), fileVisitOptions, Integer.MAX_VALUE, fileVisitor);
                } else {
                    Files.walkFileTree(file.toPath(), fileVisitOptions, 1, fileVisitor);
                }

                if (fileVisitor.isInterrupted()) {
                    return false;
                }
            } catch (IOException ex) {
                Xlog.e(getClass(), ex.getLocalizedMessage());
            }
        } else if (file.isFile() && pathMatcher.matches(file.toPath().getFileName())) {
            mFiles.add(file);
        }

        if (mFiles.isEmpty()) {
            mListener.onOperationLog(Dict.FILELIST_EMPTY.toString());
        } else {
            Collections.sort(mFiles);
        }

        return true;
    }

    private Date getDate(File sourceFile) throws IOException, ImageProcessingException {
        Date date = new Date(System.currentTimeMillis());
        DateSource dateSource = mProfile.getDateSource();

        if (dateSource == DateSource.FILE_CREATED) {
            BasicFileAttributes attr = Files.readAttributes(sourceFile.toPath(), BasicFileAttributes.class);
            date = new Date(attr.creationTime().toMillis());
        } else if (dateSource == DateSource.FILE_MODIFIED) {
            BasicFileAttributes attr = Files.readAttributes(sourceFile.toPath(), BasicFileAttributes.class);
            date = new Date(attr.lastModifiedTime().toMillis());
        } else if (dateSource == DateSource.EXIF_ORIGINAL) {
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

        return date;
    }

    private String getMessage(String message) {
        if (mProfile.isDryRun()) {
            message = String.format("dry-run: %s", message);
        }

        return StringUtils.defaultString(message, "");
    }

    OperationListener getListener() {
        return mListener;
    }

    public enum Command {

        COPY, MOVE;

        @Override
        public String toString() {
            return Dict.valueOf(name()).toString();
        }
    }
}
