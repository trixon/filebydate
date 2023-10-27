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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class ProfileManager {

    private final ObjectProperty<ObservableMap<String, Profile>> mIdToItemProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ObservableList<Profile>> mItemsProperty = new SimpleObjectProperty<>();

    public static ProfileManager getInstance() {
        return Holder.INSTANCE;
    }

    private ProfileManager() {
        mItemsProperty.setValue(FXCollections.observableArrayList());
        mIdToItemProperty.setValue(FXCollections.observableHashMap());

        mIdToItemProperty.get().addListener((MapChangeListener.Change<? extends String, ? extends Profile> change) -> {
            var values = new ArrayList<Profile>(getIdToItem().values());
            values.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            getItems().setAll(values);
        });
    }

    public boolean exists(Profile item) {
        return getIdToItem().containsValue(item);
    }

    public boolean exists(String name) {
        return getItems().stream()
                .anyMatch(item -> (StringUtils.equalsIgnoreCase(name, item.getName())));
    }

    public Profile getById(String id) {
        return getIdToItem().get(id);
    }

    public final ObservableMap<String, Profile> getIdToItem() {
        return mIdToItemProperty.get();
    }

    public final ObservableList<Profile> getItems() {
        return mItemsProperty.get();
    }

    public List<Profile> getTasks(ArrayList<String> taskIds) {
        var tasks = new ArrayList<Profile>();

        taskIds.forEach(id -> {
            var task = getById(id);
            if (task != null) {
                tasks.add(task);
            }
        });

        return tasks;
    }

    public boolean hasActiveTasks() {
        return false;
    }

    public ObjectProperty<ObservableList<Profile>> itemsProperty() {
        return mItemsProperty;
    }

    public void log(String message) {
        System.out.println(message);
    }

    public Profile save() throws IOException {
        return null;
    }

    private static class Holder {

        private static final ProfileManager INSTANCE = new ProfileManager();
    }
}
