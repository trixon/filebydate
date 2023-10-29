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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.openide.DialogDescriptor;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.almond.util.fx.control.FileChooserPane;
import se.trixon.almond.util.fx.control.FileChooserPane.ObjectMode;
import se.trixon.filebydate.Options;
import se.trixon.filebydate.core.DateSource;
import se.trixon.filebydate.core.NameCase;
import se.trixon.filebydate.core.Operation.Command;
import se.trixon.filebydate.core.StorageManager;
import se.trixon.filebydate.core.Task;
import se.trixon.filebydate.core.TaskManager;

/**
 *
 * @author Patrik Karlström
 */
public class TaskEditor extends GridPane {

    private ComboBox<NameCase> mCaseBaseComboBox;
    private ComboBox<NameCase> mCaseExtComboBox;
    private ComboBox<String> mDatePatternComboBox;
    private Label mDatePatternLabel;
    private ComboBox<DateSource> mDateSourceComboBox;
    private TextField mDescTextField;
    private FileChooserPane mDestChooserPane;
    private DialogDescriptor mDialogDescriptor;
    private ComboBox<String> mFilePatternComboBox;
    private CheckBox mLinksCheckBox;
    private TextField mNameTextField;
    private ComboBox<Command> mOperationComboBox;
    private final Options mOptions = Options.getInstance();
    private CheckBox mRecursiveCheckBox;
    private CheckBox mReplaceCheckBox;
    private FileChooserPane mSourceChooserPane;
    private Task mTask;
    private final TaskManager mTaskManager = TaskManager.getInstance();

    public TaskEditor() {
        createUI();

        initListeners();

        Platform.runLater(() -> {
            initValidation();
            mNameTextField.requestFocus();
        });
    }

    public void load(Task task, DialogDescriptor dialogDescriptor) {
        if (task == null) {
            task = new Task();
        }
        mDialogDescriptor = dialogDescriptor;
        mTask = task;
        mNameTextField.setText(task.getName());
        mDescTextField.setText(task.getDescription());
        mSourceChooserPane.setPath(task.getSourceDir());
        mDestChooserPane.setPath(task.getDestDir());
        mFilePatternComboBox.setValue(task.getFilePattern());
        mDateSourceComboBox.setValue(task.getDateSource());
        mDatePatternComboBox.setValue(task.getDatePattern());
        mOperationComboBox.getSelectionModel().select(task.getCommand());
        mLinksCheckBox.setSelected(task.isFollowLinks());
        mRecursiveCheckBox.setSelected(task.isRecursive());
        mReplaceCheckBox.setSelected(task.isReplaceExisting());
        mCaseBaseComboBox.setValue(task.getCaseBase());
        mCaseExtComboBox.setValue(task.getCaseExt());
    }

    public Task save() {
        mTaskManager.getIdToItem().put(mTask.getId(), mTask);

        mTask.setName(mNameTextField.getText().trim());
        mTask.setDescription(mDescTextField.getText());
        mTask.setSourceDir(mSourceChooserPane.getPath());
        mTask.setDestDir(mDestChooserPane.getPath());
        mTask.setFilePattern(mFilePatternComboBox.getValue());
        mTask.setDateSource(mDateSourceComboBox.getValue());
        mTask.setDatePattern(mDatePatternComboBox.getValue());
        mTask.setOperation(mOperationComboBox.getSelectionModel().getSelectedIndex());
        mTask.setFollowLinks(mLinksCheckBox.isSelected());
        mTask.setRecursive(mRecursiveCheckBox.isSelected());
        mTask.setReplaceExisting(mReplaceCheckBox.isSelected());
        mTask.setCaseBase(mCaseBaseComboBox.getValue());
        mTask.setCaseExt(mCaseExtComboBox.getValue());

        StorageManager.save();

        return mTask;
    }

    private void createUI() {
        //setGridLinesVisible(true);

        var nameLabel = new Label(Dict.NAME.toString());
        var descLabel = new Label(Dict.DESCRIPTION.toString());
        var filePatternLabel = new Label(Dict.FILE_PATTERN.toString());
        var dateSourceLabel = new Label(Dict.DATE_SOURCE.toString());
        mDatePatternLabel = new Label(Dict.DATE_PATTERN.toString());
        var operationLabel = new Label(Dict.OPERATION.toString());
        var caseBaseLabel = new Label(Dict.BASENAME.toString());
        var caseExtLabel = new Label(Dict.EXTENSION.toString());

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

        mSourceChooserPane = new FileChooserPane(Dict.OPEN.toString(), Dict.SOURCE.toString(), ObjectMode.DIRECTORY, SelectionMode.SINGLE);
        mDestChooserPane = new FileChooserPane(Dict.OPEN.toString(), Dict.DESTINATION.toString(), ObjectMode.DIRECTORY, SelectionMode.SINGLE);

        mFilePatternComboBox.setEditable(true);
        mDatePatternComboBox.setEditable(true);
        //mDatePatternLabel.setPrefWidth(300);

        int col = 0;
        int row = 0;
        nameLabel.setPrefWidth(9999);
        add(nameLabel, col, row, REMAINING, 1);
        add(mNameTextField, col, ++row, REMAINING, 1);
        add(descLabel, col, ++row, REMAINING, 1);
        add(mDescTextField, col, ++row, REMAINING, 1);
        add(mSourceChooserPane, col, ++row, REMAINING, 1);
        add(mDestChooserPane, col, ++row, REMAINING, 1);

        var patternPane = new GridPane();
        patternPane.addRow(0, filePatternLabel, dateSourceLabel, mDatePatternLabel);
        patternPane.addRow(1, mFilePatternComboBox, mDateSourceComboBox, mDatePatternComboBox);
        patternPane.setHgap(8);
        addRow(++row, patternPane);

        GridPane.setHgrow(mFilePatternComboBox, Priority.ALWAYS);
        GridPane.setHgrow(mDateSourceComboBox, Priority.ALWAYS);
        GridPane.setHgrow(mDatePatternComboBox, Priority.ALWAYS);

        GridPane.setFillWidth(mFilePatternComboBox, true);
        GridPane.setFillWidth(mDateSourceComboBox, true);
        GridPane.setFillWidth(mDatePatternComboBox, true);

        double width = 100.0 / 3.0;
        var col1 = new ColumnConstraints();
        col1.setPercentWidth(width);
        var col2 = new ColumnConstraints();
        col2.setPercentWidth(width);
        var col3 = new ColumnConstraints();
        col3.setPercentWidth(width);
        patternPane.getColumnConstraints().addAll(col1, col2, col3);

        mFilePatternComboBox.setMaxWidth(Double.MAX_VALUE);
        mDateSourceComboBox.setMaxWidth(Double.MAX_VALUE);
        mDatePatternComboBox.setMaxWidth(Double.MAX_VALUE);
        GridPane subPane = new GridPane();
        //subPane.setGridLinesVisible(true);
        subPane.addRow(0, operationLabel, new Label(), new Label(), new Label(), caseBaseLabel, caseExtLabel);
        subPane.addRow(1, mOperationComboBox, mLinksCheckBox, mRecursiveCheckBox, mReplaceCheckBox, mCaseBaseComboBox, mCaseExtComboBox);
        subPane.setHgap(8);
        add(subPane, col, ++row, REMAINING, 1);

        var rowInsets = FxHelper.getUIScaledInsets(0, 0, 8, 0);

        GridPane.setMargin(mNameTextField, rowInsets);
        GridPane.setMargin(mDescTextField, rowInsets);
        GridPane.setMargin(mSourceChooserPane, rowInsets);
        GridPane.setMargin(mDestChooserPane, rowInsets);
        GridPane.setMargin(patternPane, rowInsets);

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
        mOperationComboBox.setItems(FXCollections.observableArrayList(Arrays.asList(Command.COPY, Command.MOVE)));
    }

    private void initListeners() {
    }

    private void initValidation() {
        final String text_is_required = "Text is required";
        boolean indicateRequired = false;

        var namePredicate = (Predicate<String>) s -> {
            return mTaskManager.isValid(mTask.getName(), s);
        };
        var datePredicate = (Predicate<String>) s -> {
            return !StringUtils.isBlank(s) && previewDateFormat();
        };

        var uniqueNamePredicate = (Predicate<String>) s -> {
            var newName = mNameTextField.getText();
            if (!mTaskManager.exists(newName)) {
                return true;
            } else {
                return StringUtils.equalsIgnoreCase(newName, mTask.getName());
            }
        };

        var validationSupport = new ValidationSupport();
        validationSupport.registerValidator(mNameTextField, indicateRequired, Validator.createEmptyValidator(text_is_required));
        validationSupport.registerValidator(mNameTextField, indicateRequired, Validator.createPredicateValidator(namePredicate, text_is_required));
        validationSupport.registerValidator(mNameTextField, indicateRequired, Validator.createPredicateValidator(uniqueNamePredicate, text_is_required));
        validationSupport.registerValidator(mDescTextField, indicateRequired, Validator.createEmptyValidator(text_is_required));
        validationSupport.registerValidator(mSourceChooserPane.getTextField(), indicateRequired, Validator.createEmptyValidator(text_is_required));
        validationSupport.registerValidator(mDestChooserPane.getTextField(), indicateRequired, Validator.createEmptyValidator(text_is_required));
        validationSupport.registerValidator(mFilePatternComboBox, indicateRequired, Validator.createEmptyValidator(text_is_required));
        validationSupport.registerValidator(mDatePatternComboBox, indicateRequired, Validator.createEmptyValidator(text_is_required));
        validationSupport.registerValidator(mDatePatternComboBox, indicateRequired, Validator.createPredicateValidator(datePredicate, text_is_required));

        validationSupport.validationResultProperty().addListener((p, o, n) -> {
            mDialogDescriptor.setValid(!validationSupport.isInvalid());
        });

        mFilePatternComboBox.getEditor().textProperty().addListener((p, o, n) -> {
            mFilePatternComboBox.setValue(n);
        });

        mDatePatternComboBox.getEditor().textProperty().addListener((p, o, n) -> {
            mDatePatternComboBox.setValue(n);
        });

        validationSupport.initInitialDecoration();
    }

    private boolean previewDateFormat() {
        boolean validFormat = true;
        String datePreview;

        try {
            var simpleDateFormat = new SimpleDateFormat(mDatePatternComboBox.getValue(), mOptions.getLocale());
            datePreview = simpleDateFormat.format(new Date(System.currentTimeMillis()));
        } catch (IllegalArgumentException ex) {
            datePreview = Dict.Dialog.ERROR.toString();
            validFormat = false;
        }

        String dateLabel = String.format("%s (%s)", Dict.DATE_PATTERN.toString(), datePreview);
        mDatePatternLabel.setText(dateLabel);
        mDatePatternLabel.setTooltip(new Tooltip(datePreview));

        return validFormat;
    }
}
