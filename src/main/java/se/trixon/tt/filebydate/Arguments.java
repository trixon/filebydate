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
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.text.SimpleDateFormat;
import org.apache.commons.cli.CommandLine;

/**
 *
 * @author Patrik Karlsson
 */
public class Arguments {

    private String mDatePattern;
    private DateSource mDateSource;
    private String mDateSourceString;
    private File mDest;
    private boolean mDryRun;
    private String mFilePattern;
    private boolean mLinks;
    private boolean mModeCopy;
    private boolean mModeMove;
    private OperationMode mOperationMode;
    private PathMatcher mPathMatcher;
    private boolean mRecursive;
    private File mSource;
    private StringBuilder mValidationErrorBuilder = new StringBuilder();

    public Arguments(CommandLine commandLine) {
        mModeCopy = commandLine.hasOption("copy");
        mModeMove = commandLine.hasOption("move");

        mDatePattern = commandLine.getOptionValue("dp");
        mDateSourceString = commandLine.getOptionValue("ds");
        mFilePattern = commandLine.getOptionValue("fp");

        mDryRun = commandLine.hasOption("dry-run");
        mLinks = commandLine.hasOption("links");
        mRecursive = commandLine.hasOption("recursive");

        setSourceAndDest(commandLine.getArgs());
    }

    public String getDatePattern() {
        return mDatePattern;
    }

    public DateSource getDateSource() {
        return mDateSource;
    }

    public String getDateSourceString() {
        return mDateSourceString;
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

    public PathMatcher getPathMatcher() {
        return mPathMatcher;
    }

    public File getSource() {
        return mSource;
    }

    public String getValidationError() {
        return mValidationErrorBuilder.toString();
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
        if (mModeCopy == mModeMove) {
            addValidationError("Pick one operation of cp/mv");
        } else {
            mOperationMode = mModeCopy ? OperationMode.CP : OperationMode.MV;
        }

        try {
            mPathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + mFilePattern);
        } catch (Exception e) {
            addValidationError("invalid file pattern: " + mFilePattern);
        }

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(mDatePattern);
        } catch (Exception e) {
            addValidationError("invalid date pattern: " + mDatePattern);
        }

        try {
            mDateSource = DateSource.valueOf(mDateSourceString.toUpperCase());
        } catch (Exception e) {
            addValidationError("invalid date source: " + mDateSourceString);
        }

        if (mSource == null || !mSource.isDirectory()) {
            addValidationError("invalid source directory: " + mSource);
        }

        if (mDest == null || !mDest.isDirectory()) {
            addValidationError("invalid dest directory: " + mDest);
        }

        return mValidationErrorBuilder.length() == 0;
    }

    public void setDatePattern(String datePattern) {
        mDatePattern = datePattern;
    }

    public void setDateSource(DateSource dateSource) {
        mDateSource = dateSource;
    }

    public void setDateSourceString(String dateSourceString) {
        mDateSourceString = dateSourceString;
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

    public void setPathMatcher(PathMatcher pathMatcher) {
        mPathMatcher = pathMatcher;
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
            addValidationError("invalid arg count");
        }
    }

    @Override
    public String toString() {
        return "Arguments{"
                + "\n ModeCopy=" + mModeCopy
                + "\n ModeMove=" + mModeMove
                + "\n DatePattern=" + mDatePattern
                + "\n DateSource=" + mDateSourceString
                + "\n DryRun=" + mDryRun
                + "\n FilePattern=" + mFilePattern
                + "\n Links=" + mLinks
                + "\n Recursive=" + mRecursive
                + "\n Source=" + mSource
                + "\n Dest=" + mDest
                + "\n}";
    }

    private void addValidationError(String string) {
        mValidationErrorBuilder.append(string).append("\n");
    }
}
