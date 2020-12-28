/*
 * Copyright 2020 Patrik Karlström.
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 *
 * @author Patrik Karlström
 */
public class RunStateManager {

    private final ObjectProperty<RunState> mRunStateProperty = new SimpleObjectProperty<>();

    private RunStateManager() {
    }

    public static RunStateManager getInstance() {
        return Holder.INSTANCE;
    }

    public ObjectProperty<RunState> runStateProperty() {
        return mRunStateProperty;
    }

    public RunState getRunState() {
        return mRunStateProperty.get();
    }

    public void setRunState(RunState runState) {
        mRunStateProperty.set(runState);
    }

    private static class Holder {

        private static final RunStateManager INSTANCE = new RunStateManager();
    }
}
