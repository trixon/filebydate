/*
 * Copyright 2023 Patrik Karlström <patrik@trixon.se>.
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
package se.trixon.filebydate.ui.options;

import java.awt.BorderLayout;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javax.swing.JPanel;
import se.trixon.almond.nbp.fx.FxPanel;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.fx.control.LocaleComboBox;
import se.trixon.filebydate.Options;

final class MainPanel extends JPanel {

    private final MainPanelController mController;
    private final FxPanel mFxPanel;
    private LocaleComboBox mLocaleComboBox = new LocaleComboBox();
    private final Options mOptions = Options.getInstance();

    MainPanel(MainPanelController controller) {
        mController = controller;
        mFxPanel = new FxPanel() {

            @Override
            protected void fxConstructor() {
                setScene(createScene());
            }

            private Scene createScene() {
                var label = new Label(Dict.CALENDAR_LANGUAGE.toString());
                var gp = new GridPane();
                //gridPane.setGridLinesVisible(true);
                gp.addColumn(0, label, mLocaleComboBox);

                return new Scene(gp);
            }
        };
        mFxPanel.initFx(null);
        mFxPanel.setPreferredSize(null);

        setLayout(new BorderLayout());
        add(mFxPanel, BorderLayout.CENTER);
    }

    void load() {
        mLocaleComboBox.setValue(mOptions.getLocale());
    }

    void store() {
        mOptions.setLocale(mLocaleComboBox.getValue());
    }

    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }
}
