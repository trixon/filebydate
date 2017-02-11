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

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import se.trixon.almond.util.BundleHelper;
import se.trixon.almond.util.Dict;
import se.trixon.filebydate.Operation.Command;
import se.trixon.filebydate.ui.MainFrame;

/**
 *
 * @author Patrik Karlsson
 */
public class Profile implements Comparable<Profile>, Cloneable {

    private NameCase mBaseNameCase = NameCase.UNCHANGED;
    private final ResourceBundle mBundle = BundleHelper.getBundle(Profile.class, "Bundle");
    private final ResourceBundle mBundleUI = BundleHelper.getBundle(MainFrame.class, "Bundle");
    private String mCaseBase;
    private String mCaseExt;
    private Command mCommand;
    private SimpleDateFormat mDateFormat;
    private String mDatePattern;
    private DateSource mDateSource = DateSource.FILE_CREATED;
    private String mDateSourceString;
    private File mDestDir;
    private boolean mDryRun;
    private NameCase mExtNameCase = NameCase.UNCHANGED;
    private String mFilePattern;
    private boolean mFollowLinks;
    private boolean mModeCopy;
    private boolean mModeMove;
    private String mName;
    private PathMatcher mPathMatcher;
    private boolean mRecursive;
    private boolean mReplaceExisting;
    private File mSourceDir;
    private StringBuilder mValidationErrorBuilder = new StringBuilder();

    public Profile() {
    }

    public Profile(CommandLine commandLine) {
        mModeCopy = commandLine.hasOption("copy");
        mModeMove = commandLine.hasOption("move");

        mDatePattern = commandLine.getOptionValue("dp");
        mDateSourceString = commandLine.getOptionValue("ds");
        mCaseBase = commandLine.getOptionValue("case-base");
        mCaseExt = commandLine.getOptionValue("case-ext");

        mDryRun = commandLine.hasOption("dry-run");
        mFollowLinks = commandLine.hasOption("links");
        mRecursive = commandLine.hasOption("recursive");
        mReplaceExisting = commandLine.hasOption("overwrite");

        setSourceAndDest(commandLine.getArgs());
    }

    @Override
    public Profile clone() {
        try {
            return (Profile) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Profile.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public int compareTo(Profile o) {
        return mName.compareTo(o.getName());
    }

    public NameCase getBaseNameCase() {
        return mBaseNameCase;
    }

    public String getCaseBase() {
        return mCaseBase;
    }

    public String getCaseExt() {
        return mCaseExt;
    }

    public Command getCommand() {
        return mCommand;
    }

    public SimpleDateFormat getDateFormat() {
        return mDateFormat;
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

    public File getDestDir() {
        return mDestDir;
    }

    public String getDestDirAsString() {
        return mDestDir == null ? "" : mDestDir.getAbsolutePath();
    }

    public NameCase getExtNameCase() {
        return mExtNameCase;
    }

    public String getFilePattern() {
        return mFilePattern;
    }

    public String getName() {
        return mName;
    }

    public int getOperation() {
        return mModeCopy ? 0 : 1;
    }

    public PathMatcher getPathMatcher() {
        return mPathMatcher;
    }

    public File getSourceDir() {
        return mSourceDir;
    }

    public String getSourceDirAsString() {
        return mSourceDir == null ? "" : mSourceDir.getAbsolutePath();
    }

    public String getValidationError() {
        return mValidationErrorBuilder.toString();
    }

    public boolean isDryRun() {
        return mDryRun;
    }

    public boolean isFollowLinks() {
        return mFollowLinks;
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

    public boolean isReplaceExisting() {
        return mReplaceExisting;
    }

    public boolean isValid() {
        mValidationErrorBuilder = new StringBuilder();

        if (mModeCopy == mModeMove) {
            addValidationError(mBundle.getString("invalid_command"));
        } else {
            mCommand = mModeCopy ? Command.COPY : Command.MOVE;
        }

        try {
            mPathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + mFilePattern);
        } catch (Exception e) {
            addValidationError("invalid file pattern: " + mFilePattern);
        }

        try {
            mDateFormat = new SimpleDateFormat(mDatePattern, Options.getInstance().getLocale());
        } catch (Exception e) {
            addValidationError(String.format(mBundle.getString("invalid_date_pattern"), mDatePattern));
        }

        if (mDateSourceString != null) {
            try {
                mDateSource = DateSource.valueOf(mDateSourceString.toUpperCase());
            } catch (Exception e) {
                addValidationError(String.format(mBundle.getString("invalid_date_source"), mDateSourceString));
            }
        }

        if (mCaseBase != null) {
            mBaseNameCase = NameCase.getCase(mCaseBase);
            if (mBaseNameCase == null) {
                addValidationError(String.format(mBundle.getString("invalid_case_base"), mCaseBase));
            }

        }

        if (mCaseExt != null) {
            mExtNameCase = NameCase.getCase(mCaseExt);
            if (mExtNameCase == null) {
                addValidationError(String.format(mBundle.getString("invalid_case_ext"), mCaseExt));
            }
        }

        if (mSourceDir == null || !mSourceDir.isDirectory()) {
            addValidationError(String.format(mBundle.getString("invalid_source_dir"), mSourceDir));
        }

        if (mDestDir == null || !mDestDir.isDirectory()) {
            addValidationError(String.format(mBundle.getString("invalid_dest_dir"), mDestDir));
        }

        return mValidationErrorBuilder.length() == 0;
    }

    public void setBaseNameCase(NameCase baseNameCase) {
        mBaseNameCase = baseNameCase;
    }

    public void setCaseBase(String caseBase) {
        mCaseBase = caseBase;
    }

    public void setCaseExt(String caseExt) {
        mCaseExt = caseExt;
    }

    public void setCommand(Command operationMode) {
        mCommand = operationMode;
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

    public void setDestDir(File dest) {
        mDestDir = dest;
    }

    public void setDryRun(boolean dryRun) {
        mDryRun = dryRun;
    }

    public void setExtNameCase(NameCase extNameCase) {
        mExtNameCase = extNameCase;
    }

    public void setFilePattern(String filePattern) {
        mFilePattern = filePattern;
    }

    public void setFollowLinks(boolean links) {
        mFollowLinks = links;
    }

    public void setModeCopy(boolean modeCopy) {
        mModeCopy = modeCopy;
    }

    public void setModeMove(boolean modeMove) {
        mModeMove = modeMove;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setOperation(int index) {
        if (index == 0) {
            mModeCopy = true;
            mModeMove = false;
        } else {
            mModeCopy = false;
            mModeMove = true;
        }
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        mPathMatcher = pathMatcher;
    }

    public void setRecursive(boolean recursive) {
        mRecursive = recursive;
    }

    public void setReplaceExisting(boolean replaceExisting) {
        mReplaceExisting = replaceExisting;
    }

    public void setSourceAndDest(String[] args) {
        if (args.length == 2) {
            String source = args[0];
            File sourceFile = new File(source);

            if (sourceFile.isDirectory()) {
                mSourceDir = sourceFile;
                mFilePattern = "*";
            } else {
                String sourceDir = FilenameUtils.getFullPathNoEndSeparator(source);
                mSourceDir = new File(sourceDir);
                mFilePattern = FilenameUtils.getName(source);
            }

            setDestDir(new File(args[1]));
        } else {
            addValidationError(mBundle.getString("invalid_arg_count"));
        }
    }

    public void setSourceDir(File source) {
        mSourceDir = source;
    }

    public String toDebugString() {
        isValid();
        String s = String.format(mBundle.getString("profile"),
                mCommand.toString(),
                mSourceDir,
                mFilePattern,
                mDestDir,
                mDatePattern,
                mDateSource
        );

        StringBuilder b = new StringBuilder(s);

        conditionalAppendDebugOption(b, mFollowLinks, Dict.FOLLOW_LINKS.toString());
        conditionalAppendDebugOption(b, mRecursive, Dict.RECURSIVE.toString());
        conditionalAppendDebugOption(b, mReplaceExisting, Dict.REPLACE.toString());
        conditionalAppendDebugOption(b, mBaseNameCase != NameCase.UNCHANGED, mBundleUI.getString("MainFrame.caseBaseLabel.text") + " " + mBaseNameCase);
        conditionalAppendDebugOption(b, mExtNameCase != NameCase.UNCHANGED, mBundleUI.getString("MainFrame.caseSuffixLabel.text") + " " + mExtNameCase);

        return b.toString();
    }

    @Override
    public String toString() {
        return mName;
    }

    private void addValidationError(String string) {
        mValidationErrorBuilder.append(string).append("\n");
    }

    private void conditionalAppendDebugOption(StringBuilder b, boolean append, String string) {
        String itemFormat = "\n • %s";

        if (append) {
            b.append(String.format(itemFormat, string));
        }
    }
}
