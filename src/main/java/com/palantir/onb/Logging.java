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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Log Levels, this is a dirty way to log and not proper java, that can be fixed later 0 - Say when
 * the system starts and stops, either on purpose or fatal errors 1 - Say when ports come up, no
 * other info 2 - Echo out when a packet is received, and simple response 3 - More in depth info
 * about packet and response 4 - Most verbose level of output 5 - Pcap File, currently disabled.
 */
public class Logging implements LogStandard {
    // CHECKSTYLE.OFF: RegexpSinglelineJava
    private int consoleLogLevel;
    private int fileLogLevel;
    private String fileLogSaveLoc = "log.txt";
    private boolean setDates = true;

    /**
     * Setup logger.
     * @param conLevel console log level
     * @param fileLevel file log level
     */
    public Logging(int conLevel, int fileLevel) {
        consoleLogLevel = conLevel;
        fileLogLevel = fileLevel;
    }

    /**
     * Default logging, very little console, no file.
     */
    public Logging() {
        consoleLogLevel = 0;
        fileLogLevel = 0;
    }

    /**
     * Set the console log level.
     * @param logLevel int of log level, 0 is lowest
     */
    @Override
    public void setLogConsole(int logLevel) {
        consoleLogLevel = logLevel;
    }

    /**
     * Set file log level.
     * @param logLevel log level, 0 is off, 4 is most
     */
    @Override
    public void setLogFile(int logLevel) {
        fileLogLevel = logLevel;
    }

    /**
     * Set the location of the file log.
     * @param logLevelLoc string of location
     */
    @Override
    public void setLogFileLocation(String logLevelLoc) {
        fileLogSaveLoc = logLevelLoc;
        fileLogSaveLoc = fileLogSaveLoc.replace('\\', File.separatorChar);
        final File folderTest = new File(fileLogSaveLoc);
        if (!folderTest.exists()) {
            final File temp = folderTest.getParentFile();
            if (temp != null && !temp.mkdirs()) {
                simpleReport("Could not create folder to store logs", true);
            }
        }
    }

    /**
     * Get the file log location.
     * @return string of location
     */
    @Override
    public String getLogFileLocation() {
        return fileLogSaveLoc;
    }

    /**
     * Get console log level.
     * @return int of console log level
     */
    @Override
    public int getLogConsole() {
        return consoleLogLevel;
    }

    /**
     * Get file log level.
     * @return int of file log level
     */
    @Override
    public int getLogFile() {
        return fileLogLevel;
    }

    /**
     * Report to all levels.
     * @param allLevels Text of log
     * @param reportingError is this an error
     */
    @Override
    @SuppressWarnings("JavaTimeDefaultTimeZone")
    public void simpleReport(String allLevels, boolean reportingError) {
        String prefix = "";
        if (setDates) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss - ", Locale.ROOT);
            final LocalDateTime date = LocalDateTime.now(ZoneId.systemDefault());
            prefix = formatter.format(date);
        }

        if (!allLevels.isEmpty()) {
            if (reportingError) {
                System.err.println(prefix + allLevels);
            } else {
                System.out.println(prefix + allLevels);
            }
        }

        boolean successWrite = false;
        if (fileLogLevel > 0) {
            if (!allLevels.isEmpty()) {
                successWrite = writeLog(prefix + allLevels);
            }
        } else {
            successWrite = true;
        }

        if (!successWrite) {
            System.err.println(prefix + "Error writing log file");
        }
    }

    // CHECKSTYLE.OFF: CyclomaticComplexity
    /**
     * Report for each level.
     * @param level0 console lowest log, file disabled
     * @param level1 more verbose
     * @param level2 more verbose
     * @param level3 more verbose
     * @param level4 Most verbose
     * @param reportingError Is the report an error?
     */
    @Override
    @SuppressWarnings("JavaTimeDefaultTimeZone")
    public void report(
            String level0, String level1, String level2, String level3, String level4, boolean reportingError) {
        String prefix = "";
        if (setDates) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss - ", Locale.ROOT);
            final LocalDateTime date = LocalDateTime.now(ZoneId.systemDefault());
            prefix = formatter.format(date);
        }

        switch (consoleLogLevel) {
            case 0 -> {
                if (!level0.isEmpty()) {
                    if (reportingError) {
                        System.err.println(prefix + level0);
                    } else {
                        System.out.println(prefix + level0);
                    }
                }
            }
            case 1 -> {
                if (!level1.isEmpty()) {
                    if (reportingError) {
                        System.err.println(prefix + level1);
                    } else {
                        System.out.println(prefix + level1);
                    }
                }
            }
            case 2 -> {
                if (!level2.isEmpty()) {
                    if (reportingError) {
                        System.err.println(prefix + level2);
                    } else {
                        System.out.println(prefix + level2);
                    }
                }
            }
            case 3 -> {
                if (!level3.isEmpty()) {
                    if (reportingError) {
                        System.err.println(prefix + level3);
                    } else {
                        System.out.println(prefix + level3);
                    }
                }
            }
            case 4 -> {
                if (!level4.isEmpty()) {
                    if (reportingError) {
                        System.err.println(prefix + level4);
                    } else {
                        System.out.println(prefix + level4);
                    }
                }
            }
            case 5 -> {
                // 5 is just 4 with pcap files
                if (!level4.isEmpty()) {
                    if (reportingError) {
                        System.err.println(prefix + level4);
                    } else {
                        System.out.println(prefix + level4);
                    }
                }
            }
        }
        boolean successWrite = false;
        if (fileLogLevel > 0) {
            if (fileLogLevel > 5) {
                successWrite = writeLog(prefix + level4);
            } else {
                switch (fileLogLevel) {
                    case 1 -> {
                        if (!level1.isEmpty()) {
                            successWrite = writeLog(prefix + level1);
                        }
                    }
                    case 2 -> {
                        if (!level2.isEmpty()) {
                            successWrite = writeLog(prefix + level2);
                        }
                    }
                    case 3 -> {
                        if (!level3.isEmpty()) {
                            successWrite = writeLog(prefix + level3);
                        }
                    }
                    case 4, 5 -> {
                        if (!level4.isEmpty()) {
                            successWrite = writeLog(prefix + level4);
                        }
                    }
                }
            }
        } else {
            successWrite = true;
        }

        if (!successWrite) {
            System.err.println("Error writing log file");
        }
    }
    // CHECKSTYLE.ON

    /**
     * Write this log line to the log file.
     * @param passedText text to write
     * @return did it write correctly
     */
    @SuppressWarnings("JavaTimeDefaultTimeZone")
    private boolean writeLog(String passedText) {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
                Paths.get(fileLogSaveLoc),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND))) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss - ", Locale.ROOT);
            final LocalDateTime date = LocalDateTime.now(ZoneId.systemDefault());
            out.println(formatter.format(date) + "\t" + passedText);
            out.flush();
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    /**
     * If the logger is using a buffer, dump the buffer.
     * @return buffer of logs
     */
    @SuppressWarnings("for-rollout:NullAway")
    @Override
    public List<TimeStampedLog> getLogNDump() {
        return null;
    }

    /**
     * Should we be settings dates.
     * @param doSetDate boolean of if we should be setting dates.
     */
    @Override
    public void setDates(boolean doSetDate) {
        setDates = doSetDate;
    }
}
