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
package se.trixon.filebydate;

/**
 *
 * @author Patrik Karlsson
 */
public class Profile implements Comparable<Profile>, Cloneable {

    private int mCaseBasename;
    private int mCaseSuffix;
    private String mDatePattern;
    private int mDateSource;
    private String mDest;
    private String mFilePattern;
    private boolean mFollowLinks;
    private String mName;
    private int mOperation;
    private boolean mRecursive;
    private String mSource;

    @Override
    public Profile clone() throws CloneNotSupportedException {
        return (Profile) super.clone();
    }

    @Override
    public int compareTo(Profile o) {
        return mName.compareTo(o.getName());
    }

    public int getCaseBasename() {
        return mCaseBasename;
    }

    public int getCaseSuffix() {
        return mCaseSuffix;
    }

    public String getDatePattern() {
        return mDatePattern;
    }

    public int getDateSource() {
        return mDateSource;
    }

    public String getDest() {
        return mDest;
    }

    public String getFilePattern() {
        return mFilePattern;
    }

    public String getName() {
        return mName;
    }

    public int getOperation() {
        return mOperation;
    }

    public String getSource() {
        return mSource;
    }

    public boolean isFollowLinks() {
        return mFollowLinks;
    }

    public boolean isRecursive() {
        return mRecursive;
    }

    public void setCaseBasename(int caseBasename) {
        mCaseBasename = caseBasename;
    }

    public void setCaseSuffix(int caseSuffix) {
        mCaseSuffix = caseSuffix;
    }

    public void setDatePattern(String datePattern) {
        mDatePattern = datePattern;
    }

    public void setDateSource(int dateSource) {
        mDateSource = dateSource;
    }

    public void setDest(String dest) {
        mDest = dest;
    }

    public void setFilePattern(String filePattern) {
        mFilePattern = filePattern;
    }

    public void setFollowLinks(boolean followLinks) {
        mFollowLinks = followLinks;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setOperation(int operation) {
        mOperation = operation;
    }

    public void setRecursive(boolean recursive) {
        mRecursive = recursive;
    }

    public void setSource(String source) {
        mSource = source;
    }

    @Override
    public String toString() {
        return mName;
    }
}
