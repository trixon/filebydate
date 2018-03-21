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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import se.trixon.almond.util.fx.AlmondFx;
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
//        mListView.getSelectionModel().select(0);
    }

    private void createUI() {
        mListView = new ListView<>();
        mListView.setItems(mItems);
        mListView.setCellFactory((ListView<Profile> param) -> new ProfileListCell());

        mToolBar = new ToolBar(
                new Button("+"),
                new Button("X"),
                new Separator(),
                new Button("?"),
                new Button("S")
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
            mFadeInTransition.setFromValue(0.0);
            mFadeInTransition.setToValue(1.0);

            mFadeOutTransition.setDuration(mDuration);
            mFadeOutTransition.setFromValue(1);
            mFadeOutTransition.setToValue(0);

            initLayout();
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

        private void initLayout() {
            String fontFamily = mNameLabel.getFont().getFamily();
            double fontSize = mNameLabel.getFont().getSize();

            mNameLabel.setFont(Font.font(fontFamily, FontWeight.BOLD, fontSize * 2));
            mDescLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));
            mLastLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));

            Button runButton = new Button("run");
            runButton.setOnAction((ActionEvent event) -> {
                System.out.println(getSelectedProfile());
                mListView.requestFocus();
            });

            VBox vBox = new VBox(mNameLabel, mDescLabel, mLastLabel);
            VBox buttonBox = new VBox(
                    runButton,
                    new Button("-"),
                    new Button("/"),
                    new Button("D")
            );

            buttonBox.setVisible(false);
            mBorderPane.setCenter(vBox);
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
