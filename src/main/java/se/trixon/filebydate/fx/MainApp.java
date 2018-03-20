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

import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import se.trixon.almond.util.fx.AlmondFx;
import se.trixon.filebydate.Options;

/**
 *
 * @author Patrik Karlsson
 */
public class MainApp extends Application {

    public static final String APP_TITLE = "FileByDate";
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());
    private final AlmondFx mAlmondFX = AlmondFx.getInstance();
    private final Options mOptions = Options.getInstance();
    private BorderPane mRoot;
    private Stage mStage;
    private ListView<String> mListView;
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
        mAlmondFX.addStageWatcher(stage, MainApp.class);
        createUI();
        mStage.setTitle(APP_TITLE);
        mStage.show();
    }

    private void createUI() {
        mListView = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList(
                "Profile #1",
                "Profile #2",
                "Profile #3",
                "Profile #4"
        );
        mListView.setItems(items);
        mToolBar = new ToolBar(
                new Button("Run"),
                new Separator(),
                new Button("+"),
                new Button("-"),
                new Button("/"),
                new Button("D"),
                new Button("X"),
                new Separator(),
                new Button("?"),
                new Button("S")
        );
        mRoot = new BorderPane();
        Text summaryText = new Text("summary");
        mRoot.setTop(mToolBar);
        mRoot.setCenter(mListView);
        mRoot.setBottom(summaryText);
        Scene scene = new Scene(mRoot);
        mStage.setScene(scene);

        mOptions.getPreferences().addPreferenceChangeListener((PreferenceChangeEvent evt) -> {
            switch (evt.getKey()) {
                default:
            }
        });
    }
}
