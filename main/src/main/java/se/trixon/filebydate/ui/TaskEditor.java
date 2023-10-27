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

import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.openide.DialogDescriptor;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.fx.control.FileChooserPane;
import se.trixon.filebydate.core.Profile;
import se.trixon.filebydate.core.ProfileManager;
import se.trixon.filebydate.core.StorageManager;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class TaskEditor extends BorderPane {

    protected final ValidationSupport mValidationSupport = new ValidationSupport();
    private final TextField mDescTextField = new TextField();
    private DialogDescriptor mDialogDescriptor;
    private FileChooserPane mDirDestFileChooser;
    private Profile mItem;
    private final TextField mNameTextField = new TextField();
    private final ProfileManager mProfileManager = ProfileManager.getInstance();

    public TaskEditor() {
        createUI();
        initValidation();
    }

    public void load(Profile item, DialogDescriptor dialogDescriptor) {
        if (item == null) {
            item = new Profile();
        }
        mItem = item;
        mDialogDescriptor = dialogDescriptor;
        mNameTextField.setText(item.getName());
        mDescTextField.setText(item.getDescription());
//        mDirDestFileChooser.setPath(item.getDestination());
    }

    public Profile save() {
        mProfileManager.getIdToItem().put(mItem.getId(), mItem);

        mItem.setName(mNameTextField.getText());
        mItem.setDescription(mDescTextField.getText());
//        mItem.setDestination(mDirDestFileChooser.getPathAsString());

        StorageManager.save();

        return mItem;
    }

    private void createUI() {
        var nameLabel = new Label(Dict.NAME.toString());
        var descLabel = new Label(Dict.DESCRIPTION.toString());
        var sourceTitle = Dict.SOURCE.toString();
        var destTitle = Dict.DESTINATION.toString();
        var selectionMode = SelectionMode.SINGLE;
        var objectMode = FileChooserPane.ObjectMode.FILE;

        mDirDestFileChooser = new FileChooserPane(destTitle, destTitle, objectMode, selectionMode);

        var vbox = new VBox(
                nameLabel,
                mNameTextField,
                descLabel,
                mDescTextField,
                mDirDestFileChooser
        );

        setTop(vbox);
    }

    private void initValidation() {
        mValidationSupport.validationResultProperty().addListener((p, o, n) -> {
            mDialogDescriptor.setValid(!mValidationSupport.isInvalid());
        });

        mValidationSupport.initInitialDecoration();

        final String textRequired = "Text is required";
        final String textUnique = "Text has to be unique";

        Predicate uniqueNamePredicate = (Predicate) (Object o) -> {
            var newName = mNameTextField.getText();
            if (!mProfileManager.exists(newName)) {
                return true;
            } else {
                return StringUtils.equalsIgnoreCase(newName, mItem.getName());
            }
        };

        Platform.runLater(() -> {
            mValidationSupport.registerValidator(mNameTextField, true, Validator.combine(
                    Validator.createEmptyValidator(textRequired),
                    Validator.createPredicateValidator(uniqueNamePredicate, textUnique)
            ));
            mValidationSupport.registerValidator(mDescTextField, true, Validator.createEmptyValidator(textRequired));
            mValidationSupport.registerValidator(mDirDestFileChooser.getTextField(), true, Validator.createEmptyValidator(textRequired));
        });
    }

}