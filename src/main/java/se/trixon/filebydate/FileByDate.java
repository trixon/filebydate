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

import java.awt.GraphicsEnvironment;
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
import org.apache.commons.lang3.SystemUtils;
import se.trixon.almond.util.AlmondUI;
import se.trixon.almond.util.BundleHelper;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.PomInfo;
import se.trixon.almond.util.Xlog;
import se.trixon.filebydate.ui.MainFrame;

/**
 *
 * @author Patrik Karlsson
 */
public class FileByDate implements OperationListener {

    private final AlmondUI mAlmondUI = AlmondUI.getInstance();
    private final ResourceBundle mBundle = BundleHelper.getBundle(FileByDate.class, "Bundle");
    private MainFrame mMainFrame = null;
    private Options mOptions;
    private final ProfileManager mProfileManager = ProfileManager.getInstance();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new FileByDate(args);
    }

    public FileByDate(String[] args) {
        initOptions();

        if (args.length == 0) {
            System.out.println(mBundle.getString("hint_tui"));
            displayGui();
        } else {
            try {
                CommandLineParser commandLineParser = new DefaultParser();
                CommandLine commandLine = commandLineParser.parse(mOptions, args);

                if (commandLine.hasOption("help")) {
                    displayHelp();
                    System.exit(0);
                } else if (commandLine.hasOption("version")) {
                    displayVersion();
                    System.exit(0);
                } else if (commandLine.hasOption("list-profiles")) {
                    displayProfiles();
                } else if (commandLine.hasOption("view-profile")) {
                    loadProfiles();
                    Profile profile = mProfileManager.getProfile(commandLine.getOptionValue("view-profile"));
                    if (profile == null) {
                        System.err.println(Dict.Dialog.ERROR_PROFILE_NOT_FOUND.toString());
                        System.exit(1);
                    } else {
                        profile.isValid();
                        System.out.println(profile.toDebugString());
                    }
                } else if (commandLine.hasOption("gui")) {
                    displayGui();
                } else {
                    Profile profile = null;

                    if (commandLine.hasOption("profile")) {
                        loadProfiles();
                        profile = mProfileManager.getProfile(commandLine.getOptionValue("profile"));
                        if (profile == null) {
                            System.err.println(Dict.Dialog.ERROR_PROFILE_NOT_FOUND.toString());
                            System.exit(1);
                        } else {
                            profile.setDryRun(commandLine.hasOption("dry-run"));
                        }
                    } else {
                        profile = new Profile(commandLine);
                    }

                    if (profile.isValid()) {
                        Operation operation = new Operation(this, profile);
                        operation.start();
                    } else {
                        System.out.println(profile.getValidationError());
                        System.out.println(Dict.ABORTING.toString());
                    }
                }
            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
                System.out.println(mBundle.getString("parse_help"));
            }
        }
    }

    @Override
    public void onOperationFailed(String message) {
    }

    @Override
    public void onOperationFinished(String message) {
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
    public void onOperationStarted() {
    }

    private void displayGui() {
        if (GraphicsEnvironment.isHeadless()) {
            Xlog.timedErr(Dict.Dialog.ERROR_NO_GUI_IN_HEADLESS.toString());
            System.exit(1);

            return;
        }

        mAlmondUI.installDarcula();
        mAlmondUI.initLookAndFeel();

        java.awt.EventQueue.invokeLater(() -> {
            mMainFrame = new MainFrame();
            mMainFrame.setVisible(true);
        });
    }

    private void displayHelp() {
        PrintStream defaultStdOut = System.out;
        StringBuilder sb = new StringBuilder()
                .append(mBundle.getString("usage")).append("\n\n");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);

        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.printHelp("xxx", mOptions, false);
        System.out.flush();
        System.setOut(defaultStdOut);
        sb.append(baos.toString().replace("usage: xxx" + SystemUtils.LINE_SEPARATOR, "")).append("\n")
                .append(mBundle.getString("help_footer"));

        System.out.println(sb.toString());
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
        System.out.println(String.format(mBundle.getString("version_info"), pomInfo.getVersion()));
    }

    private void initOptions() {
        Option help = Option.builder("h")
                .longOpt("help")
                .desc(mBundle.getString("opt_help_desc"))
                .build();

        Option version = Option.builder("v")
                .longOpt("version")
                .desc(mBundle.getString("opt_version_desc"))
                .build();

        Option copy = Option.builder("cp")
                .longOpt("copy")
                .desc(mBundle.getString("opt_copy_desc"))
                .build();

        Option move = Option.builder("mv")
                .longOpt("move")
                .desc(mBundle.getString("opt_move_desc"))
                .build();

        Option recursive = Option.builder("r")
                .longOpt("recursive")
                .desc(mBundle.getString("opt_recursive_desc"))
                .build();

        Option links = Option.builder("l")
                .longOpt("links")
                .desc(mBundle.getString("opt_links_desc"))
                .build();

        Option dryRun = Option.builder("n")
                .longOpt("dry-run")
                .desc(mBundle.getString("opt_dry_run_desc"))
                .build();

        Option overwrite = Option.builder("o")
                .longOpt("overwrite")
                .desc(mBundle.getString("opt_overwrite_desc"))
                .build();

        Option datePattern = Option.builder("dp")
                .longOpt("date-pattern")
                .desc(mBundle.getString("opt_date_pattern_desc"))
                .hasArg()
                .optionalArg(false)
                .build();

        Option dateSource = Option.builder("ds")
                .longOpt("date-source")
                .desc(mBundle.getString("opt_date_source_desc"))
                .hasArg()
                .optionalArg(false)
                .build();

        Option caseBase = Option.builder("cb")
                .longOpt("case-base")
                .desc(mBundle.getString("opt_case_base_desc"))
                .hasArg()
                .optionalArg(false)
                .build();

        Option caseExt = Option.builder("ce")
                .longOpt("case-ext")
                .desc(mBundle.getString("opt_case_ext_desc"))
                .hasArg()
                .optionalArg(false)
                .build();

        Option gui = Option.builder("g")
                .longOpt("gui")
                .desc(mBundle.getString("opt_gui_desc"))
                .build();

        Option profile = Option.builder("p")
                .longOpt("profile")
                .hasArg()
                .numberOfArgs(1)
                .desc(mBundle.getString("opt_profile_desc"))
                .build();

        Option listProfiles = Option.builder("lp")
                .longOpt("list-profiles")
                .desc(mBundle.getString("opt_list_profiles_desc"))
                .build();

        Option viewProfile = Option.builder("vp")
                .longOpt("view-profile")
                .hasArg()
                .numberOfArgs(1)
                .desc(mBundle.getString("opt_view_profile_desc"))
                .build();

        mOptions = new Options();

        mOptions.addOption(copy);
        mOptions.addOption(move);

        mOptions.addOption(dryRun);
        mOptions.addOption(links);
        mOptions.addOption(overwrite);
        mOptions.addOption(recursive);

        mOptions.addOption(datePattern);
        mOptions.addOption(dateSource);

        mOptions.addOption(caseBase);
        mOptions.addOption(caseExt);

        mOptions.addOption(listProfiles);
        mOptions.addOption(viewProfile);
        mOptions.addOption(profile);

        mOptions.addOption(gui);

        mOptions.addOption(help);
        mOptions.addOption(version);
    }

    private void loadProfiles() {
        try {
            mProfileManager.load();
        } catch (IOException ex) {
            Logger.getLogger(FileByDate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
