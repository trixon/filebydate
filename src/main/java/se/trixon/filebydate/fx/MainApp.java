/*
 * Copyright 2018 Patrik Karlsson.
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
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
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionGroup;
import org.controlsfx.control.action.ActionUtils;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.PomInfo;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.fx.AlmondFx;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.almond.util.fx.LocaleComboBox;
import se.trixon.almond.util.fx.dialogs.about.AboutPane;
import se.trixon.almond.util.icons.material.MaterialIcon;
import se.trixon.almond.util.swing.dialogs.about.AboutModel;
import se.trixon.filebydate.FileByDate;
import se.trixon.filebydate.Options;
import se.trixon.filebydate.Profile;
import se.trixon.filebydate.ProfileManager;
import se.trixon.filebydate.ui.MainFrame;

/**
 *
 * @author Patrik Karlsson
 */
public class MainApp extends Application {

    public static final String APP_TITLE = "FileByDate";
    private static final int ICON_SIZE_PROFILE = 32;
    private static final int ICON_SIZE_TOOLBAR = 48;
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());
    private final AlmondFx mAlmondFX = AlmondFx.getInstance();
    private final ObservableList<Profile> mItems = FXCollections.observableArrayList();
    private ListView<Profile> mListView;
    private final Options mOptions = Options.getInstance();
    private final ProfileManager mProfileManager = ProfileManager.getInstance();
    private LinkedList<Profile> mProfiles;
    private BorderPane mRoot;
    private Stage mStage;
    private TextArea mSummaryTextArea;
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

        mSummaryTextArea = new TextArea();
        mSummaryTextArea.setPrefRowCount(10);
        mSummaryTextArea.setEditable(false);

        mRoot = new BorderPane();
        mRoot.setTop(mToolBar);
        mRoot.setCenter(mListView);
        mRoot.setBottom(mSummaryTextArea);

        Scene scene = new Scene(mRoot);
        mStage.setScene(scene);

        mOptions.getPreferences().addPreferenceChangeListener((PreferenceChangeEvent evt) -> {
            switch (evt.getKey()) {
                default:
            }
        });
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
        Action swingAction = new Action("Swing", (ActionEvent event) -> {
            java.awt.EventQueue.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            });
        });

        //add
        Action addAction = new Action(Dict.ADD.toString(), (ActionEvent event) -> {
            profileEdit(null);
        });
        addAction.setGraphic(MaterialIcon._Content.ADD.getImageView(ICON_SIZE_TOOLBAR));

        //options
        Action optionsAction = new Action(Dict.OPTIONS.toString(), (ActionEvent event) -> {
            displayOptions();
        });
        optionsAction.setGraphic(MaterialIcon._Action.SETTINGS.getImageView(ICON_SIZE_TOOLBAR));
        optionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));

        //help
        Action helpAction = new Action(Dict.HELP.toString(), (ActionEvent event) -> {
            SystemHelper.browse("https://trixon.se/projects/filebydate/documentation/");
        });
        helpAction.setAccelerator(KeyCombination.keyCombination("F1"));

        //about
        PomInfo pomInfo = new PomInfo(FileByDate.class, "se.trixon", "filebydate");
        AboutModel aboutModel = new AboutModel(SystemHelper.getBundle(FileByDate.class, "about"), SystemHelper.getResourceAsImageView(MainFrame.class, "calendar-icon-1024px.png"));
        aboutModel.setAppVersion(pomInfo.getVersion());
        Action aboutAction = AboutPane.getAction(mStage, aboutModel);

        //about date format
        String title = String.format(Dict.ABOUT_S.toString(), Dict.DATE_PATTERN.toString().toLowerCase());
        Action aboutDateFormatAction = new Action(title, (ActionEvent event) -> {
            SystemHelper.browse("https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html");
        });

        Collection<? extends Action> actions = Arrays.asList(
                addAction,
                optionsAction,
                swingAction,
                ActionUtils.ACTION_SPAN,
                new ActionGroup(Dict.HELP.toString(), MaterialIcon._Action.HELP_OUTLINE.getImageView(ICON_SIZE_TOOLBAR),
                        helpAction,
                        aboutDateFormatAction,
                        ActionUtils.ACTION_SEPARATOR,
                        aboutAction
                )
        );

        mToolBar = ActionUtils.createToolBar(actions, ActionUtils.ActionTextBehavior.HIDE);
    }

    private void loadProfiles() {
        try {
            mProfileManager.load();
            mProfiles = mProfileManager.getProfiles();
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void populateProfiles(Profile profile) {
        mItems.clear();
        Collections.sort(mProfiles);

        mProfiles.stream().forEach((item) -> {
            mItems.add(item);
        });

        if (profile != null) {
            mListView.getSelectionModel().select(profile);
        }
    }

    private void postInit() {
        loadProfiles();
        populateProfiles(null);
    }

    private void profileEdit(Profile profile) {
        System.out.println(System.currentTimeMillis());
    }

    private void profileRemove(Profile profile) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mStage);
        alert.setTitle(Dict.Dialog.TITLE_PROFILE_REMOVE.toString());
        String message = String.format(Dict.Dialog.MESSAGE_PROFILE_REMOVE.toString(), profile.getName());
        alert.setHeaderText(message);
        alert.setContentText(Dict.Dialog.MESSAGE_ARE_YOU_SURE.toString());

        Optional<ButtonType> result = FxHelper.showAndWait(alert, mStage);
        if (result.get() == ButtonType.OK) {
            mProfiles.remove(profile);
            populateProfiles(null);
        }
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
            mLastLabel.setText(new Date().toString());
            mDescLabel.setText("...");

            setGraphic(mBorderPane);
        }

        private void clearContent() {
            setText(null);
            setGraphic(null);
        }

        private void createUI() {
            String fontFamily = mNameLabel.getFont().getFamily();
            double fontSize = mNameLabel.getFont().getSize();

            mNameLabel.setFont(Font.font(fontFamily, FontWeight.BOLD, fontSize * 2));
            mDescLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));
            mLastLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));

            Action runAction = new Action(Dict.RUN.toString(), (ActionEvent event) -> {
                System.out.println(event);
                mListView.requestFocus();
            });
            runAction.setGraphic(MaterialIcon._Av.PLAY_ARROW.getImageView(ICON_SIZE_PROFILE));

            Action editAction = new Action(Dict.EDIT.toString(), (ActionEvent event) -> {
                profileEdit(getSelectedProfile());
                mListView.requestFocus();
            });
            editAction.setGraphic(MaterialIcon._Content.CREATE.getImageView(ICON_SIZE_PROFILE));

            Action cloneAction = new Action(Dict.CLONE.toString(), (ActionEvent event) -> {
                System.out.println(event);
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
            mBorderPane.setRight(toolBar);
            mFadeInTransition.setNode(toolBar);
            mFadeOutTransition.setNode(toolBar);

            mBorderPane.setOnMouseEntered((MouseEvent event) -> {
                if (!toolBar.isVisible()) {
                    toolBar.setVisible(true);
                }

                selectListItem();
                mSummaryTextArea.setText(getSelectedProfile().toDebugString());
                mFadeInTransition.playFromStart();
            });

            mBorderPane.setOnMouseExited((MouseEvent event) -> {
                mFadeOutTransition.playFromStart();
            });
        }

        private Profile getSelectedProfile() {
            return mListView.getSelectionModel().getSelectedItem();
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
