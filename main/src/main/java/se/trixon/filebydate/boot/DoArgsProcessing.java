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

    @Arg(longName = "list")
    @Description(
            shortDescription = "#DoArgsProcessing.list.desc"
    )
    @Messages("DoArgsProcessing.list.desc=list jobs")
    public boolean mListOption;
    @Arg(longName = "start")
    @Description(
            displayName = "#DoArgsProcessing.start.name",
            shortDescription = "#DoArgsProcessing.start.desc"
    )
    @Messages({
        "DoArgsProcessing.start.name=--start <task>",
        "DoArgsProcessing.start.desc=start task"
    })
    public String mStartOption;

    @Arg(longName = "version")
    @Description(
            shortDescription = "#DoArgsProcessing.version.desc"
    )
    @Messages("DoArgsProcessing.version.desc=print the version information and exit")
    public boolean mVersionOption;
    private final ResourceBundle mBundle = NbBundle.getBundle(DoArgsProcessing.class);

    {
        TaskManager.getInstance().setGui(false);
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
            startTask(mStartOption);
        }

        LifecycleManager.getDefault().exit();
    }

    private void displayVersion() {
        var pomInfo = new PomInfo(Task.class, "se.trixon.filebydate", "main");
        System.out.println(mBundle.getString("version_info").formatted(pomInfo.getVersion()));
    }

    private void listJobs() {
        for (var task : TaskManager.getInstance().getItems()) {
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

    private void startTask(String name) {
        var job = TaskManager.getInstance().getTask(name);
        if (job != null) {
            ExecutorManager.getInstance().start(job, true);
        } else {
            System.out.println("TASK NOT FOUND " + name);
        }
    }
}
