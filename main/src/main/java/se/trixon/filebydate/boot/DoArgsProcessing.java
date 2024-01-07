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
package se.trixon.filebydate.boot;

import java.io.IOException;
import java.util.ResourceBundle;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Arg;
import org.netbeans.spi.sendopts.ArgsProcessor;
import org.netbeans.spi.sendopts.Description;
import org.netbeans.spi.sendopts.Env;
import org.openide.LifecycleManager;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.PomInfo;
import se.trixon.filebydate.core.ExecutorManager;
import se.trixon.filebydate.core.StorageManager;
import se.trixon.filebydate.core.Task;
import se.trixon.filebydate.core.TaskManager;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class DoArgsProcessing implements ArgsProcessor {

    @Arg(longName = "dry-run", shortName = 'n')
    @Description(shortDescription = "#opt_dry_run")
    @Messages({"opt_dry_run=perform a trial run with no changes made"})
    public boolean mDryRun;
    @Arg(longName = "copy", shortName = 'c')
    @Description(shortDescription = "#opt_copy")
    @Messages({"opt_copy=copy the files"})
    public boolean mCmdCopy;
    @Arg(longName = "move", shortName = 'm')
    @Description(shortDescription = "#opt_move")
    @Messages({"opt_move=move the files"})
    public boolean mCmdMove;
    @Arg(longName = "source", shortName = 's')
    @Description(displayName = "#opt_source_display", shortDescription = "#opt_source")
    @Messages({"opt_source_display=-s, --source <arg>", "opt_source=source directory"})
    public String mDirSource;
    @Arg(longName = "dest", shortName = 'd')
    @Description(displayName = "#opt_dest_display", shortDescription = "#opt_dest")
    @Messages({"opt_dest_display=-d, --dest <arg>", "opt_dest=destination directory"})
    public String mDirDest;
//
    @Arg(longName = "links", shortName = 'l')
    @Description(shortDescription = "#opt_links")
    @Messages({"opt_links=always follow links"})
    public boolean mLinks;
    @Arg(longName = "overwrite", shortName = 'o')
    @Description(shortDescription = "#opt_overwrite")
    @Messages({"opt_overwrite=replace existing files"})
    public boolean mOverwrite;
    @Arg(longName = "recursive", shortName = 'r')
    @Description(shortDescription = "#opt_recursive")
    @Messages({"opt_recursive=process directories recursively"})
    public boolean mRecursive;
    @Arg(longName = "date-pattern", shortName = 'p')
    @Description(displayName = "#opt_date_pattern_display", shortDescription = "#opt_date_pattern")
    @Messages({"opt_date_pattern_display=-p, --date-pattern <arg>", "opt_date_pattern=java date pattern (https://goo.gl/vbRe5T)"})
    public String mDatePattern;
    @Arg(longName = "date-source", shortName = 'a')
    @Description(displayName = "#opt_date_source_display", shortDescription = "#opt_date_source")
    @Messages({"opt_date_source_display=-a,--date-source <arg>", "opt_date_source=date source, one of:\n\t\t\t\t• exif_original\n\t\t\t\t• file_created\n\t\t\t\t• file_modified"})
    public String mDateSource;
    @Arg(longName = "case-base", shortName = 'b')
    @Description(displayName = "#opt_case_base_display", shortDescription = "#opt_case_base")
    @Messages({"opt_case_base_display=-b,--case-base <arg>", "opt_case_base=base name case, one of:\n\t\t\t\t• l, lower\n\t\t\t\t• u, upper"})
    public String mCaseBase;
    @Arg(longName = "case-ext", shortName = 'e')
    @Description(displayName = "#opt_case_ext_display", shortDescription = "#opt_case_ext")
    @Messages({"opt_case_ext_display=-e,--case-ext <arg>", "opt_case_ext=extension case, one of:\n\t\t\t\t• l, lower\n\t\t\t\t• u, upper"})
    public String mCaseExt;
//
    @Arg(longName = "list")
    @Description(shortDescription = "#opt_list")
    @Messages("opt_list=list the tasks")
    public boolean mListOption;
    @Arg(longName = "start")
    @Description(displayName = "#DoArgsProcessing.start.name", shortDescription = "#DoArgsProcessing.start.desc")
    @Messages({"DoArgsProcessing.start.name=--start <task>", "DoArgsProcessing.start.desc=start task"})
    public String mStartOption;
    @Arg(longName = "info")
    @Description(displayName = "#DoArgsProcessing.info.name", shortDescription = "#DoArgsProcessing.info.desc")
    @Messages({"DoArgsProcessing.info.name=--info <task>", "DoArgsProcessing.info.desc=display info about the task"})
    public String mInfo;
    @Arg(longName = "version")
    @Description(shortDescription = "#DoArgsProcessing.version.desc")
    @Messages("DoArgsProcessing.version.desc=print the version information and exit")
    public boolean mVersionOption;
    private final ResourceBundle mBundle = NbBundle.getBundle(DoArgsProcessing.class);
    private final TaskManager mTaskManager = TaskManager.getInstance();

    {
        mTaskManager.setGui(false);
    }

    public DoArgsProcessing() {
    }

    @Override
    public void process(Env env) throws CommandException {
        if (mVersionOption) {
            displayVersion();
        } else if (mListOption) {
            load();
            listJobs();
        } else if (mStartOption != null) {
            load();
            startTask(mTaskManager.getByName(mStartOption));
        } else if (mInfo != null) {
            load();
            infoTask(mTaskManager.getByName(mInfo));
        } else {
            buildTask();
        }

        LifecycleManager.getDefault().exit();
    }

    private void buildTask() {
        var task = new Task();
        task.setModeCopy(mCmdCopy);
        task.setModeMove(mCmdMove);
        task.setDatePattern(mDatePattern);
        task.setDateSourceString(mDateSource);
        task.setCaseBaseString(mCaseBase);
        task.setCaseExtString(mCaseExt);
        task.setFollowLinks(mLinks);
        task.setRecursive(mRecursive);
        task.setSourceAndDest(mDirSource, mDirDest);
        task.setReplaceExisting(mOverwrite);
//        System.out.println(task.toDebugString());

        if (task.isValid()) {
            System.out.println("call start with " + mDryRun);
        } else {
            System.out.println(task.getValidationError());
            System.out.println(Dict.ABORTING.toString());
        }
    }

    private void displayVersion() {
        var pomInfo = new PomInfo(Task.class, "se.trixon.filebydate", "main");
        System.out.println(mBundle.getString("version_info").formatted(pomInfo.getVersion()));
    }

    private void infoTask(Task task) {
        if (task != null) {
            System.out.println(task.toDebugString());
        }
    }

    private void listJobs() {
        for (var task : mTaskManager.getItems()) {
            System.out.println(task.getName());
        }
    }

    private void load() {
        try {
            StorageManager.getInstance().load();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void startTask(Task task) {
        if (task != null) {
            ExecutorManager.getInstance().start(task, true);
        }
    }
}
