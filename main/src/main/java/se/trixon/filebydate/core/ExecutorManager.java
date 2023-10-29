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

import java.util.HashMap;
import javafx.scene.Scene;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import se.trixon.almond.nbp.fx.FxDialogPanel;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.swing.SwingHelper;
import se.trixon.filebydate.ui.TaskSummary;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class ExecutorManager {

    private final HashMap<String, TaskExecutor> mTaskExecutors = new HashMap<>();

    public static ExecutorManager getInstance() {
        return Holder.INSTANCE;
    }

    private ExecutorManager() {
    }

    public HashMap<String, TaskExecutor> getTaskExecutors() {
        return mTaskExecutors;
    }

    public void requestStart(Task task) {
        var taskSummary = new TaskSummary(task);
        var dialogPanel = new FxDialogPanel() {
            @Override
            protected void fxConstructor() {
                setScene(new Scene(taskSummary));
            }
        };
        dialogPanel.setPreferredSize(SwingHelper.getUIScaledDim(480, 200));

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
            dialogPanel.initFx(() -> {
            });
            SwingHelper.runLaterDelayed(100, () -> dryRunButton.requestFocus());
            var result = DialogDisplayer.getDefault().notify(d);

            if (result == Dict.RUN.toString()) {
                start(task, false);
            } else if (result == dryRunButton) {
                start(task, true);
            }
        });
    }

    public void start(Task task, boolean dryRun) {
        System.out.println("START");
        System.out.println(task.getName());
        System.out.println(dryRun);
        var taskExecutor = new TaskExecutor(task, dryRun);
        mTaskExecutors.put(task.getId(), taskExecutor);
        taskExecutor.run();

//        if (task.isValid()) {
//            mLastRunProfile = profile;
//            mOperationThread = new Thread(() -> {
//                Operation operation = new Operation(mOperationListener, profile);
//                operation.start();
//            });
//            mOperationThread.setName("Operation");
//            mOperationThread.start();
//        } else {
//            mStatusPanel.out(profile.toDebugString());
//            mStatusPanel.out(profile.getValidationError());
//            mStatusPanel.out(Dict.ABORTING.toString());
//        }
    }

    private static class Holder {

        private static final ExecutorManager INSTANCE = new ExecutorManager();
    }
}