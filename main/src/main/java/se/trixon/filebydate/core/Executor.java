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
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Cancellable;
import org.openide.windows.FoldHandle;
import org.openide.windows.IOFolding;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import se.trixon.almond.nbp.output.OutputHelper;
import se.trixon.almond.nbp.output.OutputLineMode;
import se.trixon.almond.util.Dict;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class Executor implements Runnable {

    private final boolean mDryRun;

    private String mDryRunIndicator = "";
    private final InputOutput mInputOutput;
    private boolean mInterrupted;
    private long mLastRun;
    private FoldHandle mMainFoldHandle;
    private Thread mExecutorThread;
    private OutputHelper mOutputHelper;
    private ProgressHandle mProgressHandle;
    private final StatusDisplayer mStatusDisplayer = StatusDisplayer.getDefault();
    private final Task mTask;
    private final List<File> mFiles = new ArrayList<>();

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

            if (!mInterrupted) {
                jobEnded(OutputLineMode.OK, Dict.DONE.toString());

                if (!mTask.isDryRun()) {
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
