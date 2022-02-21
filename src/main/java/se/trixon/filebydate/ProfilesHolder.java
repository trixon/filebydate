/* 
 * Copyright 2022 Patrik Karlström.
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
package se.trixon.filebydate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import org.apache.commons.io.FileUtils;
import se.trixon.almond.util.gson_adapter.FileAdapter;

/**
 *
 * @author Patrik Karlström
 */
public class ProfilesHolder {

    private static final int FILE_FORMAT_VERSION = 2;
    private static final Gson GSON = new GsonBuilder()
            .setVersion(1.0)
            .serializeNulls()
            .setPrettyPrinting()
            .registerTypeAdapter(File.class, new FileAdapter())
            .create();
    @SerializedName("format_version")
    private int mFileFormatVersion;
    @SerializedName("profiles")
    private final LinkedList<Profile> mProfiles = new LinkedList<>();

    public static ProfilesHolder open(File file) throws IOException, JsonSyntaxException {
        String json = FileUtils.readFileToString(file, Charset.defaultCharset());

        ProfilesHolder profiles = GSON.fromJson(json, ProfilesHolder.class);

        if (profiles.mFileFormatVersion != FILE_FORMAT_VERSION) {
            //TODO Handle file format version change
        }

        return profiles;
    }

    public int getFileFormatVersion() {
        return mFileFormatVersion;
    }

    public LinkedList<Profile> getProfiles() {
        mProfiles.forEach((p) -> {
            p.setOperation(p.getCommand().ordinal());
        });

        return mProfiles;
    }

    public void save(File file) throws IOException {
        mFileFormatVersion = FILE_FORMAT_VERSION;
        FileUtils.writeStringToFile(file, GSON.toJson(this), Charset.defaultCharset());
    }
}
