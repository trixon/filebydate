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
package se.trixon.filebydate.boot;

import java.io.IOException;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.almond.util.icons.material.MaterialIcon;
import se.trixon.filebydate.Options;
import se.trixon.filebydate.core.StorageManager;

public class Installer extends ModuleInstall {

    static boolean GUI = true;
    private final Options mOptions = Options.getInstance();
    private final StorageManager mStorageManager = StorageManager.getInstance();
//    private final TaskManager mTaskManager = TaskManager.getInstance();

    @Override
    public boolean closing() {
        boolean exit = true;

//        if (mTaskManager.hasActiveTasks()) {
//            var d = new NotifyDescriptor(
//                    NbBundle.getMessage(Installer.class, "confirmExit"),
//                    "filebydate",
//                    NotifyDescriptor.YES_NO_OPTION,
//                    NotifyDescriptor.WARNING_MESSAGE,
//                    null,
//                    null);
//            var result = DialogDisplayer.getDefault().notify(d);
//            exit = result == NotifyDescriptor.YES_OPTION;
//        }
        return exit;
    }

    @Override
    public void restored() {
        var key = "laf";
        var preferences = NbPreferences.root().node("laf");
        var nightMode = StringUtils.containsIgnoreCase(preferences.get(key, ""), "dark");
        if (nightMode) {
            FxHelper.setDarkThemeEnabled(nightMode);
            MaterialIcon.setDefaultColor(Color.valueOf("D3D3D3"));
        }

        try {
            mStorageManager.load();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        //initStorage();
    }

    private void initStorage() {
//        var taskManager = mStorageManager.getTaskManager();
//
//        var task = new Task();
//        task.setName("Task %d %s".formatted(taskManager.getIdToItem().size(), RandomStringUtils.random(5, true, false)));
//        task.setDescription(RandomStringUtils.random(15, true, true));
//        taskManager.getIdToItem().put(task.getId(), task);
//
//        StorageManager.save();
    }

}
