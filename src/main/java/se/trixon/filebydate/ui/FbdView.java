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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionUtils;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.filebydate.NameCase;
import se.trixon.filebydate.Operation;
import se.trixon.filebydate.OperationListener;
import se.trixon.filebydate.Profile;
import se.trixon.filebydate.ProfileManager;
import static se.trixon.filebydate.ui.FbdApp.ICON_SIZE_PROFILE;
import static se.trixon.filebydate.ui.FbdApp.ICON_SIZE_TOOLBAR;

/**
 *
 * @author Patrik Karlström
 */
public class FbdView extends BorderPane {

    private static final Logger LOGGER = Logger.getLogger(FbdView.class.getName());

    private Action mAddAction;
    private Action mCancelAction;
    private Font mDefaultFont;
    private final GlyphFont mFontAwesome = GlyphFontRegistry.font("FontAwesome");
    private Action mHomeAction;
    private final Color mIconColor = Color.BLACK;
    private final ProfileIndicator mIndicator = new ProfileIndicator();
    private final ObservableList<Profile> mItems = FXCollections.observableArrayList();
    private Profile mLastRunProfile;
    private ListView<Profile> mListView;
    private Action mLogAction;
    private OperationListener mOperationListener;
    private Thread mOperationThread;
    private PreviewPanel mPreviewPanel;
    private final ProfileManager mProfileManager = ProfileManager.getInstance();
    private LinkedList<Profile> mProfiles;
    private final ProgressPanel mProgressPanel = new ProgressPanel();
    private Action mRunAction;
    private final Workbench mWorkbench;
    private final FbdModule mModule;
    private final ResourceBundle mBundle = SystemHelper.getBundle(FbdApp.class, "Bundle");
    private final Stage mStage = null;
    private final FbdApp mApp;

    public FbdView(FbdApp app, Workbench workbench, FbdModule module) {
        mApp = app;
        mWorkbench = workbench;
        mModule = module;
        createUI();
        initListeners();
        postInit();
        mListView.requestFocus();
    }

    private void createUI() {
        mDefaultFont = Font.getDefault();
        initActions();

        mListView = new ListView<>();
        mListView.setItems(mItems);
        mListView.setCellFactory((ListView<Profile> param) -> new ProfileListCell());
        Label welcomeLabel = new Label(mBundle.getString("welcome"));
        welcomeLabel.setFont(Font.font(mDefaultFont.getName(), FontPosture.ITALIC, 18));

        mListView.setPlaceholder(welcomeLabel);

        mPreviewPanel = new PreviewPanel();

        setCenter(mListView);
        setBottom(mPreviewPanel);

        setRunningState(RunState.STARTABLE);
    }

    private void initAccelerators() {
        final ObservableMap<KeyCombination, Runnable> accelerators = getScene().getAccelerators();

        accelerators.put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            profileEdit(null);
        });

    }

    private void initActions() {
        //add
        mAddAction = new Action(Dict.ADD.toString(), (ActionEvent event) -> {
            profileEdit(null);
        });
        mAddAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.PLUS).size(ICON_SIZE_TOOLBAR).color(mIconColor));

        //cancel
        mCancelAction = new Action(Dict.CANCEL.toString(), (ActionEvent event) -> {
            mOperationThread.interrupt();
        });
        mCancelAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.BAN).size(ICON_SIZE_TOOLBAR).color(mIconColor));

        //home
        mHomeAction = new Action(Dict.LIST.toString(), (ActionEvent event) -> {
            mLogAction.setDisabled(false);
            setRunningState(RunState.STARTABLE);
//            mRoot.setCenter(mListView);
        });
        mHomeAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.LIST).size(ICON_SIZE_TOOLBAR).color(mIconColor));

        //log
        mLogAction = new Action(Dict.OUTPUT.toString(), (ActionEvent event) -> {
            setRunningState(RunState.CLOSEABLE);
            setCenter(mProgressPanel);
        });
        mLogAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.ALIGN_LEFT).size(ICON_SIZE_TOOLBAR).color(mIconColor));
        mLogAction.setDisabled(true);

//        //options
//        mOptionsAction = new Action(Dict.OPTIONS.toString(), (ActionEvent event) -> {
//            displayOptions();
//        });
//        mOptionsAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.COG).size(ICON_SIZE_TOOLBAR).color(mIconColor));
//        if (!IS_MAC) {
//            mOptionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
//        }
//
//        //help
//        mHelpAction = new Action(Dict.HELP.toString(), (ActionEvent event) -> {
//            SystemHelper.desktopBrowse("https://trixon.se/projects/filebydate/documentation/");
//        });
//        mHelpAction.setAccelerator(KeyCombination.keyCombination("F1"));
        //about
//        PomInfo pomInfo = new PomInfo(FileByDate.class, "se.trixon", "filebydate");
//        AboutModel aboutModel = new AboutModel(SystemHelper.getBundle(FileByDate.class, "about"), SystemHelperFx.getResourceAsImageView(MainApp.class, "calendar-icon-1024px.png"));
//        aboutModel.setAppVersion(pomInfo.getVersion());
//        mAboutAction = AboutPane.getAction(mStage, aboutModel);
        //about date format
        mRunAction = new Action(Dict.RUN.toString(), (ActionEvent event) -> {
            profileRun(mLastRunProfile);
        });
        mRunAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.PLAY).size(ICON_SIZE_TOOLBAR).color(mIconColor));
    }

    private void initListeners() {
        mOperationListener = new OperationListener() {
            private boolean mSuccess;

            @Override
            public void onOperationError(String message) {
                mProgressPanel.err(message);
            }

            @Override
            public void onOperationFailed(String message) {
                onOperationFinished(message, 0);
                mSuccess = false;
            }

            @Override
            public void onOperationFinished(String message, int fileCount) {
                setRunningState(RunState.CLOSEABLE);
                mProgressPanel.out(Dict.DONE.toString());
                populateProfiles(mLastRunProfile);

                if (0 == fileCount) {
                    mProgressPanel.setProgress(1);
                }
            }

            @Override
            public void onOperationInterrupted() {
                setRunningState(RunState.CLOSEABLE);
                mProgressPanel.setProgress(0);
                mSuccess = false;
            }

            @Override
            public void onOperationLog(String message) {
                mProgressPanel.out(message);
            }

            @Override
            public void onOperationProcessingStarted() {
                mProgressPanel.setProgress(-1);
            }

            @Override
            public void onOperationProgress(int value, int max) {
                mProgressPanel.setProgress(value / (double) max);
            }

            @Override
            public void onOperationStarted() {
                setRunningState(RunState.CANCELABLE);
                mProgressPanel.setProgress(0);
                mSuccess = true;
            }
        };

    }

    private void populateProfiles(Profile profile) {
        mItems.clear();
        Collections.sort(mProfiles);

        mProfiles.stream().forEach((item) -> {
            mItems.add(item);
        });

        if (profile != null) {
            mListView.getSelectionModel().select(profile);
            mListView.scrollTo(profile);
        }

        mPreviewPanel.setVisible(!mListView.getSelectionModel().isEmpty());
    }

    private void postInit() {
        profilesLoad();
        populateProfiles(null);
    }

    void profileEdit(Profile profile) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.initOwner(mStage);
        alert.initOwner(null);
        String title = Dict.EDIT.toString();
        boolean addNew = false;
        boolean clone = profile != null && profile.getName() == null;

        if (profile == null) {
            title = Dict.ADD.toString();
            addNew = true;
            profile = new Profile();
            profile.setSourceDir(FileUtils.getUserDirectory());
            profile.setDestDir(FileUtils.getUserDirectory());
            profile.setFilePattern("{*.jpg,*.JPG}");
            profile.setDatePattern("yyyy/MM/yyyy-MM-dd");
            profile.setOperation(0);
            profile.setFollowLinks(true);
            profile.setRecursive(true);
            profile.setReplaceExisting(false);
            profile.setCaseBase(NameCase.UNCHANGED);
            profile.setCaseExt(NameCase.UNCHANGED);
        } else if (clone) {
            title = Dict.CLONE.toString();
            profile.setLastRun(0);
        }

        alert.setTitle(title);
        alert.setGraphic(null);
        alert.setHeaderText(null);

        ProfilePanel profilePanel = new ProfilePanel(profile);

        final DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(profilePanel);
        profilePanel.setOkButton((Button) dialogPane.lookupButton(ButtonType.OK));

        Optional<ButtonType> result = FxHelper.showAndWait(alert, mStage);
        if (result.get() == ButtonType.OK) {
            profilePanel.save();
            if (addNew || clone) {
                mProfiles.add(profile);
            }

            profilesSave();
            populateProfiles(profile);
        }
    }

    private void profileRemove(Profile profile) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.initOwner(mStage);
        alert.initOwner(null);
        alert.setTitle(Dict.Dialog.TITLE_PROFILE_REMOVE.toString() + "?");
        String message = String.format(Dict.Dialog.MESSAGE_PROFILE_REMOVE.toString(), profile.getName());
        alert.setHeaderText(message);

        ButtonType removeButtonType = new ButtonType(Dict.REMOVE.toString(), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(Dict.CANCEL.toString(), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(removeButtonType, cancelButtonType);

        Optional<ButtonType> result = FxHelper.showAndWait(alert, mStage);
        if (result.get() == removeButtonType) {
            mProfiles.remove(profile);
            profilesSave();
            populateProfiles(null);
            mLogAction.setDisabled(mItems.isEmpty() || mLastRunProfile == null);
        }
    }

    private void profileRun(Profile profile) {
        String title = String.format(Dict.Dialog.TITLE_PROFILE_RUN.toString(), profile.getName());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.initOwner(mStage);
        alert.initOwner(null);

        alert.setTitle(title);
        alert.setGraphic(null);
        alert.setHeaderText(null);

        PreviewPanel previewPanel = new PreviewPanel();
        previewPanel.load(profile);
        final DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(previewPanel);

        ButtonType runButtonType = new ButtonType(Dict.RUN.toString());
        ButtonType dryRunButtonType = new ButtonType(Dict.DRY_RUN.toString(), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(Dict.CANCEL.toString(), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(runButtonType, dryRunButtonType, cancelButtonType);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() != cancelButtonType) {
            boolean dryRun = result.get() == dryRunButtonType;
            profile.setDryRun(dryRun);
            mProgressPanel.clear();
            setCenter(mProgressPanel);
            mIndicator.setProfile(profile);

            if (profile.isValid()) {
                mLastRunProfile = profile;
                mOperationThread = new Thread(() -> {
                    Operation operation = new Operation(mOperationListener, profile);
                    operation.start();
                });
                mOperationThread.setName("Operation");
                mOperationThread.start();
            } else {
                mProgressPanel.out(profile.toDebugString());
                mProgressPanel.out(profile.getValidationError());
                mProgressPanel.out(Dict.ABORTING.toString());
            }
        }
    }

    private void profilesLoad() {
        try {
            mProfileManager.load();
            mProfiles = mProfileManager.getProfiles();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void profilesSave() {
        try {
            mProfileManager.save();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void setRunningState(RunState runState) {
        ArrayList<Action> actions = new ArrayList<>();

        switch (runState) {
            case STARTABLE:
                actions.addAll(Arrays.asList(
                        mLogAction
                ));
                mApp.getOptionsAction().setDisabled(false);
                break;

            case CANCELABLE:
                actions.addAll(Arrays.asList(
                        mHomeAction,
                        ActionUtils.ACTION_SPAN,
                        mCancelAction
                ));
                mHomeAction.setDisabled(true);
                mApp.getOptionsAction().setDisabled(true);
                break;

            case CLOSEABLE:
                actions.addAll(Arrays.asList(
                        mHomeAction,
                        ActionUtils.ACTION_SPAN,
                        mRunAction
                ));
                mHomeAction.setDisabled(false);
                mApp.getOptionsAction().setDisabled(false);
                break;

            default:
                throw new AssertionError();
        }

        actions.addAll(Arrays.asList());

    }

    public enum RunState {
        STARTABLE, CANCELABLE, CLOSEABLE;
    }

    class ProfileListCell extends ListCell<Profile> {

        private final BorderPane mBorderPane = new BorderPane();
        private final Label mDescLabel = new Label();
        private final Duration mDuration = Duration.millis(200);
        private final FadeTransition mFadeInTransition = new FadeTransition();
        private final FadeTransition mFadeOutTransition = new FadeTransition();
        private final Label mLastLabel = new Label();
        private final Label mNameLabel = new Label();
        private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat();

        public ProfileListCell() {
            mFadeInTransition.setDuration(mDuration);
            mFadeInTransition.setFromValue(0);
            mFadeInTransition.setToValue(1);

            mFadeOutTransition.setDuration(mDuration);
            mFadeOutTransition.setFromValue(1);
            mFadeOutTransition.setToValue(0);

            createUI();
        }

        private void addContent(Profile profile) {
            setText(null);

            mNameLabel.setText(profile.getName());
            mDescLabel.setText(profile.getDescription());
            String lastRun = "-";
            if (profile.getLastRun() != 0) {
                lastRun = mSimpleDateFormat.format(new Date(profile.getLastRun()));
            }
            mLastLabel.setText(lastRun);

            setGraphic(mBorderPane);
        }

        private void clearContent() {
            setText(null);
            setGraphic(null);
        }

        private void createUI() {
            String fontFamily = mDefaultFont.getFamily();
            double fontSize = mDefaultFont.getSize();

            mNameLabel.setFont(Font.font(fontFamily, FontWeight.BOLD, fontSize * 2));
            mDescLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));
            mLastLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));

            Action runAction = new Action(Dict.RUN.toString(), (ActionEvent event) -> {
                profileRun(getSelectedProfile());
                mListView.requestFocus();
            });
            runAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.PLAY).size(ICON_SIZE_PROFILE).color(mIconColor));

            Action editAction = new Action(Dict.EDIT.toString(), (ActionEvent event) -> {
                profileEdit(getSelectedProfile());
                mListView.requestFocus();
            });
            editAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.EDIT).size(ICON_SIZE_PROFILE).color(mIconColor));

            Action cloneAction = new Action(Dict.CLONE.toString(), (ActionEvent event) -> {
                profileClone();
                mListView.requestFocus();
            });
            cloneAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.COPY).size(ICON_SIZE_PROFILE).color(mIconColor));

            Action removeAction = new Action(Dict.REMOVE.toString(), (ActionEvent event) -> {
                profileRemove(getSelectedProfile());
                mListView.requestFocus();
            });
            removeAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.TRASH).size(ICON_SIZE_PROFILE).color(mIconColor));

            VBox mainBox = new VBox(mNameLabel, mDescLabel, mLastLabel);
            mainBox.setAlignment(Pos.CENTER_LEFT);

            Collection<? extends Action> actions = Arrays.asList(
                    runAction,
                    editAction,
                    cloneAction,
                    removeAction
            );

            ToolBar toolBar = ActionUtils.createToolBar(actions, ActionUtils.ActionTextBehavior.HIDE);
            toolBar.setBackground(Background.EMPTY);
            toolBar.setVisible(false);
            FxHelper.adjustButtonWidth(toolBar.getItems().stream(), ICON_SIZE_PROFILE * 1.8);

            toolBar.getItems().stream().filter((item) -> (item instanceof ButtonBase))
                    .map((item) -> (ButtonBase) item).forEachOrdered((buttonBase) -> {
                FxHelper.undecorateButton(buttonBase);
            });

            BorderPane.setAlignment(toolBar, Pos.CENTER);

            mBorderPane.setCenter(mainBox);
            BorderPane.setMargin(mainBox, new Insets(8));
            mBorderPane.setRight(toolBar);
            mFadeInTransition.setNode(toolBar);
            mFadeOutTransition.setNode(toolBar);

            mBorderPane.setOnMouseEntered((MouseEvent event) -> {
                if (!toolBar.isVisible()) {
                    toolBar.setVisible(true);
                }

                selectListItem();
                mPreviewPanel.load(getSelectedProfile());
                mFadeInTransition.playFromStart();
            });

            mBorderPane.setOnMouseExited((MouseEvent event) -> {
                mFadeOutTransition.playFromStart();
            });
        }

        private Profile getSelectedProfile() {
            return mListView.getSelectionModel().getSelectedItem();
        }

        private void profileClone() {
            Profile p = getSelectedProfile().clone();
            p.setName(null);
            profileEdit(p);
        }

        private void selectListItem() {
            mListView.getSelectionModel().select(this.getIndex());
            mListView.requestFocus();
        }

        @Override
        protected void updateItem(Profile profile, boolean empty) {
            super.updateItem(profile, empty);

            if (profile == null || empty) {
                clearContent();
            } else {
                addContent(profile);
            }
        }
    }
}
