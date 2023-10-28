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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import se.trixon.almond.util.fx.FxHelper;

/**
 *
 * @author Patrik Karlström
 */
public class RunStateManager {

    private final ObjectProperty<Task> mTaskProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<RunState> mRunStateProperty = new SimpleObjectProperty<>();
    private final BooleanProperty mRunningProperty = new SimpleBooleanProperty(false);

    public static RunStateManager getInstance() {
        return Holder.INSTANCE;
    }

    private RunStateManager() {
    }

    public Task getTask() {
        return mTaskProperty.get();
    }

    public RunState getRunState() {
        return mRunStateProperty.get();
    }

    public boolean isRunning() {
        return mRunningProperty.get();
    }

    public ObjectProperty<Task> taskProperty() {
        return mTaskProperty;
    }

    public ObjectProperty<RunState> runStateProperty() {
        return mRunStateProperty;
    }

    public BooleanProperty runningProperty() {
        return mRunningProperty;
    }

    public void setTask(Task task) {
        mTaskProperty.set(task);
    }

    public void setRunState(RunState runState) {
        FxHelper.runLater(() -> {
            mRunStateProperty.set(runState);
            mRunningProperty.set(runState == RunState.CANCELABLE);
        });
    }

    private static class Holder {

        private static final RunStateManager INSTANCE = new RunStateManager();
    }
}
