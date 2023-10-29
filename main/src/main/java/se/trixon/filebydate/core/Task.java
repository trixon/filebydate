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

import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.openide.util.NbBundle;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.fx.control.editable_list.EditableListItem;
import se.trixon.filebydate.Options;
import se.trixon.filebydate.core.Operation.Command;
import se.trixon.filebydate.ui.MainTopComponent;

/**
 *
 * @author Patrik Karlström
 */
public class Task implements Comparable<Task>, Cloneable, EditableListItem {

    private transient final ResourceBundle mBundle = NbBundle.getBundle(Task.class);
    private transient final ResourceBundle mBundleUI = NbBundle.getBundle(MainTopComponent.class);
    @SerializedName("case_base")
    private NameCase mCaseBase = NameCase.UNCHANGED;
    private transient String mCaseBaseString;
    @SerializedName("case_ext")
    private NameCase mCaseExt = NameCase.UNCHANGED;
    private transient String mCaseExtString;
    @SerializedName("operation")
    private Command mCommand;
    private transient SimpleDateFormat mDateFormat;
    @SerializedName("date_pattern")
    private String mDatePattern;
    @SerializedName("date_source")
    private DateSource mDateSource = DateSource.FILE_CREATED;
    private transient String mDateSourceString;
    @SerializedName("description")
    private String mDescription;
    @SerializedName("destination")
    private File mDestDir;
    @SerializedName("dry_run")
    private boolean mDryRun;
    @SerializedName("file_pattern")
    private String mFilePattern;
    @SerializedName("follow_links")
    private boolean mFollowLinks;
    @SerializedName("last_run")
    private long mLastRun;
    private transient boolean mModeCopy;
    private transient boolean mModeMove;
    @SerializedName("name")
    private String mName;
    private transient PathMatcher mPathMatcher;
    @SerializedName("recursive")
    private boolean mRecursive;
    @SerializedName("overwrite")
    private boolean mReplaceExisting;
    @SerializedName("source")
    private File mSourceDir;
    private transient StringBuilder mValidationErrorBuilder = new StringBuilder();
    @SerializedName("uuid")
    private String mId = UUID.randomUUID().toString();

    public Task() {
    }

    public Task(CommandLine commandLine) {
        mModeCopy = commandLine.hasOption("copy");
        mModeMove = commandLine.hasOption("move");

        mDatePattern = commandLine.getOptionValue("dp");
        mDateSourceString = commandLine.getOptionValue("ds");
        mCaseBaseString = commandLine.getOptionValue("case-base");
        mCaseExtString = commandLine.getOptionValue("case-ext");

        mDryRun = commandLine.hasOption("dry-run");
        mFollowLinks = commandLine.hasOption("links");
        mRecursive = commandLine.hasOption("recursive");
        mReplaceExisting = commandLine.hasOption("overwrite");

        setSourceAndDest(commandLine.getArgs());
    }

    @Override
    public Task clone() {
        try {
            return (Task) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public int compareTo(Task o) {
        return mName.compareTo(o.getName());
    }

    public NameCase getCaseBase() {
        return mCaseBase;
    }

    public NameCase getCaseExt() {
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

    public String getDescription() {
        return StringUtils.defaultString(mDescription);
    }

    public File getDestDir() {
        return mDestDir;
    }

    public String getDestDirAsString() {
        return mDestDir == null ? "" : mDestDir.getPath();
    }

    public String getFilePattern() {
        return mFilePattern;
    }

    public String getId() {
        return mId;
    }

    public long getLastRun() {
        return mLastRun;
    }

    @Override
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
        return mSourceDir == null ? "" : mSourceDir.getPath();
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
            updateCommand();
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

        if (mCaseBaseString != null) {
            mCaseBase = NameCase.getCase(mCaseBaseString);
            if (mCaseBase == null) {
                addValidationError(String.format(mBundle.getString("invalid_case_base"), mCaseBaseString));
            }

        }

        if (mCaseExtString != null) {
            mCaseExt = NameCase.getCase(mCaseExtString);
            if (mCaseExt == null) {
                addValidationError(String.format(mBundle.getString("invalid_case_ext"), mCaseExtString));
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

    public void setCaseBase(NameCase caseBase) {
        mCaseBase = caseBase;
    }

    public void setCaseExt(NameCase caseExt) {
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

    public void setDescription(String description) {
        mDescription = description;
    }

    public void setDestDir(File dest) {
        mDestDir = dest;
    }

    public void setDryRun(boolean dryRun) {
        mDryRun = dryRun;
    }

    public void setFilePattern(String filePattern) {
        mFilePattern = filePattern;
    }

    public void setFollowLinks(boolean links) {
        mFollowLinks = links;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public void setLastRun(long lastRun) {
        mLastRun = lastRun;
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

        updateCommand();
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
        conditionalAppendDebugOption(b, mCaseBase != NameCase.UNCHANGED, Dict.BASENAME.toString() + " " + mCaseBase);
        conditionalAppendDebugOption(b, mCaseExt != NameCase.UNCHANGED, Dict.EXTENSION.toString() + " " + mCaseExt);

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

    private void updateCommand() {
        mCommand = mModeCopy ? Command.COPY : Command.MOVE;
    }
}
