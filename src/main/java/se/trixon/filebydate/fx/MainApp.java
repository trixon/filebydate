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
package se.trixon.filebydate.fx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionGroup;
import org.controlsfx.control.action.ActionUtils;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.PomInfo;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.fx.AlmondFx;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.almond.util.fx.control.LocaleComboBox;
import se.trixon.almond.util.fx.control.LogPanel;
import se.trixon.almond.util.fx.dialogs.about.AboutPane;
import se.trixon.almond.util.icons.material.MaterialIcon;
import se.trixon.almond.util.swing.dialogs.about.AboutModel;
import se.trixon.filebydate.FileByDate;
import se.trixon.filebydate.NameCase;
import se.trixon.filebydate.Operation;
import se.trixon.filebydate.OperationListener;
import se.trixon.filebydate.Options;
import se.trixon.filebydate.Profile;
import se.trixon.filebydate.ProfileManager;
import se.trixon.filebydate.ui.MainFrame;

/**
 *
 * @author Patrik Karlström
 */
public class MainApp extends Application {

    public static final String APP_TITLE = "FileByDate";
    private static final int ICON_SIZE_PROFILE = 32;
    private static final int ICON_SIZE_TOOLBAR = 48;
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());
    private Action mAboutAction;
    private Action mAboutDateFormatAction;
    private Action mAddAction;
    private final AlmondFx mAlmondFX = AlmondFx.getInstance();
    private Action mCancelAction;
    private Action mHelpAction;
    private Action mHomeAction;
    private final ObservableList<Profile> mItems = FXCollections.observableArrayList();
    private Profile mLastRunProfile;
    private ListView<Profile> mListView;
    private Action mLogAction;
    private final LogPanel mLogPanel = new LogPanel();
    private OperationListener mOperationListener;
    private Thread mOperationThread;
    private final Options mOptions = Options.getInstance();
    private Action mOptionsAction;
    private PreviewPanel mPreviewPanel;
    private final ProfileManager mProfileManager = ProfileManager.getInstance();
    private LinkedList<Profile> mProfiles;
    private BorderPane mRoot;
    private Action mRunAction;
    private Stage mStage;
    private Action mSwingAction;
    private ToolBar mToolBar;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        mStage = stage;
        stage.getIcons().add(new Image(MainFrame.class.getResourceAsStream("calendar-icon-1024px.png")));

        mAlmondFX.addStageWatcher(stage, MainApp.class);
        createUI();
        postInit();
        initListeners();
        mStage.setTitle(APP_TITLE);
        mStage.show();
        mListView.requestFocus();
        initAccelerators();
    }

    private void createUI() {
        initActions();

        mListView = new ListView<>();
        mListView.setItems(mItems);
        mListView.setCellFactory((ListView<Profile> param) -> new ProfileListCell());
        mPreviewPanel = new PreviewPanel();

        mRoot = new BorderPane();
        mRoot.setCenter(mListView);
        mRoot.setBottom(mPreviewPanel);

        Scene scene = new Scene(mRoot);
        //scene.getStylesheets().add("css/modena_dark.css");
        mStage.setScene(scene);

        mOptions.getPreferences().addPreferenceChangeListener((PreferenceChangeEvent evt) -> {
            switch (evt.getKey()) {
                default:
            }
        });

        setRunningState(RunState.STARTABLE);
    }

    private void displayOptions() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mStage);
        alert.setTitle(Dict.OPTIONS.toString());
        alert.setGraphic(null);
        alert.setHeaderText(null);

        Label label = new Label(Dict.CALENDAR_LANGUAGE.toString());
        LocaleComboBox localeComboBox = new LocaleComboBox();
        CheckBox checkBox = new CheckBox(Dict.DYNAMIC_WORD_WRAP.toString());
        GridPane gridPane = new GridPane();
        //gridPane.setGridLinesVisible(true);
        gridPane.addColumn(0, label, localeComboBox, checkBox);
        GridPane.setMargin(checkBox, new Insets(16, 0, 0, 0));

        final DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(gridPane);

        localeComboBox.setLocale(mOptions.getLocale());
        checkBox.setSelected(mOptions.isWordWrap());

        Optional<ButtonType> result = FxHelper.showAndWait(alert, mStage);
        if (result.get() == ButtonType.OK) {
            mOptions.setLocale(localeComboBox.getLocale());
            mOptions.setWordWrap(checkBox.isSelected());
        }
    }

    private void initAccelerators() {
        final ObservableMap<KeyCombination, Runnable> accelerators = mStage.getScene().getAccelerators();

        accelerators.put(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            mStage.fireEvent(new WindowEvent(mStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        });

        accelerators.put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            profileEdit(null);
        });
        accelerators.put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            displayOptions();
        });
    }

    private void initActions() {
        //Legacy UI
        mSwingAction = new Action("Swing", (ActionEvent event) -> {
            java.awt.EventQueue.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            });
        });

        //add
        mAddAction = new Action(Dict.ADD.toString(), (ActionEvent event) -> {
            profileEdit(null);
        });
        mAddAction.setGraphic(MaterialIcon._Content.ADD.getImageView(ICON_SIZE_TOOLBAR));

        //cancel
        mCancelAction = new Action(Dict.CANCEL.toString(), (ActionEvent event) -> {
            mOperationThread.interrupt();
        });
        mCancelAction.setGraphic(MaterialIcon._Navigation.CANCEL.getImageView(ICON_SIZE_TOOLBAR));

        //home
        mHomeAction = new Action(Dict.HOME.toString(), (ActionEvent event) -> {
            mLogAction.setDisabled(false);
            setRunningState(RunState.STARTABLE);
            mRoot.setCenter(mListView);
        });
        mHomeAction.setGraphic(MaterialIcon._Action.HOME.getImageView(ICON_SIZE_TOOLBAR));

        //log
        mLogAction = new Action(Dict.OUTPUT.toString(), (ActionEvent event) -> {
            setRunningState(RunState.CLOSEABLE);
            mRoot.setCenter(mLogPanel);
        });
        mLogAction.setGraphic(MaterialIcon._Action.SUBJECT.getImageView(ICON_SIZE_TOOLBAR));
        mLogAction.setDisabled(true);

        //options
        mOptionsAction = new Action(Dict.OPTIONS.toString(), (ActionEvent event) -> {
            displayOptions();
        });
        mOptionsAction.setGraphic(MaterialIcon._Action.SETTINGS.getImageView(ICON_SIZE_TOOLBAR));
        mOptionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));

        //help
        mHelpAction = new Action(Dict.HELP.toString(), (ActionEvent event) -> {
            SystemHelper.browse("https://trixon.se/projects/filebydate/documentation/");
        });
        mHelpAction.setAccelerator(KeyCombination.keyCombination("F1"));

        //about
        PomInfo pomInfo = new PomInfo(FileByDate.class, "se.trixon", "filebydate");
        AboutModel aboutModel = new AboutModel(SystemHelper.getBundle(FileByDate.class, "about"), SystemHelper.getResourceAsImageView(MainFrame.class, "calendar-icon-1024px.png"));
        aboutModel.setAppVersion(pomInfo.getVersion());
        mAboutAction = AboutPane.getAction(mStage, aboutModel);

        //about date format
        String title = String.format(Dict.ABOUT_S.toString(), Dict.DATE_PATTERN.toString().toLowerCase());
        mAboutDateFormatAction = new Action(title, (ActionEvent event) -> {
            SystemHelper.browse("https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html");
        });

        mRunAction = new Action(Dict.RUN.toString(), (ActionEvent event) -> {
            profileRun(mLastRunProfile);
        });
        mRunAction.setGraphic(MaterialIcon._Av.PLAY_ARROW.getImageView(ICON_SIZE_TOOLBAR));
    }

    private void initListeners() {
        mOperationListener = new OperationListener() {
            @Override
            public void onOperationFailed(String message) {
            }

            @Override
            public void onOperationFinished(String message) {
                mLogPanel.println(Dict.DONE.toString());
                populateProfiles(null);
                setRunningState(RunState.CLOSEABLE);
            }

            @Override
            public void onOperationInterrupted() {
                setRunningState(RunState.CLOSEABLE);
            }

            @Override
            public void onOperationLog(String message) {
                mLogPanel.println(message);
            }

            @Override
            public void onOperationProcessingStarted() {
            }

            @Override
            public void onOperationStarted() {
                setRunningState(RunState.CANCELABLE);
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
    }

    private void postInit() {
        profilesLoad();
        populateProfiles(null);
    }

    private void profileEdit(Profile profile) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.initOwner(mStage);
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
        }

        alert.setTitle(title);
        alert.setGraphic(null);
        alert.setHeaderText(null);

        ProfilePane profilePane = new ProfilePane(profile);

        final DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(profilePane);
        profilePane.setOkButton((Button) dialogPane.lookupButton(ButtonType.OK));

        Optional<ButtonType> result = FxHelper.showAndWait(alert, mStage);
        if (result.get() == ButtonType.OK) {
            profilePane.save();
            if (addNew || clone) {
                mProfiles.add(profile);
            }

            profilesSave();
            populateProfiles(profile);
        }
    }

    private void profileRemove(Profile profile) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.initOwner(mStage);
        alert.setTitle(Dict.Dialog.TITLE_PROFILE_REMOVE.toString());
        String message = String.format(Dict.Dialog.MESSAGE_PROFILE_REMOVE.toString(), profile.getName());
        alert.setHeaderText(message);
        alert.setContentText(Dict.Dialog.MESSAGE_ARE_YOU_SURE.toString());

        Optional<ButtonType> result = FxHelper.showAndWait(alert, mStage);
        if (result.get() == ButtonType.OK) {
            mProfiles.remove(profile);
            profilesSave();
            populateProfiles(null);
        }
    }

    private void profileRun(Profile profile) {
        String title = String.format(Dict.Dialog.TITLE_PROFILE_RUN.toString(), profile.getName());
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.initOwner(mStage);

        alert.setTitle(title);
        alert.setGraphic(null);
        alert.setHeaderText(null);

        PreviewPanel previewPanel = new PreviewPanel();
        previewPanel.load(profile);
        final DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(previewPanel);

        ButtonType runButtonType = new ButtonType(Dict.RUN.toString());
        ButtonType dryRunButtonType = new ButtonType(Dict.DRY_RUN.toString(), ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(Dict.CANCEL.toString(), ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(runButtonType, dryRunButtonType, cancelButtonType);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() != cancelButtonType) {
            boolean dryRun = result.get() == dryRunButtonType;
            profile.setDryRun(dryRun);
            mLogPanel.clear();
            mRoot.setCenter(mLogPanel);

            if (profile.isValid()) {
                mLastRunProfile = profile;
                mOperationThread = new Thread(() -> {
                    Operation operation = new Operation(mOperationListener, profile);
                    operation.start();
                });
                mOperationThread.setName("Operation");
                mOperationThread.start();
            } else {
                mLogPanel.println(profile.toDebugString());
                mLogPanel.println(profile.getValidationError());
                mLogPanel.println(Dict.ABORTING.toString());
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
                actions.add(mLogAction);
                mHomeAction.setDisabled(true);
                mAddAction.setDisabled(false);
                mOptionsAction.setDisabled(false);
                break;

            case CANCELABLE:
                actions.addAll(Arrays.asList(mHomeAction, mCancelAction));
                mHomeAction.setDisabled(true);
                mAddAction.setDisabled(true);
                mOptionsAction.setDisabled(true);
                break;

            case CLOSEABLE:
                actions.addAll(Arrays.asList(mHomeAction, mRunAction));
                mHomeAction.setDisabled(false);
                mAddAction.setDisabled(true);
                mOptionsAction.setDisabled(false);
                break;

            default:
                throw new AssertionError();
        }

        actions.addAll(Arrays.asList(
                ActionUtils.ACTION_SPAN,
                mSwingAction,
                mAddAction,
                mOptionsAction,
                new ActionGroup(Dict.HELP.toString(), MaterialIcon._Action.HELP_OUTLINE.getImageView(ICON_SIZE_TOOLBAR),
                        mHelpAction,
                        mAboutDateFormatAction,
                        ActionUtils.ACTION_SEPARATOR,
                        mAboutAction
                )
        ));

        Platform.runLater(() -> {
            if (mToolBar == null) {
                mToolBar = ActionUtils.createToolBar(actions, ActionUtils.ActionTextBehavior.HIDE);
                mRoot.setTop(mToolBar);
            } else {
                mToolBar = ActionUtils.updateToolBar(mToolBar, actions, ActionUtils.ActionTextBehavior.HIDE);
            }
        });
    }

    public enum RunState {
        STARTABLE, CANCELABLE, CLOSEABLE;
    }

    class ProfileListCell extends ListCell<Profile> {

        private final BorderPane mBorderPane = new BorderPane();
        private final Label mDescLabel = new Label();
        private final Duration mDuration = Duration.millis(500);
        private final FadeTransition mFadeInTransition = new FadeTransition();
        private final FadeTransition mFadeOutTransition = new FadeTransition();
        private final Label mLastLabel = new Label();
        private final Label mNameLabel = new Label();

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
            mLastLabel.setText("🕑 " + new Date(profile.getLastRun()).toString());
            mDescLabel.setText("🖉 " + profile.getDescription());

            setGraphic(mBorderPane);
        }

        private void clearContent() {
            setText(null);
            setGraphic(null);
        }

        private void createUI() {
            String fontFamily = Font.getDefault().getFamily();
            double fontSize = Font.getDefault().getSize();

            mNameLabel.setFont(Font.font(fontFamily, FontWeight.BOLD, fontSize * 2));
            mDescLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));
            mLastLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));

            Action runAction = new Action(Dict.RUN.toString(), (ActionEvent event) -> {
                profileRun(getSelectedProfile());
                mListView.requestFocus();
            });
            runAction.setGraphic(MaterialIcon._Av.PLAY_ARROW.getImageView(ICON_SIZE_PROFILE));

            Action editAction = new Action(Dict.EDIT.toString(), (ActionEvent event) -> {
                profileEdit(getSelectedProfile());
                mListView.requestFocus();
            });
            editAction.setGraphic(MaterialIcon._Content.CREATE.getImageView(ICON_SIZE_PROFILE));

            Action cloneAction = new Action(Dict.CLONE.toString(), (ActionEvent event) -> {
                profileClone();
                mListView.requestFocus();
            });
            cloneAction.setGraphic(MaterialIcon._Content.CONTENT_COPY.getImageView(ICON_SIZE_PROFILE));

            Action removeAction = new Action(Dict.REMOVE.toString(), (ActionEvent event) -> {
                profileRemove(getSelectedProfile());
                mListView.requestFocus();
            });
            removeAction.setGraphic(MaterialIcon._Content.REMOVE_CIRCLE_OUTLINE.getImageView(ICON_SIZE_PROFILE));

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
