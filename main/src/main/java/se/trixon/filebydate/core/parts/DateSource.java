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
package se.trixon.filebydate.core.parts;

import java.util.Locale;
import java.util.ResourceBundle;
import org.openide.util.NbBundle;
import se.trixon.filebydate.ui.MainTopComponent;

/**
 *
 * @author Patrik Karlström
 */
public enum DateSource {

    EXIF_ORIGINAL,
    FILE_CREATED,
    FILE_MODIFIED;
    private final ResourceBundle mBundle = NbBundle.getBundle(MainTopComponent.class);

    private DateSource() {
    }

    @Override
    public String toString() {
        return mBundle.getString("dateSource_" + name().toLowerCase(Locale.ROOT));
    }
}