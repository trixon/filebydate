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
package se.trixon.filebydate.ui;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.openide.util.NbBundle;
import se.trixon.almond.util.BooleanHelper;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.filebydate.core.Task;

/**
 *
 * @author Patrik Karlström
 */
public class TaskSummary extends VBox {

    private final Font mDefaultFont = Font.getDefault();
    private final Label mDescLabel = new Label();
    private final String mFontFamily;
    private final Label mNameLabel = new Label();
    private final SummaryDetails mSummaryDetails;

    public TaskSummary(Task task) {
        mFontFamily = mDefaultFont.getFamily();
        mSummaryDetails = new SummaryDetails(task);
        createUI();
        load(task);
    }

    private void createUI() {
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(
                mNameLabel,
                mDescLabel,
                new Separator(),
                mSummaryDetails
        );

        var fontSize = FxHelper.getScaledFontSize();
        mNameLabel.setFont(Font.font(mFontFamily, FontWeight.BOLD, fontSize * 1.6));
        mDescLabel.setFont(Font.font(mFontFamily, FontWeight.NORMAL, fontSize * 1.3));

        mDescLabel.setPadding(FxHelper.getUIScaledInsets(0, 0, 12, 0));
        mSummaryDetails.setPadding(FxHelper.getUIScaledInsets(12, 0, 0, 0));

        setPadding(FxHelper.getUIScaledInsets(0, 16, 0, 16));
    }

    private void load(Task task) {
        mNameLabel.setText(task.getName());
        mDescLabel.setText(task.getDescription());
    }

    class SummaryDetails extends TextFlow {

        private final Text mBasedOn = new Text();
        private final ResourceBundle mBundle = NbBundle.getBundle(MainTopComponent.class);
        private final Text mCase = new Text();
        private final Text mDest = new Text("\n");
        private final Text mFilesFrom = new Text(mBundle.getString("files_from"));
        private final Text mOperation = new Text();
        private final Text mOptions = new Text();
        private final Text mSource = new Text("\n");
        private final Text mTo = new Text(String.format(" %s\n", Dict.TO.toString().toLowerCase(Locale.getDefault())));

        public SummaryDetails(Task task) {
            createUI();
            load(task);
        }

        final void load(Task task) {
            mOperation.setText(task.getCommand().toString());
            mSource.setText(String.format("%s%s%s",
                    task.getSourceDirAsString(),
                    File.separator,
                    task.getFilePattern())
            );

            mDest.setText(String.format("%s%s%s\n",
                    task.getDestDirAsString(),
                    File.separator,
                    task.getDatePattern())
            );

            mBasedOn.setText(String.format("%s = '%s'\n",
                    Dict.DATE_SOURCE.toString(),
                    task.getDateSource().toString()
            ));

            var sb = new StringBuilder();
            sb.append(BooleanHelper.asCheckBox(task.isFollowLinks())).append("\u2009").append(Dict.FOLLOW_LINKS.toString()).append(", ");
            sb.append(BooleanHelper.asCheckBox(task.isRecursive())).append("\u2009").append(Dict.RECURSIVE.toString()).append(", ");
            sb.append(BooleanHelper.asCheckBox(task.isReplaceExisting())).append("\u2009").append(Dict.REPLACE.toString()).append(". ");
            mOptions.setText(sb.toString());

            String caseText = String.format("%s %s, %s %s",
                    Dict.BASENAME.toString(),
                    task.getCaseBase(),
                    Dict.EXTENSION.toString(),
                    task.getCaseExt()
            );

            mCase.setText(caseText);
        }

        private void createUI() {
            var redFillSet = Set.of(mOperation, mSource, mDest);

            getChildren().setAll(
                    mOperation,
                    mFilesFrom,
                    mSource,
                    mTo,
                    mDest,
                    mOptions,
                    mBasedOn,
                    mCase
            );

            var fontSize = FxHelper.getScaledFontSize() * 1.2;
            getChildren().stream()
                    .filter(node -> node instanceof Text)
                    .map(node -> (Text) node)
                    .forEachOrdered(text -> {
                        text.setFont(Font.font(fontSize));
                        if (redFillSet.contains(text)) {
                            text.setFill(Color.RED);
                        } else if (FxHelper.isDarkThemeEnabled()) {
                            text.setFill(FxHelper.getFillColorForDarkTheme());
                        }
                    });

            mOperation.setFont(Font.font(mDefaultFont.getName(), FontWeight.EXTRA_BOLD, fontSize));
        }
    }
}
