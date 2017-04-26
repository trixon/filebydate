/*
 * Copyright 2017 Patrik Karlsson.
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

import java.util.Locale;
import java.util.prefs.Preferences;

/**
 *
 * @author Patrik Karlsson
 */
public class Options {

    public static final String KEY_LOCALE = "locale";
    public static final String KEY_WORD_WRAP = "word_wrap";
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final boolean DEFAULT_WORD_WRAP = false;
    private final Preferences mPreferences = Preferences.userNodeForPackage(Options.class);

    public static Options getInstance() {
        return Holder.INSTANCE;
    }

    private Options() {
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(mPreferences.get(KEY_LOCALE, DEFAULT_LOCALE.toLanguageTag()));
    }

    public Preferences getPreferences() {
        return mPreferences;
    }

    public boolean isWordWrap() {
        return mPreferences.getBoolean(KEY_WORD_WRAP, DEFAULT_WORD_WRAP);
    }

    public void setLocale(Locale locale) {
        mPreferences.put(KEY_LOCALE, locale.toLanguageTag());
    }

    public void setWordWrap(boolean value) {
        mPreferences.putBoolean(KEY_WORD_WRAP, value);
    }

    private static class Holder {

        private static final Options INSTANCE = new Options();
    }
}
