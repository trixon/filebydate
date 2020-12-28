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
import com.dlsc.workbenchfx.view.controls.ToolbarItem;
import de.codecentric.centerdevice.MenuToolkit;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
import se.trixon.almond.util.icons.material.MaterialIcon;
import se.trixon.filebydate.FileByDate;
import se.trixon.filebydate.Options;

/**
 *
 * @author Patrik Karlström
 */
public class FbdApp extends Application {

    public static final String APP_TITLE = "FileByDate";
    public static final int ICON_SIZE_PROFILE = 32;
    public static final int ICON_SIZE_TOOLBAR = 40;
    public static final int ICON_SIZE_DRAWER = ICON_SIZE_TOOLBAR / 2;
    public static final int MODULE_ICON_SIZE = 32;
    private static final boolean IS_MAC = SystemUtils.IS_OS_MAC;
    private static final Logger LOGGER = Logger.getLogger(FbdApp.class.getName());
    private final AlmondFx mAlmondFX = AlmondFx.getInstance();
    private FbdModule mFbdModule;
    private FbdView mFbdView;
    private Action mHelpAction;
    private final Options mOptions = Options.getInstance();
    private Action mOptionsAction;
    private ToolbarItem mOptionsToolbarItem;
    private Stage mStage;
    private Workbench mWorkbench;
    private ToolbarItem mAddToolbarItem;
    private ToolbarItem mCancelToolbarItem;

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

        mAlmondFX.addStageWatcher(mStage, FbdApp.class);
        createUI();

        if (IS_MAC) {
            initMac();
        }

        mStage.getIcons().add(new Image(FbdApp.class.getResourceAsStream("calendar-icon-1024px.png")));
        mStage.setTitle(APP_TITLE);
        mStage.show();

        initAccelerators();
    }

    void setFbdView(FbdView fbdView) {
        mFbdView = fbdView;
    }

    private void createUI() {
        mWorkbench = Workbench.builder().build();
        mWorkbench.getStylesheets().add(FbdApp.class.getResource("customTheme.css").toExternalForm());

        initToolbar();
        initWorkbenchDrawer();

        mStage.setScene(new Scene(mWorkbench));
        mWorkbench.getModules().add(mFbdModule = new FbdModule(this));
    }

    private void displayOptions() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.initOwner(mStage);
        alert.initOwner(null);
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
//            profileEdit(null);
        });

        if (!IS_MAC) {
            accelerators.put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
                displayOptions();
            });
        }
    }

    private void initMac() {
        MenuToolkit menuToolkit = MenuToolkit.toolkit();
        Menu applicationMenu = menuToolkit.createDefaultApplicationMenu(APP_TITLE);
        menuToolkit.setApplicationMenu(applicationMenu);

        applicationMenu.getItems().remove(0);
        MenuItem aboutMenuItem = new MenuItem(String.format(Dict.ABOUT_S.toString(), APP_TITLE));
//        aboutMenuItem.setOnAction(mAboutAction);

        MenuItem settingsMenuItem = new MenuItem(Dict.PREFERENCES.toString());
        settingsMenuItem.setOnAction(mOptionsAction);
        settingsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));

        applicationMenu.getItems().add(0, aboutMenuItem);
        applicationMenu.getItems().add(2, settingsMenuItem);

        int cnt = applicationMenu.getItems().size();
        applicationMenu.getItems().get(cnt - 1).setText(String.format("%s %s", Dict.QUIT.toString(), APP_TITLE));
    }

    private void initToolbar() {
        mOptionsToolbarItem = new ToolbarItem(Dict.OPTIONS.toString(), MaterialIcon._Action.SETTINGS.getImageView(ICON_SIZE_TOOLBAR, Color.LIGHTGRAY),
                actionEvent -> {
                    displayOptions();
                }
        );

        mAddToolbarItem = new ToolbarItem(Dict.ADD.toString(), MaterialIcon._Content.ADD.getImageView(ICON_SIZE_TOOLBAR, Color.LIGHTGRAY), event -> {
            mFbdView.profileEdit(null);
        });

        mCancelToolbarItem = new ToolbarItem(MaterialIcon._Navigation.CANCEL.getImageView(ICON_SIZE_TOOLBAR, Color.LIGHTGRAY), event -> {
//                    mFbdView.doCancel();
        });
        mCancelToolbarItem.setTooltip(new Tooltip(Dict.CANCEL.toString()));

        mWorkbench.getToolbarControlsRight().setAll(
                mOptionsToolbarItem,
                mAddToolbarItem,
                mCancelToolbarItem
        );
    }

    private void initWorkbenchDrawer() {
        //options
        mOptionsAction = new Action(Dict.OPTIONS.toString(), actionEvent -> {
            mWorkbench.hideNavigationDrawer();
            displayOptions();
        });
        mOptionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
        mOptionsAction.setGraphic(MaterialIcon._Action.SETTINGS.getImageView(ICON_SIZE_DRAWER));

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

            AboutModel aboutModel = new AboutModel(
                    SystemHelper.getBundle(FileByDate.class, "about"),
                    SystemHelperFx.getResourceAsImageView(FbdApp.class, "logo_small.png")
            );

            AboutPane aboutPane = new AboutPane(aboutModel);

            double scaledFontSize = FxHelper.getScaledFontSize();
            Label appLabel = new Label(aboutModel.getAppName());
            appLabel.setFont(new Font(scaledFontSize * 1.8));
            Label verLabel = new Label(String.format("%s %s", Dict.VERSION.toString(), aboutModel.getAppVersion()));
            verLabel.setFont(new Font(scaledFontSize * 1.2));
            Label dateLabel = new Label(aboutModel.getAppDate());
            dateLabel.setFont(new Font(scaledFontSize * 1.2));

            VBox box = new VBox(appLabel, verLabel, dateLabel);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(0, 0, 0, 22));
            BorderPane topBorderPane = new BorderPane(box);
            topBorderPane.setLeft(aboutModel.getImageView());
            topBorderPane.setPadding(new Insets(22));
            BorderPane mainBorderPane = new BorderPane(aboutPane);
            mainBorderPane.setTop(topBorderPane);

            WorkbenchDialog dialog = WorkbenchDialog.builder(Dict.ABOUT.toString(), mainBorderPane, ButtonType.CLOSE).build();
            mWorkbench.showDialog(dialog);
        });

        mWorkbench.getNavigationDrawerItems().setAll(
                ActionUtils.createMenuItem(aboutDateFormatAction),
                ActionUtils.createMenuItem(aboutAction),
                ActionUtils.createMenuItem(mHelpAction)
        );

        if (!IS_MAC) {
            mOptionsAction.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
        }
    }

}
