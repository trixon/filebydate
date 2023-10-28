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
package se.trixon.filebydate.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.openide.modules.Places;
import org.openide.util.Exceptions;

/**
 *
 * @author Patrik Karlström
 */
public class StorageManager {

    private final File mHistoryFile;
    private final File mLogFile;
    private final File mTasksBackupFile;
    private final File mTasksFile;
    private Storage mStorage = new Storage();
    private final TaskManager mTaskManager = TaskManager.getInstance();
    private final File mUserDirectory;

    public static StorageManager getInstance() {
        return Holder.INSTANCE;
    }

    public static void save() {
        try {
            StorageManager.getInstance().saveToFile();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private StorageManager() {
        mUserDirectory = Places.getUserDirectory();

        mTasksFile = new File(mUserDirectory, "config.json");
        mTasksBackupFile = new File(mUserDirectory, "config.bak");
        mHistoryFile = new File(mUserDirectory, "var/history");
        mLogFile = new File(mUserDirectory, "var/filebydate.log");
    }

    public int getFileFormatVersion() {
        return mStorage.getFileFormatVersion();
    }

    public File getHistoryFile() {
        return mHistoryFile;
    }

    public File getLogFile() {
        return mLogFile;
    }

    public File getTasksFile() {
        return mTasksFile;
    }

    public TaskManager getTaskManager() {
        return mTaskManager;
    }

    public File getUserDirectory() {
        return mUserDirectory;
    }

    public void load() throws IOException {
        if (mTasksFile.exists()) {
            mStorage = Storage.open(mTasksFile);

            var taskItems = mTaskManager.getIdToItem();
            taskItems.clear();
            taskItems.putAll(mStorage.getTasks());
        } else {
            mStorage = new Storage();
        }
    }

    private void saveToFile() throws IOException {
        mStorage.setTasks(mTaskManager.getIdToItem());
        String json = mStorage.save(mTasksFile);
        String tag = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        FileUtils.writeStringToFile(mTasksBackupFile, String.format("%s=%s\n", tag, json), Charset.defaultCharset(), true);

        load(); //This will refresh and sort ListViews
    }

    private static class Holder {

        private static final StorageManager INSTANCE = new StorageManager();
    }
}
