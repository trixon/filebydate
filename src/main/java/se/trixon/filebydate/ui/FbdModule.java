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
import com.dlsc.workbenchfx.view.controls.ToolbarItem;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.icons.material.MaterialIcon;
import se.trixon.filebydate.ui.FbdView.RunState;
import static se.trixon.filebydate.ui.FbdApp.ICON_SIZE_TOOLBAR;
import static se.trixon.filebydate.ui.FbdApp.MODULE_ICON_SIZE;

/**
 *
 * @author Patrik Karlström
 */
public class FbdModule extends WorkbenchModule {

    private FbdView mView;
    private ToolbarItem mAddToolbarItem;
    private ToolbarItem mCancelToolbarItem;
    private boolean mFirstRun = true;
    private ToolbarItem mHomeToolbarItem;
    private ToolbarItem mLogToolbarItem;
    private ToolbarItem mRunToolbarItem;
    private final FbdApp mApp;

    public FbdModule(FbdApp app) {
        super(null, MaterialIcon._Action.DATE_RANGE.getImageView(MODULE_ICON_SIZE).getImage());
        mApp = app;
    }

    @Override
    public Node activate() {
        addAccelerators();
        return mView;
    }

    @Override
    public void init(Workbench workbench) {
        super.init(workbench);
        mView = new FbdView(mApp, workbench, this);
        if (mFirstRun) {
            initToolbar();
            mFirstRun = false;
        }

        setRunningState(RunState.STARTABLE);
    }

    public void setRunningState(RunState runState) {
        Platform.runLater(() -> {
            switch (runState) {
                case STARTABLE:
                    getToolbarControlsLeft().setAll(
                            mLogToolbarItem
                    );
                    getToolbarControlsRight().setAll(
                            mAddToolbarItem
                    );

//                mOptionsAction.setDisabled(false);
                    break;

                case CANCELABLE:
                    getToolbarControlsLeft().setAll(
                            mHomeToolbarItem
                    );
                    getToolbarControlsRight().setAll(
                            mCancelToolbarItem
                    );
                    mHomeToolbarItem.setDisable(true);
//                mOptionsAction.setDisabled(true);
                    break;

                case CLOSEABLE:
                    getToolbarControlsLeft().setAll(
                            mHomeToolbarItem
                    );

                    getToolbarControlsRight().setAll(
                            mRunToolbarItem
                    );

                    mHomeToolbarItem.setDisable(false);
//                mOptionsAction.setDisabled(false);
                    break;

                default:
                    throw new AssertionError();
            }
        });
    }

    private void addAccelerators() {
        ObservableMap<KeyCombination, Runnable> accelerators = getWorkbench().getScene().getAccelerators();
        accelerators.put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            mView.profileEdit(null);
        });
    }

    private void initToolbar() {
        mHomeToolbarItem = new ToolbarItem(
                MaterialIcon._Action.LIST.getImageView(ICON_SIZE_TOOLBAR),
                event -> {
                    mLogToolbarItem.setDisable(false);
                    setRunningState(RunState.STARTABLE);
//                    mView.doNavHome();
                }
        );
        mHomeToolbarItem.setTooltip(new Tooltip(Dict.LIST.toString()));

        mLogToolbarItem = new ToolbarItem(
                MaterialIcon._Editor.FORMAT_ALIGN_LEFT.getImageView(ICON_SIZE_TOOLBAR),
                event -> {
                    setRunningState(RunState.CLOSEABLE);
//                    mView.doNavLog();
                }
        );
        mLogToolbarItem.setTooltip(new Tooltip(Dict.OUTPUT.toString()));
        mLogToolbarItem.setDisable(true);

        mAddToolbarItem = new ToolbarItem(Dict.ADD.toString(),
                MaterialIcon._Content.ADD.getImageView(ICON_SIZE_TOOLBAR),
                event -> {
                    mView.profileEdit(null);
                }
        );

        mRunToolbarItem = new ToolbarItem(
                MaterialIcon._Av.PLAY_ARROW.getImageView(ICON_SIZE_TOOLBAR),
                event -> {
//                    mView.doRun();
                }
        );
        mRunToolbarItem.setTooltip(new Tooltip(Dict.RUN.toString()));

        mCancelToolbarItem = new ToolbarItem(
                MaterialIcon._Navigation.CANCEL.getImageView(ICON_SIZE_TOOLBAR),
                event -> {
//                    mView.doCancel();
                }
        );
        mCancelToolbarItem.setTooltip(new Tooltip(Dict.CANCEL.toString()));

        getToolbarControlsRight().setAll(
                mAddToolbarItem
        );
    }

    private void removeAccelerators() {
        ObservableMap<KeyCombination, Runnable> accelerators = getWorkbench().getScene().getAccelerators();
        accelerators.remove(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
    }

}
