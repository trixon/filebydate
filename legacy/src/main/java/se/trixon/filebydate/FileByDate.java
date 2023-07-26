/* 
 * Copyright 2022 Patrik Karlström.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.PomInfo;
import se.trixon.almond.util.SystemHelper;
import se.trixon.filebydate.ui.FbdApp;

/**
 *
 * @author Patrik Karlström
 */
public class FileByDate implements OperationListener {

    private static String[] sArgs;
    private static final ResourceBundle sBundle = SystemHelper.getBundle(FileByDate.class, "Bundle");
    private static Options sOptions;
    private CommandLine mCommandLine;
    private final ProfileManager mProfileManager = ProfileManager.getInstance();

    public static String getHelp() {
        PrintStream defaultStdOut = System.out;
        StringBuilder sb = new StringBuilder()
                .append(sBundle.getString("usage")).append("\n\n");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);

        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.printHelp("xxx", sOptions, false);
        System.out.flush();
        System.setOut(defaultStdOut);
        sb.append(baos.toString().replace("usage: xxx" + System.lineSeparator(), "")).append("\n")
                .append(sBundle.getString("help_footer"));

        return sb.toString();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        sArgs = args;
        new FileByDate();
    }

    public FileByDate() {
        initOptions();

        if (sArgs.length == 0) {
            System.out.println(sBundle.getString("hint_tui"));
            displayGui();
        } else {
            if (mCommandLine.hasOption("help")) {
                displayHelp();
                System.exit(0);
            } else if (mCommandLine.hasOption("version")) {
                displayVersion();
                System.exit(0);
            } else if (mCommandLine.hasOption("list-profiles")) {
                displayProfiles();
            } else if (mCommandLine.hasOption("view-profile")) {
                loadProfiles();
                Profile profile = mProfileManager.getProfile(mCommandLine.getOptionValue("view-profile"));
                if (profile == null) {
                    System.err.println(Dict.Dialog.ERROR_PROFILE_NOT_FOUND.toString());
                    System.exit(1);
                } else {
                    profile.isValid();
                    System.out.println(profile.toDebugString());
                }
            } else if (mCommandLine.hasOption("gui")) {
                displayGui();
            } else {
                Profile profile = null;

                if (mCommandLine.hasOption("profile")) {
                    loadProfiles();
                    profile = mProfileManager.getProfile(mCommandLine.getOptionValue("profile"));
                    if (profile == null) {
                        System.err.println(Dict.Dialog.ERROR_PROFILE_NOT_FOUND.toString());
                        System.exit(1);
                    } else {
                        profile.setDryRun(mCommandLine.hasOption("dry-run"));
                    }
                } else {
                    profile = new Profile(mCommandLine);
                }

                if (profile.isValid()) {
                    Operation operation = new Operation(this, profile);
                    operation.start();
                } else {
                    System.out.println(profile.getValidationError());
                    System.out.println(Dict.ABORTING.toString());
                }
            }
        }
    }

    @Override
    public void onOperationError(String message) {
    }

    @Override
    public void onOperationFailed(String message) {
    }

    @Override
    public void onOperationFinished(String message, int fileCount) {
        System.out.println(Dict.DONE.toString());
    }

    @Override
    public void onOperationInterrupted() {
        System.out.println(Dict.OPERATION_INTERRUPTED.toString());
    }

    @Override
    public void onOperationLog(String message) {
        System.out.println(message);
    }

    @Override
    public void onOperationProcessingStarted() {
    }

    @Override
    public void onOperationProgress(int value, int max) {
    }

    @Override
    public void onOperationStarted() {
    }

    private void displayGui() {
        new Thread(() -> {
            FbdApp.main(sArgs);
        }).start();
    }

    private void displayHelp() {
        System.out.println(getHelp());
    }

    private void displayProfiles() {
        loadProfiles();
        if (mProfileManager.hasProfiles()) {
            for (Profile profile : mProfileManager.getProfiles()) {
                System.out.println(profile.getName());
            }
        } else {
            System.out.println(Dict.Dialog.MESSAGE_NO_PROFILES_FOUND.toString());
        }
    }

    private void displayVersion() {
        PomInfo pomInfo = new PomInfo(FileByDate.class, "se.trixon", "filebydate");
        System.out.println(String.format(sBundle.getString("version_info"), pomInfo.getVersion()));
    }

    private void initOptions() {
        Option help = Option.builder("h")
                .longOpt("help")
                .desc(sBundle.getString("opt_help_desc"))
                .build();

        Option version = Option.builder("v")
                .longOpt("version")
                .desc(sBundle.getString("opt_version_desc"))
                .build();

        Option copy = Option.builder("cp")
                .longOpt("copy")
                .desc(sBundle.getString("opt_copy_desc"))
                .build();

        Option move = Option.builder("mv")
                .longOpt("move")
                .desc(sBundle.getString("opt_move_desc"))
                .build();

        Option recursive = Option.builder("r")
                .longOpt("recursive")
                .desc(sBundle.getString("opt_recursive_desc"))
                .build();

        Option links = Option.builder("l")
                .longOpt("links")
                .desc(sBundle.getString("opt_links_desc"))
                .build();

        Option dryRun = Option.builder("n")
                .longOpt("dry-run")
                .desc(sBundle.getString("opt_dry_run_desc"))
                .build();

        Option overwrite = Option.builder("o")
                .longOpt("overwrite")
                .desc(sBundle.getString("opt_overwrite_desc"))
                .build();

        Option datePattern = Option.builder("dp")
                .longOpt("date-pattern")
                .desc(sBundle.getString("opt_date_pattern_desc"))
                .hasArg()
                .optionalArg(false)
                .build();

        Option dateSource = Option.builder("ds")
                .longOpt("date-source")
                .desc(sBundle.getString("opt_date_source_desc"))
                .hasArg()
                .optionalArg(false)
                .build();

        Option caseBase = Option.builder("cb")
                .longOpt("case-base")
                .desc(sBundle.getString("opt_case_base_desc"))
                .hasArg()
                .optionalArg(false)
                .build();

        Option caseExt = Option.builder("ce")
                .longOpt("case-ext")
                .desc(sBundle.getString("opt_case_ext_desc"))
                .hasArg()
                .optionalArg(false)
                .build();

        Option profile = Option.builder("rp")
                .longOpt("run-profile")
                .hasArg()
                .numberOfArgs(1)
                .desc(sBundle.getString("opt_profile_desc"))
                .build();

        Option listProfiles = Option.builder("lp")
                .longOpt("list-profiles")
                .desc(sBundle.getString("opt_list_profiles_desc"))
                .build();

        Option viewProfile = Option.builder("vp")
                .longOpt("view-profile")
                .hasArg()
                .numberOfArgs(1)
                .desc(sBundle.getString("opt_view_profile_desc"))
                .build();

        sOptions = new Options();

        sOptions.addOption(copy);
        sOptions.addOption(move);

        sOptions.addOption(dryRun);
        sOptions.addOption(links);
        sOptions.addOption(overwrite);
        sOptions.addOption(recursive);

        sOptions.addOption(datePattern);
        sOptions.addOption(dateSource);

        sOptions.addOption(caseBase);
        sOptions.addOption(caseExt);

        sOptions.addOption(listProfiles);
        sOptions.addOption(viewProfile);
        sOptions.addOption(profile);

        sOptions.addOption(help);
        sOptions.addOption(version);

        try {
            CommandLineParser commandLineParser = new DefaultParser();
            mCommandLine = commandLineParser.parse(sOptions, sArgs);
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            System.out.println(sBundle.getString("parse_help"));
            System.exit(0);
        }
    }

    private void loadProfiles() {
        try {
            mProfileManager.load();
        } catch (IOException ex) {
            Logger.getLogger(FileByDate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
