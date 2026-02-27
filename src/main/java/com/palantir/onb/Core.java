/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.onb;

import com.palantir.onb.gui.Launcher;
import com.palantir.onb.types.BootRules;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Main entry point for application.
 */
public final class Core {
    @SuppressWarnings("for-rollout:NullAway")
    private static Manager manager;

    public static final String ONB_VERSION = loadVersion();

    private Core() {}

    public static Manager getManager() {
        return manager;
    }

    /**
     * Load version from generated version.properties file.
     * Falls back to "unknown" if file cannot be read.
     */
    private static String loadVersion() {
        try (InputStream input = Core.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input == null) {
                return "unknown";
            }
            Properties props = new Properties();
            props.load(input);
            return props.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }

    /**
     * Main entry point to the program.
     *
     * @param args variables from the command line
     */
    public static void main(final String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        final Logging newLogger = new Logging();
        newLogger.simpleReport("Starting OpenNetBoot Classic Version " + ONB_VERSION + "...", false);

        // Start iterating over the arguments given
        manager = new Manager();
        Manager.setLogger(newLogger);
        byte startStatus = processArgs(newLogger, args);

        // Start loading rule information
        if (!manager.loadRules()) {
            newLogger.simpleReport("No rules file, creating one, and root folder...", false);
            exportBlankRules(newLogger, "rules.onr");
            File rootFolder = new File("tftpboot");
            if (!rootFolder.exists()) {
                rootFolder.mkdir();
            }
            if (!manager.loadRules()) {
                newLogger.simpleReport("Error loading newly created rules", true);
                return;
            }
        }
        manager.switchToLoadedRules();
        manager.combineRules();

        switch (startStatus) {
            case 1 -> {
                final VirtualNic vNic = new VirtualNic();
                vNic.setLogger(newLogger);
                vNic.run();
            }
            case 2 -> manager.startServices();
            case -1, 3 -> {
                // We now default to starting the gui
                Launcher launcher = new Launcher();
                launcher.open(args);
            }
        }
    }

    // CHECKSTYLE.OFF: CyclomaticComplexity
    /**
     * Process the parameters the user has entered.
     * @param newLogger Logger to log errors
     * @param args argument strings
     * @return Status to start in, -1 is nothing set, 0 is stop immediately, 1 is virtualnic, 2 is console, 3 is gui
     */
    private static byte processArgs(Logging newLogger, String[] args) {
        byte returnStatus = -1;
        List<String> argList = Arrays.asList(args);
        Iterator<String> argIterator = argList.iterator();
        while (argIterator.hasNext()) {
            String arg = argIterator.next();
            switch (arg) {
                case "-lsint" -> {
                    InterfaceData.printInterfaces();
                    return 0;
                }
                case "-i" -> {
                    if (argIterator.hasNext()) {
                        if ("any".equalsIgnoreCase(argIterator.next())) {
                            // this is so we know its set
                            manager.overrideInterfaces(new String[0]);
                        } else {
                            String temp = argIterator.next();
                            if (InterfaceData.verifyNics(temp)) {
                                manager.overrideInterfaces(temp.split(","));
                            } else {
                                newLogger.simpleReport("Error parsing interfaces", true);
                                return 0;
                            }
                        }
                    }
                    manager.overrideStartPxe(true);
                }
                case "-r" -> {
                    if (argIterator.hasNext()) {
                        manager.setRulesToLoad(argIterator.next());
                    }
                    manager.overrideStartPxe(true);
                }
                case "-blankrules" -> {
                    if (argIterator.hasNext()) {
                        exportBlankRules(newLogger, argIterator.next());
                    } else {
                        exportBlankRules(newLogger, "rules.onr");
                    }
                    System.exit(0);
                }
                case "-l" -> {
                    if (argIterator.hasNext()) {
                        try {
                            final int level = Integer.parseInt(argIterator.next());
                            manager.overrideLogLevelConsole(level);
                            manager.overrideLogLevelFile(level);
                        } catch (final NumberFormatException e) {
                            newLogger.simpleReport("Error reading overall log level", true);
                            return 0;
                        }
                    }
                }
                case "-lc" -> {
                    if (argIterator.hasNext()) {
                        try {
                            final int level = Integer.parseInt(argIterator.next());
                            manager.overrideLogLevelConsole(level);
                        } catch (final NumberFormatException e) {
                            newLogger.simpleReport("Error reading console log level", true);
                            return 0;
                        }
                    }
                }
                case "-lf" -> {
                    if (argIterator.hasNext()) {
                        try {
                            final int level = Integer.parseInt(argIterator.next());
                            manager.overrideLogLevelFile(level);
                        } catch (final NumberFormatException e) {
                            newLogger.simpleReport("Error reading file log level", true);
                            return 0;
                        }
                    }
                }
                case "-lfl" -> {
                    if (argIterator.hasNext()) {
                        manager.overrideLogLocation(argIterator.next());
                    }
                }
                case "-tftp" -> manager.overrideStartTftp(true);
                case "-disabletftp" -> manager.overrideStartTftp(false);
                case "-http" -> manager.overrideStartHttp(true);
                case "-disablehttp" -> manager.overrideStartHttp(false);
                case "-disablepxe" -> manager.overrideStartPxe(false);
                case "-pxe" -> manager.overrideStartPxe(true);
                case "-f" -> {
                    if (argIterator.hasNext()) {
                        manager.overrideTftpFolder(argIterator.next());
                    } else {
                        newLogger.simpleReport("-f set but no folder provided", true);
                        return 0;
                    }
                }
                case "-virtualnic" -> returnStatus = 1;
                case "-console" -> returnStatus = 2;
                case "-gui" -> returnStatus = 3;
                case "-test" -> returnStatus = 0;
                default -> {
                    newLogger.simpleReport("Option " + arg + " not recognized", true);
                    printHelp();
                    return 0;
                }
            }
        }
        return returnStatus;
    }
    // CHECKSTYLE:ON

    /**
     * Echo out the help information to the console.
     */
    private static void printHelp() {
        final String output = "OpenNetBoot - v" + Core.ONB_VERSION + "\n" + "\t\n"
                + "\tNo throws to start with gui.\n" + "\t\n" + "\t-h\t\t- displays help\n"
                + "\t-i [interfaces]\t- comma delimited list of interfaces\n"
                + "\t-lsint\t\t- display interfaces for filtering\n"
                + "\t-r [file name]\t- load a rule base from a specific file\n"
                + "\t-pxe\t- start pxe server\n" + "\t-tftp\t- start tftp server\n"
                + "\t-http\t- start http server\n" + "\t-disablepxe \t- stop pxe from loading\n"
                + "\t-disabletftp \t- stop tftp from loading\n"
                + "\t-disablehttp \t- stop http from loading\n"
                + "\t-blankrules (optional file name, ending in .onr)\t- save a file named "
                + "'Rules - 0.onr', if that exists it will increment the number\n"
                + "\t-virtualnic\n" + "\t-console\t- force start in console mode\n" + "\t\n"
                + "Logging\n"
                + " * 0 - Say when the system starts and stops, either on purpose or fatal errors\n"
                + " * 1 - Say when ports come up, no other info\n"
                + " * 2 - Echo out when a packet is received, and simple response\n"
                + " * 3 - More in depth info about packet and response\n"
                + " * 4 - Most verbose level of output\n"
                + "\t-l [level]\t- starts console and file loggers on specified level\n"
                + "\t-lc [level]\t- starts console logger on specified level\n"
                + "\t-lf [level]\t- starts file logger on specified level\n" + "\t\n" + "\t\n"
                + "Examples\n" + "\tonb.jar -i vmnet1,en0";
        // CHECKSTYLE.OFF: RegexpSinglelineJava
        System.out.println(output);
    }

    /**
     * Export a blank rules file as a example for someone.
     */
    private static void exportBlankRules(Logging logger, String saveLoc) {
        String newSaveLoc = "";

        if (saveLoc.isEmpty() && new File("rules.onr").exists()) {
            for (int i = 1; i < 10; i++) {
                final File rulesTest = new File("rules" + i + ".onr");
                if (!rulesTest.exists()) {
                    newSaveLoc = "rules" + i + ".onr";
                    break;
                }
            }
            if (newSaveLoc.isEmpty()) {
                logger.simpleReport("Cannot find available rule file name, please specify.", true);
                return;
            }
        } else {
            newSaveLoc = saveLoc;
        }

        final BootRules tempRules = BootRulesParser.stockBlankRules();
        final String rulesToSave;
        try {
            rulesToSave = tempRules.saveRules();
        } catch (IOException e) {
            logger.simpleReport(e.toString(), true);
            return;
        }

        if (tempRules.saveFile(rulesToSave, newSaveLoc)) {
            logger.simpleReport("Saved new rule file at " + newSaveLoc, false);
        } else {
            logger.simpleReport("Failed to save rule file at " + newSaveLoc, true);
        }
    }
}
