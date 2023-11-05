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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import javafx.scene.Scene;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;
import org.openide.windows.IOProvider;
import se.trixon.almond.nbp.dialogs.NbMessage;
import se.trixon.almond.nbp.fx.FxDialogPanel;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.swing.SwingHelper;
import se.trixon.filebydate.ui.TaskSummary;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class ExecutorManager {

    private final ResourceBundle mBundle = NbBundle.getBundle(Task.class);
    private final HashMap<String, Executor> mExecutors = new HashMap<>();

    public static ExecutorManager getInstance() {
        return Holder.INSTANCE;
    }

    private ExecutorManager() {
    }

    public HashMap<String, Executor> getExecutors() {
        return mExecutors;
    }

    public void requestStart(Task task) {
        if (mExecutors.containsKey(task.getId())) {
            NbMessage.error(mBundle.getString("task_running_title"), mBundle.getString("task_running_message"));
        } else {
            var taskSummary = new TaskSummary(task);
            var dialogPanel = new FxDialogPanel() {
                @Override
                protected void fxConstructor() {
                    setScene(new Scene(taskSummary));
                }
            };
            dialogPanel.setPreferredSize(SwingHelper.getUIScaledDim(640, 300));

            SwingUtilities.invokeLater(() -> {
                var title = Dict.Dialog.TITLE_TASK_RUN_S.toString().formatted(task.getName());
                var dryRunButton = new JButton(Dict.DRY_RUN.toString());
                var d = new DialogDescriptor(
                        dialogPanel,
                        title,
                        true,
                        new Object[]{Dict.CANCEL.toString(), Dict.RUN.toString(), dryRunButton},
                        dryRunButton,
                        0,
                        null,
                        null
                );

                d.setValid(false);
                dialogPanel.setNotifyDescriptor(d);
                dialogPanel.initFx(null);
                SwingHelper.runLaterDelayed(100, () -> dryRunButton.requestFocus());
                var result = DialogDisplayer.getDefault().notify(d);

                if (result == Dict.RUN.toString()) {
                    start(task, false);
                } else if (result == dryRunButton) {
                    start(task, true);
                }
            });
        }
    }

    public void start(Task task, boolean dryRun) {
        task.setDryRun(dryRun);
        var taskExecutor = new Executor(task);
        mExecutors.put(task.getId(), taskExecutor);
        taskExecutor.run();
    }

    private static class Holder {

        private static final ExecutorManager INSTANCE = new ExecutorManager();
    }

    public class Executor implements Runnable {

        private String mDryRunIndicator = "";
        private long mLastRun;
        private Thread mOperationThread;
        private final Printer mPrinter;
        private ProgressHandle mProgressHandle;
        private final Task mTask;

        public Executor(Task task) {
            mTask = task;
            mPrinter = new Printer(IOProvider.getDefault().getIO(mTask.getName(), false));
            if (task.isDryRun()) {
                mDryRunIndicator = String.format(" (%s)", Dict.DRY_RUN.toString());
            }

            task.setOperation(task.getCommand().ordinal());
        }

        @Override
        public void run() {
            String s = "%s %s %s.".formatted(now(), Dict.START.toString(), mTask.getName());
            mPrinter.outln(s);

            if (!mTask.isValid()) {
                mPrinter.errln(mTask.getValidationError());
                return;
            }

            var allowToCancel = (Cancellable) () -> {
                mOperationThread.interrupt();
                mProgressHandle.finish();
                ExecutorManager.getInstance().getExecutors().remove(mTask.getId());

                return true;
            };
            mProgressHandle = ProgressHandle.createHandle(mTask.getName(), allowToCancel);
            mLastRun = System.currentTimeMillis();

            mProgressHandle.start();
            mProgressHandle.switchToIndeterminate();
            mOperationThread = new Thread(() -> {
                var operation = new Operation(mTask, mPrinter, mProgressHandle);
                var startTime = System.currentTimeMillis();
                operation.start();

                if (operation.isInterrupted()) {
                    mPrinter.errln("");
                    mPrinter.errln("%s %s".formatted(now(), Dict.TASK_ABORTED.toString()));
                } else {
                    long millis = System.currentTimeMillis() - startTime;
                    long min = TimeUnit.MILLISECONDS.toMinutes(millis);
                    long sec = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
                    var status = String.format("%s (%d %s, %d %s)", Dict.TASK_COMPLETED.toString(), min, Dict.TIME_MIN.toString(), sec, Dict.TIME_SEC.toString());
                    mPrinter.outln("");
                    mPrinter.outln("%s %s".formatted(now(), status));

                    if (!mTask.isDryRun()) {
                        mTask.setLastRun(System.currentTimeMillis());
                        StorageManager.save();
                    }
                }

                mProgressHandle.finish();
                ExecutorManager.getInstance().getExecutors().remove(mTask.getId());
            }, "Operation");
            mOperationThread.start();

        }

        private String now() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss"));
        }

    }
}
