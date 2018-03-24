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
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private MenuItem mAboutDateFormatMenuItem;
    private MenuItem mAboutMenuItem;
    private Button mAddButton;
    private final AlmondFx mAlmondFX = AlmondFx.getInstance();
    private MenuItem mHelpMenuItem;
    private final ObservableList<Profile> mItems = FXCollections.observableArrayList();
    private ListView<Profile> mListView;
    private final Options mOptions = Options.getInstance();
    private final ProfileManager mProfileManager = ProfileManager.getInstance();
    private LinkedList<Profile> mProfiles;
    private Button mRemoveAllButton;
    private BorderPane mRoot;
    private Button mSettingsButton;
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
        postInitActions();
        mStage.setTitle(APP_TITLE);
        mStage.show();
        mListView.requestFocus();
//        mListView.getSelectionModel().select(0);
//        mAboutMenuItem.fire();
    }

    private void createUI() {
        mListView = new ListView<>();
        mListView.setItems(mItems);
        mListView.setCellFactory((ListView<Profile> param) -> new ProfileListCell());

        mAddButton = new Button(null, MaterialIcon._Content.ADD.getImageView(ICON_SIZE_TOOLBAR));
        mRemoveAllButton = new Button(null, MaterialIcon._Content.CLEAR.getImageView(ICON_SIZE_TOOLBAR));
        mSettingsButton = new Button(null, MaterialIcon._Action.SETTINGS.getImageView(ICON_SIZE_TOOLBAR));

        mAddButton.setTooltip(new Tooltip(Dict.ADD.toString()));
        mRemoveAllButton.setTooltip(new Tooltip(Dict.REMOVE_ALL.toString()));
        mSettingsButton.setTooltip(new Tooltip(Dict.OPTIONS.toString()));

        mHelpMenuItem = new MenuItem(Dict.HELP.toString());

        //about
        PomInfo pomInfo = new PomInfo(FileByDate.class, "se.trixon", "filebydate");
        AboutModel aboutModel = new AboutModel(SystemHelper.getBundle(FileByDate.class, "about"), SystemHelper.getResourceAsImageView(MainFrame.class, "calendar-icon-1024px.png"));
        aboutModel.setAppVersion(pomInfo.getVersion());
        mAboutMenuItem = ActionUtils.createMenuItem(AboutPane.getAction(mStage, new AboutPane(aboutModel)));

        //about date format
        String title = String.format(Dict.ABOUT_S.toString(), Dict.DATE_PATTERN.toString().toLowerCase());
        mAboutDateFormatMenuItem = new MenuItem(title);

        MenuButton mHelpMenuButton = new MenuButton(null, MaterialIcon._Action.HELP_OUTLINE.getImageView(ICON_SIZE_TOOLBAR));
        mHelpMenuButton.getItems().addAll(mHelpMenuItem, mAboutDateFormatMenuItem, mAboutMenuItem);

        mToolBar = new ToolBar(
                mAddButton,
                mRemoveAllButton,
                mSettingsButton,
                new Separator(),
                mHelpMenuButton
        );

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

    private void loadProfiles() {
//        SwingHelper.enableComponents(configPanel, false);

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

//        if (profile != null) {
//            mModel.setSelectedItem(profile);
//        }
        boolean hasProfiles = !mProfiles.isEmpty();
//        SwingHelper.enableComponents(configPanel, hasProfiles);
    }

    private void postInit() {
        loadProfiles();
        populateProfiles(null);
    }

    private void postInitActions() {
        mAddButton.setOnAction((ActionEvent event) -> {
            System.out.println(event);
        });

        mRemoveAllButton.setOnAction((ActionEvent event) -> {
            System.out.println(event);
        });

        mSettingsButton.setOnAction((ActionEvent event) -> {
            displayOptions();
        });

        mHelpMenuItem.setOnAction((ActionEvent event) -> {
            SystemHelper.browse("https://trixon.se/projects/filebydate/documentation/");
        });

        mAboutDateFormatMenuItem.setOnAction((ActionEvent event) -> {
            SystemHelper.browse("https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html");
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
        gridPane.setMargin(checkBox, new Insets(16, 0, 0, 0));

        final DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(gridPane);
        dialogPane.setPrefSize(300, 200);

        localeComboBox.setLocale(mOptions.getLocale());
        checkBox.setSelected(mOptions.isWordWrap());

        Optional<ButtonType> result = FxHelper.showAndWait(alert, mStage);
        if (result.get() == ButtonType.OK) {
            mOptions.setLocale(localeComboBox.getLocale());
            mOptions.setWordWrap(checkBox.isSelected());
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
        private Button mRunButton;
        private Button mEditButton;
        private Button mRemoveButton;
        private Button mCloneButton;

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

        private Profile getSelectedProfile() {
            return mListView.getSelectionModel().getSelectedItem();
        }

        private void createUI() {
            String fontFamily = mNameLabel.getFont().getFamily();
            double fontSize = mNameLabel.getFont().getSize();

            mNameLabel.setFont(Font.font(fontFamily, FontWeight.BOLD, fontSize * 2));
            mDescLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));
            mLastLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));

            mRunButton = new Button(null, MaterialIcon._Av.PLAY_ARROW.getImageView(ICON_SIZE_PROFILE));
            mEditButton = new Button(null, MaterialIcon._Content.CREATE.getImageView(ICON_SIZE_PROFILE));
            mCloneButton = new Button(null, MaterialIcon._Content.CONTENT_COPY.getImageView(ICON_SIZE_PROFILE));
            mRemoveButton = new Button(null, MaterialIcon._Content.REMOVE_CIRCLE_OUTLINE.getImageView(ICON_SIZE_PROFILE));

            mRunButton.setTooltip(new Tooltip(Dict.RUN.toString()));
            mEditButton.setTooltip(new Tooltip(Dict.EDIT.toString()));
            mCloneButton.setTooltip(new Tooltip(Dict.CLONE.toString()));
            mRemoveButton.setTooltip(new Tooltip(Dict.REMOVE.toString()));

            mRunButton.setOnAction((ActionEvent event) -> {
                System.out.println(event);
                mListView.requestFocus();
            });

            mEditButton.setOnAction((ActionEvent event) -> {
                System.out.println(event);
                mListView.requestFocus();
            });

            mCloneButton.setOnAction((ActionEvent event) -> {
                System.out.println(event);
                mListView.requestFocus();
            });

            mRemoveButton.setOnAction((ActionEvent event) -> {
                System.out.println(event);
                mListView.requestFocus();
            });

            VBox mainBox = new VBox(mNameLabel, mDescLabel, mLastLabel);
            mainBox.setAlignment(Pos.CENTER_LEFT);

            HBox buttonBox = new HBox(
                    mRunButton,
                    mEditButton,
                    mCloneButton,
                    mRemoveButton
            );
            buttonBox.setAlignment(Pos.CENTER_LEFT);
            buttonBox.setSpacing(8F);
            buttonBox.setVisible(false);

            mBorderPane.setCenter(mainBox);
            mBorderPane.setRight(buttonBox);

            mFadeInTransition.setNode(buttonBox);
            mFadeOutTransition.setNode(buttonBox);

            mBorderPane.setOnMouseEntered((MouseEvent event) -> {
                if (!buttonBox.isVisible()) {
                    buttonBox.setVisible(true);
                }

                selectListItem();
                mSummaryTextArea.setText(getSelectedProfile().toDebugString());
                mFadeInTransition.playFromStart();
            });

            mBorderPane.setOnMouseExited((MouseEvent event) -> {
                mFadeOutTransition.playFromStart();
            });
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
