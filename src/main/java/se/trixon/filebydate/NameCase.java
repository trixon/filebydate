/* 
 * Copyright 2018 Patrik Karlström.
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

import java.util.ResourceBundle;
import se.trixon.almond.util.SystemHelper;
import se.trixon.filebydate.ui.MainFrame;

/**
 *
 * @author Patrik Karlström
 */
public enum NameCase {
    UNCHANGED, LOWER, UPPER;
    private final ResourceBundle mBundleUI = SystemHelper.getBundle(MainFrame.class, "Bundle");

    public static NameCase getCase(String key) {
        if (key != null) {
            key = key.toLowerCase();
            if (key.equalsIgnoreCase("l") || key.equalsIgnoreCase("lower")) {
                return LOWER;
            } else if (key.equalsIgnoreCase("u") || key.equalsIgnoreCase("upper")) {
                return UPPER;
            }
        }

        return null;
    }

    private NameCase() {
    }

    @Override
    public String toString() {
        return mBundleUI.getString("case_" + name().toLowerCase());
    }
}
