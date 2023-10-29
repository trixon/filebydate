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

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.icons.material.swing.MaterialIcon;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class TaskExecutor implements Runnable {

    private final boolean mDryRun;
    private String mDryRunIndicator = "";

    private final InputOutput mInputOutput;
    private long mLastRun;
    private ProgressHandle mProgressHandle;

    private final Task mTask;

    public TaskExecutor(Task task, boolean dryRun) {
        mTask = task;
        mDryRun = dryRun;
        AbstractAction abstractAction = new AbstractAction("xyz", MaterialIcon._Action.ACCESSIBILITY.getImageIcon(36)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("action performed");
            }

        };
        var actions = new Action[]{abstractAction};

        mInputOutput = IOProvider.getDefault().getIO(mTask.getName(), false, actions, null);

        if (mDryRun) {
            mDryRunIndicator = String.format(" (%s)", Dict.DRY_RUN.toString());
        }

        try {
            mInputOutput.getOut().reset();
            mInputOutput.select();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    @Override
    public void run() {
        var allowToCancel = (Cancellable) () -> {
            //TODO kill thread...
            System.out.println("request CANCEL");
            mInputOutput.getErr().println("cancelet");

            mProgressHandle.finish();

            return true;
        };
        var ii = MaterialIcon._Action.ACCESSIBILITY.getImageIcon(16);
        AbstractAction abstractAction = new AbstractAction("xyz", ii) {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("action performed");
            }

        };
        mProgressHandle = ProgressHandle.createHandle(mTask.getName(), allowToCancel, abstractAction);
        mProgressHandle.addDefaultAction(abstractAction);
        mLastRun = System.currentTimeMillis();

//        appendHistoryFile(getHistoryLine(mTask.getId(), Dict.STARTED.toString(), mDryRunIndicator));
        String s = "%s %s %s.".formatted(now(), Dict.START.toString(), mTask.getName());
        mInputOutput.getOut().println(s);
        mInputOutput.getErr().println(String.format("\n\n%s", Dict.JOB_FAILED.toString()));
        mProgressHandle.start();
        mProgressHandle.switchToIndeterminate();
        mProgressHandle.progress("567");
        new Thread(() -> {
            try {
                Thread.sleep(100 * 1000);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            mInputOutput.getOut().println("DONE");
            mProgressHandle.finish();
        }).start();

//        var jobExecuteSection = mTask.getExecuteSection();
//        try {
//            // run before first task
//            run(jobExecuteSection.getBefore(), "JobEditor.runBefore");
//
//            runTasks();
//
//            if (mNumOfFailedTasks == 0) {
//                // run after last task - if all ok
//                run(jobExecuteSection.getAfterOk(), "JobEditor.runAfterOk");
//            } else {
//                s = String.format(Dict.TASKS_FAILED.toString(), mNumOfFailedTasks);
//                mInputOutput.getErr().println(s);
//
//                // run after last task - if any failed
//                run(jobExecuteSection.getAfterFail(), "JobEditor.runAfterFail");
//            }
//
//            // run after last task
//            run(jobExecuteSection.getAfter(), "JobEditor.runAfter");
//
//            appendHistoryFile(getHistoryLine(mTask.getId(), Dict.DONE.toString(), mDryRunIndicator));
//            s = String.format("%s %s: %s", Jota.nowToDateTime(), Dict.DONE.toString(), Dict.JOB.toString());
//            mInputOutput.getOut().println(s);
//            updateJobStatus(0);
//            writelogs();
//            mInputOutput.getOut().println(String.format(Dict.JOB_FINISHED.toString(), mTask.getName()));
//        } catch (InterruptedException ex) {
//            appendHistoryFile(getHistoryLine(mTask.getId(), Dict.CANCELED.toString(), mDryRunIndicator));
//            updateJobStatus(99);
//            writelogs();
//        } catch (IOException ex) {
//            writelogs();
//            Exceptions.printStackTrace(ex);
//        } catch (ExecutionFailedException ex) {
//            //Logger.getLogger(JobExecutor.class.getName()).log(Level.SEVERE, null, ex);
//            //send(ProcessEvent.OUT, "before failed and will not continue");
//            appendHistoryFile(getHistoryLine(mTask.getId(), Dict.FAILED.toString(), mDryRunIndicator));
//            updateJobStatus(1);
//            writelogs();
//            mInputOutput.getErr().println(String.format("\n\n%s", Dict.JOB_FAILED.toString()));
//        }
        ExecutorManager.getInstance().getTaskExecutors().remove(mTask.getId());
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss"));
    }

}
