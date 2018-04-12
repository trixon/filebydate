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
package se.trixon.filebydate.ui;

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
import java.util.prefs.PreferenceChangeEvent;
import java.util.stream.Stream;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonBase;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionGroup;
import org.controlsfx.control.action.ActionUtils;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import se.trixon.almond.util.AboutModel;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.PomInfo;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.fx.AlmondFx;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.almond.util.fx.control.LocaleComboBox;
import se.trixon.almond.util.fx.control.LogPanel;
import se.trixon.almond.util.fx.dialogs.about.AboutPane;
import se.trixon.filebydate.FileByDate;
import se.trixon.filebydate.NameCase;
import se.trixon.filebydate.Operation;
import se.trixon.filebydate.OperationListener;
import se.trixon.filebydate.Options;
import se.trixon.filebydate.Profile;
import se.trixon.filebydate.ProfileManager;

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
    private final ResourceBundle mBundle = SystemHelper.getBundle(MainApp.class, "Bundle");
    private Action mCancelAction;
    private Font mDefaultFont;
    private final GlyphFont mFontAwesome = GlyphFontRegistry.font("FontAwesome");
    private Action mHelpAction;
    private Action mHomeAction;
    private final Color mIconColor = Color.BLACK;
    private final ProfileIndicator mIndicator = new ProfileIndicator();
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
        stage.getIcons().add(new Image(MainApp.class.getResourceAsStream("calendar-icon-1024px.png")));

        mAlmondFX.addStageWatcher(stage, MainApp.class);
        createUI();
        postInit();
        initListeners();
        mStage.setTitle(APP_TITLE);
        mStage.show();
        mListView.requestFocus();
        initAccelerators();
        //profileEdit(mProfiles.getFirst());
    }

    private void adjustButtonWidth(Stream<Node> stream, double prefWidth) {
        stream.filter((item) -> (item instanceof ButtonBase))
                .map((item) -> (ButtonBase) item).forEachOrdered((buttonBase) -> {
            buttonBase.setPrefWidth(prefWidth);
        });
    }

    private void createUI() {
        mRoot = new BorderPane();
        Scene scene = new Scene(mRoot);
        //scene.getStylesheets().add("css/modena_dark.css");

        mDefaultFont = Font.getDefault();
        initActions();

        mListView = new ListView<>();
        mListView.setItems(mItems);
        mListView.setCellFactory((ListView<Profile> param) -> new ProfileListCell());
        Label welcomeLabel = new Label(mBundle.getString("welcome"));
        welcomeLabel.setFont(Font.font(mDefaultFont.getName(), FontPosture.ITALIC, 18));

        mListView.setPlaceholder(welcomeLabel);

        mPreviewPanel = new PreviewPanel();

        mRoot.setCenter(mListView);
        mRoot.setBottom(mPreviewPanel);

        mStage.setScene(scene);
        mLogPanel.setWrapText(mOptions.isWordWrap());

        mOptions.getPreferences().addPreferenceChangeListener((PreferenceChangeEvent evt) -> {
            switch (evt.getKey()) {
                case Options.KEY_WORD_WRAP:
                    mLogPanel.setWrapText(mOptions.isWordWrap());
                    break;
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
            mRoot.setCenter(mListView);
        });
        mHomeAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.LIST).size(ICON_SIZE_TOOLBAR).color(mIconColor));

        //log
        mLogAction = new Action(Dict.OUTPUT.toString(), (ActionEvent event) -> {
            setRunningState(RunState.CLOSEABLE);
            mRoot.setCenter(mLogPanel);
        });
        mLogAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.ALIGN_LEFT).size(ICON_SIZE_TOOLBAR).color(mIconColor));
        mLogAction.setDisabled(true);

        //options
        mOptionsAction = new Action(Dict.OPTIONS.toString(), (ActionEvent event) -> {
            displayOptions();
        });
        mOptionsAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.COG).size(ICON_SIZE_TOOLBAR).color(mIconColor));
        mOptionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));

        //help
        mHelpAction = new Action(Dict.HELP.toString(), (ActionEvent event) -> {
            SystemHelper.browse("https://trixon.se/projects/filebydate/documentation/");
        });
        mHelpAction.setAccelerator(KeyCombination.keyCombination("F1"));

        //about
        PomInfo pomInfo = new PomInfo(FileByDate.class, "se.trixon", "filebydate");
        AboutModel aboutModel = new AboutModel(SystemHelper.getBundle(FileByDate.class, "about"), SystemHelper.getResourceAsImageView(MainApp.class, "calendar-icon-1024px.png"));
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
        mRunAction.setGraphic(mFontAwesome.create(FontAwesome.Glyph.PLAY).size(ICON_SIZE_TOOLBAR).color(mIconColor));
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

        mPreviewPanel.setVisible(!mListView.getSelectionModel().isEmpty());
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
            profile.setLastRun(0);
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
            mLogAction.setDisabled(mItems.isEmpty() || mLastRunProfile == null);
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
                actions.addAll(Arrays.asList(
                        mLogAction,
                        ActionUtils.ACTION_SPAN,
                        mAddAction
                ));
                mOptionsAction.setDisabled(false);
                break;

            case CANCELABLE:
                actions.addAll(Arrays.asList(
                        mHomeAction,
                        ActionUtils.ACTION_SPAN,
                        mCancelAction
                ));
                mHomeAction.setDisabled(true);
                mOptionsAction.setDisabled(true);
                break;

            case CLOSEABLE:
                actions.addAll(Arrays.asList(
                        mHomeAction,
                        ActionUtils.ACTION_SPAN,
                        mRunAction
                ));
                mHomeAction.setDisabled(false);
                mOptionsAction.setDisabled(false);
                break;

            default:
                throw new AssertionError();
        }

        actions.addAll(Arrays.asList(
                mOptionsAction,
                new ActionGroup(Dict.HELP.toString(), mFontAwesome.create(FontAwesome.Glyph.QUESTION).size(ICON_SIZE_TOOLBAR).color(mIconColor),
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
                mToolBar.getItems().add(1, mIndicator);
                mIndicator.setVisible(runState != RunState.STARTABLE);
            }

            adjustButtonWidth(mToolBar.getItems().stream(), ICON_SIZE_TOOLBAR * 1.5);
            mToolBar.getItems().stream().filter((item) -> (item instanceof ButtonBase))
                    .map((item) -> (ButtonBase) item).forEachOrdered((buttonBase) -> {
                FxHelper.undecorateButton(buttonBase);
            });
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
            mDescLabel.setText("🖉 " + profile.getDescription());
            String lastRun = "-";
            if (profile.getLastRun() != 0) {
                lastRun = mSimpleDateFormat.format(new Date(profile.getLastRun()));
            }
            mLastLabel.setText("🕑 " + lastRun);

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
            adjustButtonWidth(toolBar.getItems().stream(), ICON_SIZE_PROFILE * 1.8);

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
