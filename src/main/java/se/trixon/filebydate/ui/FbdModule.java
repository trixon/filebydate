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
package se.trixon.filebydate.ui;

import com.dlsc.workbenchfx.Workbench;
import com.dlsc.workbenchfx.model.WorkbenchModule;
import javafx.application.Platform;
import javafx.scene.Node;
import se.trixon.almond.util.icons.material.MaterialIcon;
import static se.trixon.filebydate.ui.FbdApp.MODULE_ICON_SIZE;
import se.trixon.filebydate.ui.FbdView.RunState;

/**
 *
 * @author Patrik Karlström
 */
public class FbdModule extends WorkbenchModule {

    private FbdView mFbdView;
    private final FbdApp mApp;

    public FbdModule(FbdApp app) {
        super(null, MaterialIcon._Action.DATE_RANGE.getImageView(MODULE_ICON_SIZE).getImage());
        mApp = app;
    }

    @Override
    public Node activate() {
        return mFbdView;
    }

    public FbdView getFbdView() {
        return mFbdView;
    }

    @Override
    public void init(Workbench workbench) {
        super.init(workbench);
        mFbdView = new FbdView(mApp, workbench, this);
        mApp.setFbdView(mFbdView);

        setRunningState(RunState.STARTABLE);
    }

    public void setRunningState(RunState runState) {
        Platform.runLater(() -> {
            switch (runState) {
                case STARTABLE:
                    getToolbarControlsLeft().setAll( //                            mLogToolbarItem
                            );
                    getToolbarControlsRight().setAll( //                            mAddToolbarItem
                            );

//                mOptionsAction.setDisabled(false);
                    break;

                case CANCELABLE:
                    getToolbarControlsLeft().setAll( //                            mHomeToolbarItem
                            );
                    getToolbarControlsRight().setAll( //                            mCancelToolbarItem
                            );
//                    mHomeToolbarItem.setDisable(true);
//                mOptionsAction.setDisabled(true);
                    break;

                case CLOSEABLE:
                    getToolbarControlsLeft().setAll( //                            mHomeToolbarItem
                            );

                    getToolbarControlsRight().setAll( //                            mRunToolbarItem
                            );

//                    mHomeToolbarItem.setDisable(false);
//                mOptionsAction.setDisabled(false);
                    break;

                default:
                    throw new AssertionError();
            }
        });
    }
}
