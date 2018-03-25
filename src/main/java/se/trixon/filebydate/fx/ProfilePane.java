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

import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.fx.control.FileChooserPane;
import se.trixon.filebydate.DateSource;
import se.trixon.filebydate.NameCase;
import se.trixon.filebydate.Profile;
import se.trixon.filebydate.ui.MainFrame;

/**
 *
 * @author Patrik Karlsson
 */
public class ProfilePane extends GridPane {

    private final ResourceBundle mBundleUI = SystemHelper.getBundle(MainFrame.class, "Bundle");

    private ComboBox<NameCase> mCaseBaseComboBox;
    private ComboBox<NameCase> mCaseExtComboBox;
    private ComboBox<String> mDatePatternComboBox;
    private ComboBox<DateSource> mDateSourceComboBox;
    private TextField mDescTextField;
    private FileChooserPane mDestFileChooserPane;
    private ComboBox<String> mFilePatternComboBox;
    private CheckBox mLinksCheckBox;
    private TextField mNameTextField;
    private ComboBox<String> mOperationComboBox;
    private final Profile mProfile;
    private CheckBox mRecursiveCheckBox;
    private CheckBox mReplaceCheckBox;
    private FileChooserPane mSourceFileChooserPane;

    public ProfilePane(Profile profile) {
        mProfile = profile;
        createUI();
        if (profile == null) {

        } else {
            mNameTextField.setText(mProfile.getName());
            mDescTextField.setText("FIXME FIXME FIXME FIXME ");
            mSourceFileChooserPane.setPath(mProfile.getSourceDir());
            mDestFileChooserPane.setPath(mProfile.getDestDir());
            mFilePatternComboBox.setValue(mProfile.getFilePattern());
            mDateSourceComboBox.setValue(mProfile.getDateSource());
            mDatePatternComboBox.setValue(mProfile.getDatePattern());
            mOperationComboBox.getSelectionModel().select(mProfile.getOperation());
            mLinksCheckBox.setSelected(mProfile.isFollowLinks());
            mRecursiveCheckBox.setSelected(mProfile.isRecursive());
            mReplaceCheckBox.setSelected(mProfile.isReplaceExisting());
            mCaseBaseComboBox.setValue(mProfile.getCaseBase());
            mCaseExtComboBox.setValue(mProfile.getCaseExt());
        }
    }

    private void createUI() {
        setGridLinesVisible(true);

        Label nameLabel = new Label(Dict.NAME.toString());
        Label descLabel = new Label(Dict.DESCRIPTION.toString());
        Label sourceLabel = new Label(Dict.SOURCE.toString());
        Label destLabel = new Label(Dict.DESTINATION.toString());
        Label filePatternLabel = new Label(Dict.FILE_PATTERN.toString());
        Label dateSourceLabel = new Label(Dict.DATE_SOURCE.toString());
        Label datePatternLabel = new Label(Dict.DATE_PATTERN.toString());
        Label operationLabel = new Label(Dict.OPERATION.toString());
        Label caseBaseLabel = new Label(Dict.BASENAME.toString());
        Label caseExtLabel = new Label(Dict.EXTENSION.toString());

        mLinksCheckBox = new CheckBox(Dict.FOLLOW_LINKS.toString());
        mRecursiveCheckBox = new CheckBox(Dict.RECURSIVE.toString());
        mReplaceCheckBox = new CheckBox(Dict.REPLACE.toString());

        mCaseBaseComboBox = new ComboBox<>();
        mDatePatternComboBox = new ComboBox<>();
        mDateSourceComboBox = new ComboBox<>();
        mFilePatternComboBox = new ComboBox<>();
        mOperationComboBox = new ComboBox<>();
        mCaseExtComboBox = new ComboBox<>();

        mNameTextField = new TextField();
        mDescTextField = new TextField();

        mSourceFileChooserPane = new FileChooserPane();
        mDestFileChooserPane = new FileChooserPane();

        mFilePatternComboBox.setEditable(true);
        mDatePatternComboBox.setEditable(true);
        int col = 0;
        int row = 0;

        add(nameLabel, 0, 0, REMAINING, 1);
        add(mNameTextField, 0, ++row, REMAINING, 1);
        add(descLabel, 0, ++row, REMAINING, 1);
        add(mDescTextField, 0, ++row, REMAINING, 1);
        add(sourceLabel, 0, ++row, REMAINING, 1);
        add(mSourceFileChooserPane, 0, ++row, REMAINING, 1);
        add(destLabel, 0, ++row, REMAINING, 1);
        add(mDestFileChooserPane, 0, ++row, REMAINING, 1);

        addRow(++row, filePatternLabel, dateSourceLabel, datePatternLabel);
        addRow(++row, mFilePatternComboBox, mDateSourceComboBox, mDatePatternComboBox);
        GridPane.setHgrow(mFilePatternComboBox, Priority.ALWAYS);
        GridPane.setHgrow(mDateSourceComboBox, Priority.ALWAYS);
        GridPane.setHgrow(mDatePatternComboBox, Priority.ALWAYS);

        mFilePatternComboBox.setMaxWidth(Double.MAX_VALUE);
        mDateSourceComboBox.setMaxWidth(Double.MAX_VALUE);
        mDatePatternComboBox.setMaxWidth(Double.MAX_VALUE);

        GridPane subPane = new GridPane();
        subPane.setGridLinesVisible(true);
        subPane.addRow(0, operationLabel, new Label(), new Label(), new Label(), caseBaseLabel, caseExtLabel);
        subPane.addRow(1, mOperationComboBox, mLinksCheckBox, mRecursiveCheckBox, mReplaceCheckBox, mCaseBaseComboBox, mCaseExtComboBox);
        add(subPane, 0, ++row, REMAINING, 1);

        mFilePatternComboBox.setItems(FXCollections.observableArrayList(
                "*",
                "{*.jpg,*.JPG}",
                "{*.mp4,*.MP4}"
        ));
        mDatePatternComboBox.setItems(FXCollections.observableArrayList(
                "yyyy/MM/yyyy-MM-dd",
                "yyyy/MM/yyyy-MM-dd/HH",
                "yyyy/MM/dd",
                "yyyy/ww",
                "yyyy/ww/u"
        ));
        mCaseBaseComboBox.setItems(FXCollections.observableArrayList(Arrays.asList(NameCase.values())));
        mCaseExtComboBox.setItems(FXCollections.observableArrayList(Arrays.asList(NameCase.values())));
        mDateSourceComboBox.setItems(FXCollections.observableArrayList(Arrays.asList(DateSource.values())));
        mOperationComboBox.setItems(FXCollections.observableArrayList(Arrays.asList(mBundleUI.getString("operations").split("\\|"))));
    }
}
