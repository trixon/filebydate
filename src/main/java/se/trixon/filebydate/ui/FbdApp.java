/*
 * Copyright 2019 Patrik Karlström.
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
import com.dlsc.workbenchfx.model.WorkbenchDialog;
import de.jangassen.MenuToolkit;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionUtils;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.SystemHelperFx;
import se.trixon.almond.util.fx.AboutModel;
import se.trixon.almond.util.fx.AlmondFx;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.almond.util.fx.control.LocaleComboBox;
import se.trixon.almond.util.fx.dialogs.about.AboutPane;
import se.trixon.filebydate.FileByDate;
import se.trixon.filebydate.Options;
import se.trixon.filebydate.RunStateManager;

/**
 *
 * @author Patrik Karlström
 */
public class FbdApp extends Application {

    public static final String APP_TITLE = "FileByDate";
    private static final boolean IS_MAC = SystemUtils.IS_OS_MAC;
    private static final Logger LOGGER = Logger.getLogger(FbdApp.class.getName());
    private final AlmondFx mAlmondFX = AlmondFx.getInstance();
    private FbdModule mFbdModule;
    private Action mHelpAction;
    private final Options mOptions = Options.getInstance();
    private Action mOptionsAction;
    private final RunStateManager mRunStateManager = RunStateManager.getInstance();
    private Stage mStage;
    private Workbench mWorkbench;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    public Action getOptionsAction() {
        return mOptionsAction;
    }

    @Override
    public void start(Stage stage) throws Exception {
        mStage = stage;

        createUI();
        if (IS_MAC) {
            initMac();
        }

        initAccelerators();

        FxHelper.runLaterDelayed(1, () -> {
            mStage.getIcons().add(new Image(FbdApp.class.getResourceAsStream("calendar-icon-1024px.png")));
            mStage.setTitle(APP_TITLE);
            mAlmondFX.addStageWatcher(mStage, FbdApp.class);
            mStage.show();
        });
    }

    private void createUI() {
        mWorkbench = Workbench.builder().build();
        mWorkbench.getStylesheets().add(FbdApp.class.getResource("customTheme.css").toExternalForm());

        initWorkbenchDrawer();

        mStage.setScene(new Scene(mWorkbench));
        mWorkbench.getModules().add(mFbdModule = new FbdModule());
    }

    private void displayOptions() {
        var label = new Label(Dict.CALENDAR_LANGUAGE.toString());
        var localeComboBox = new LocaleComboBox();
        var checkBox = new CheckBox(Dict.DYNAMIC_WORD_WRAP.toString());
        var gridPane = new GridPane();
        //gridPane.setGridLinesVisible(true);
        gridPane.addColumn(0, label, localeComboBox, checkBox);
        GridPane.setMargin(checkBox, new Insets(16, 0, 0, 0));

        localeComboBox.setValue(mOptions.getLocale());
        checkBox.setSelected(mOptions.isWordWrap());
        String title = Dict.OPTIONS.toString();

        var dialog = WorkbenchDialog.builder(title, gridPane, ButtonType.CANCEL, ButtonType.OK).onResult(buttonType -> {
            if (buttonType == ButtonType.OK) {
                mOptions.setLocale(localeComboBox.getValue());
                mOptions.setWordWrap(checkBox.isSelected());
            }
        }).build();

        mWorkbench.showDialog(dialog);
    }

    private void initAccelerators() {
        final ObservableMap<KeyCombination, Runnable> accelerators = mStage.getScene().getAccelerators();

        accelerators.put(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN), () -> {
            mStage.fireEvent(new WindowEvent(mStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        });

        if (!IS_MAC) {
            accelerators.put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), () -> {
                displayOptions();
            });
        }
    }

    private void initMac() {
        var menuToolkit = MenuToolkit.toolkit();
        var applicationMenu = menuToolkit.createDefaultApplicationMenu(APP_TITLE);
        menuToolkit.setApplicationMenu(applicationMenu);

        applicationMenu.getItems().remove(0);
        var aboutMenuItem = new MenuItem(String.format(Dict.ABOUT_S.toString(), APP_TITLE));
//        aboutMenuItem.setOnAction(mAboutAction);

        var settingsMenuItem = new MenuItem(Dict.PREFERENCES.toString());
        settingsMenuItem.setOnAction(mOptionsAction);
        settingsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));

        applicationMenu.getItems().add(0, aboutMenuItem);
        applicationMenu.getItems().add(2, settingsMenuItem);

        int cnt = applicationMenu.getItems().size();
        applicationMenu.getItems().get(cnt - 1).setText(String.format("%s %s", Dict.QUIT.toString(), APP_TITLE));
    }

    private void initWorkbenchDrawer() {
        //options
        mOptionsAction = new Action(Dict.OPTIONS.toString(), actionEvent -> {
            mWorkbench.hideNavigationDrawer();
            displayOptions();
        });
        mOptionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
        mOptionsAction.disabledProperty().bind(mRunStateManager.runningProperty());

        //help
        mHelpAction = new Action(Dict.HELP.toString(), actionEvent -> {
            mWorkbench.hideNavigationDrawer();
            SystemHelper.desktopBrowse("https://trixon.se/projects/filebydate/documentation/");
        });
        //mHelpAction.setAccelerator(new KeyCodeCombination(KeyCode.F1, KeyCombination.SHORTCUT_ANY));
        mHelpAction.setAccelerator(KeyCombination.keyCombination("F1"));

        String title = String.format(Dict.ABOUT_S.toString(), Dict.DATE_PATTERN.toString().toLowerCase());
        var aboutDateFormatAction = new Action(title, actionEvent -> {
            SystemHelper.desktopBrowse("https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/text/SimpleDateFormat.html");
        });

        //about
        var aboutAction = new Action(Dict.ABOUT.toString(), actionEvent -> {
            mWorkbench.hideNavigationDrawer();

            var aboutModel = new AboutModel(
                    SystemHelper.getBundle(FileByDate.class, "about"),
                    SystemHelperFx.getResourceAsImageView(FbdApp.class, "logo_small.png")
            );

            var aboutPane = new AboutPane(aboutModel);

            double scaledFontSize = FxHelper.getScaledFontSize();
            var appLabel = new Label(aboutModel.getAppName());
            appLabel.setFont(new Font(scaledFontSize * 1.8));
            var verLabel = new Label(String.format("%s %s", Dict.VERSION.toString(), aboutModel.getAppVersion()));
            verLabel.setFont(new Font(scaledFontSize * 1.2));
            var dateLabel = new Label(aboutModel.getAppDate());
            dateLabel.setFont(new Font(scaledFontSize * 1.2));

            var box = new VBox(appLabel, verLabel, dateLabel);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(0, 0, 0, 22));
            var topBorderPane = new BorderPane(box);
            topBorderPane.setLeft(aboutModel.getImageView());
            topBorderPane.setPadding(new Insets(22));
            var mainBorderPane = new BorderPane(aboutPane);
            mainBorderPane.setTop(topBorderPane);

            var dialog = WorkbenchDialog.builder(Dict.ABOUT.toString(), mainBorderPane, ButtonType.CLOSE).build();
            mWorkbench.showDialog(dialog);
        });

        mWorkbench.getNavigationDrawerItems().setAll(
                ActionUtils.createMenuItem(mOptionsAction),
                ActionUtils.createMenuItem(aboutDateFormatAction),
                ActionUtils.createMenuItem(aboutAction),
                ActionUtils.createMenuItem(mHelpAction)
        );

        if (!IS_MAC) {
            mOptionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
        }
    }
}
