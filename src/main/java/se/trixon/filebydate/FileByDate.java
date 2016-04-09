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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ResourceBundle;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import se.trixon.util.BundleHelper;
import se.trixon.util.SystemHelper;
import se.trixon.util.dictionary.Dict;

/**
 *
 * @author Patrik Karlsson
 */
public class FileByDate implements OperationListener {

    private final ResourceBundle mBundle = BundleHelper.getBundle(FileByDate.class, "Bundle");
    private Options mOptions;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new FileByDate(args);
    }

    public FileByDate(String[] args) {
        initOptions();

        if (args.length == 0) {
            displayHelp();
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
                } else {
                    OptionsHolder optionsHolder = new OptionsHolder(commandLine);

                    if (optionsHolder.isValid()) {
                        //System.out.println(optionsHolder.toString());
                        Operation operation = new Operation(this, optionsHolder);
                        operation.start();
                    } else {
                        System.out.println("*** invalid args");
                        System.out.println(optionsHolder.getValidationError());
                        System.out.println(Dict.ABORTING.toString());
                    }
                }
            } catch (ParseException ex) {
                System.out.println(ex.getLocalizedMessage());
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
        System.out.println("OPERATION INTERRUPTED");
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

    private void displayVersion() {
        System.out.println(String.format(mBundle.getString("version_info"), SystemHelper.getJarVersion(FileByDate.class)));
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

        mOptions = new Options();

        mOptions.addOption(copy);
        mOptions.addOption(move);

        mOptions.addOption(dryRun);
        mOptions.addOption(recursive);
        mOptions.addOption(links);

        mOptions.addOption(datePattern);
        mOptions.addOption(dateSource);

        mOptions.addOption(help);
        mOptions.addOption(version);
    }
}
