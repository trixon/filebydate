/*
 * Copyright 2016 Patrik Karlsson.
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
package se.trixon.tt.filebydate;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 *
 * @author Patrik Karlsson
 */
public class Arguments {

    private String mDatePattern;
    private String mDateSource;
    private File mDest;
    private boolean mDryRun;
    private String mFilePattern;
    private boolean mLinks;
    private boolean mModeCopy;
    private boolean mModeMove;
    private OperationMode mOperationMode;
    private boolean mRecursive;
    private File mSource;

    public String getDatePattern() {
        return mDatePattern;
    }

    public String getDateSource() {
        return mDateSource;
    }

    public File getDest() {
        return mDest;
    }

    public String getFilePattern() {
        return mFilePattern;
    }

    public OperationMode getOperationMode() {
        return mOperationMode;
    }

    public File getSource() {
        return mSource;
    }

    public boolean isDatePatternValid() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(mDatePattern);

        return true;
    }

    public boolean isDryRun() {
        return mDryRun;
    }

    public boolean isLinks() {
        return mLinks;
    }

    public boolean isModeCopy() {
        return mModeCopy;
    }

    public boolean isModeMove() {
        return mModeMove;
    }

    public boolean isRecursive() {
        return mRecursive;
    }

    public boolean isValid() {
        boolean valid = true;
        StringBuilder sb = new StringBuilder();

        valid = valid && mDatePattern != null;
        valid = valid && mDateSource != null;
        valid = valid && mFilePattern != null;
        valid = valid && mSource.isDirectory();

        if (mSource == null || !mSource.isDirectory()) {
            sb.append("invalid source directory: " + mSource).append("\n");
        }

        valid = valid && mDest.isDirectory();

        System.out.println(sb.toString());
        return valid;
    }

    public void setDatePattern(String datePattern) {
        mDatePattern = datePattern;
    }

    public void setDateSource(String dateSource) {
        mDateSource = dateSource;
    }

    public void setDest(File dest) {
        mDest = dest;
    }

    public void setDryRun(boolean dryRun) {
        mDryRun = dryRun;
    }

    public void setFilePattern(String filePattern) {
        mFilePattern = filePattern;
    }

    public void setLinks(boolean links) {
        mLinks = links;
    }

    public void setModeCopy(boolean modeCopy) {
        mModeCopy = modeCopy;
    }

    public void setModeMove(boolean modeMove) {
        mModeMove = modeMove;
    }

    public void setOperationMode(OperationMode operationMode) {
        mOperationMode = operationMode;
    }

    public void setRecursive(boolean recursive) {
        mRecursive = recursive;
    }

    public void setSource(File source) {
        mSource = source;
    }

    public void setSourceAndDest(String[] args) {
        if (args.length == 2) {
            setSource(new File(args[0]));
            setDest(new File(args[1]));
        } else {
            System.out.println("invalid arg count");
        }
    }

    @Override
    public String toString() {
        return "Arguments{"
                + "\n ModeCopy=" + mModeCopy
                + "\n ModeMove=" + mModeMove
                + "\n DatePattern=" + mDatePattern
                + "\n DateSource=" + mDateSource
                + "\n DryRun=" + mDryRun
                + "\n FilePattern=" + mFilePattern
                + "\n Links=" + mLinks
                + "\n Recursive=" + mRecursive
                + "\n Source=" + mSource
                + "\n Dest=" + mDest
                + "\n}";
    }
}
