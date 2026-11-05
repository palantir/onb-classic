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

package com.palantir.onb.gui;

import com.palantir.onb.LogStandard;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This is a implementation of logStandard that just stores the data to be gotten by the GUI.
 * This function always uses level 4, then will pass through a lower level to a file.
 */
public class GuiLog implements LogStandard {
    private LogStandard childLogger;
    private final List<TimeStampedLog> dataStore = new ArrayList<>();
    private final List<TimeStampedLog> dataStoreErrors = new ArrayList<>();
    private static final Object locker = new Object();
    private int consoleLogLevel;
    private boolean setDates = true;

    /**
     * We always init the gui logger at level 4.
     */
        @SuppressWarnings("for-rollout:NullAway")
        GuiLog() {
        consoleLogLevel = 4;
    }

    /**
     * We can set a custom level if one day this is seen needed.
     * @param conLevel console log level
     */
        @SuppressWarnings("for-rollout:NullAway")
        public GuiLog(int conLevel) {
        consoleLogLevel = conLevel;
    }

    /**
     * This is a pass through logger, mostly a file/console logger for GUI.
     * @param childLogger The child logger to use
     */
    public void setChildLogger(LogStandard childLogger) {
        this.childLogger = childLogger;
    }

    /**
     * Allow setting of log level.
     * @param logLevel set the console log level
     */
    @Override
    public void setLogConsole(int logLevel) {
        consoleLogLevel = logLevel;
    }

    /**
     * Implementation forces us to have this.
     * @param _logLevel Unused log level
     */
    @Override
    public void setLogFile(int _logLevel) {}

    /**
     * Implementation forces us to have this.
     * @param _logLevelLoc log location
     */
    @Override
    public void setLogFileLocation(String _logLevelLoc) {}

    /**
     * GUI should never ask for this but we need to have it.
     * @return null!
     */
        @SuppressWarnings("for-rollout:NullAway")
        @Override
    public String getLogFileLocation() {
        return null;
    }

    /**
     * Always 4, but you can ask if you want.
     * @return 4
     */
    @Override
    public int getLogConsole() {
        return consoleLogLevel;
    }

    /**
     * Gui doesnt log to file, child logger will.
     * @return gui does not directly write log file, so this returns 0 at all times
     */
    @Override
    public int getLogFile() {
        return 0;
    }

    /**
     * Simple log, go to all levels.
     * @param allLevels Text for all log levels
     * @param reportingError Is this an error
     */
    @Override
    public void simpleReport(String allLevels, boolean reportingError) {
        if (childLogger != null) {
            childLogger.simpleReport(allLevels, reportingError);
        }
        String prefix = "";
        if (setDates) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss - ", Locale.ROOT);
            final LocalDateTime date = LocalDateTime.now(ZoneId.systemDefault());
            prefix = formatter.format(date);
        }
        synchronized (locker) {
            if (reportingError) {
                dataStoreErrors.add(new TimeStampedLog(prefix + allLevels, System.currentTimeMillis()));
            } else {
                dataStore.add(new TimeStampedLog(prefix + allLevels, System.currentTimeMillis()));
            }
        }
    }

    /**
     * Log differently at specific levels.
     * @param level0 Least verbose level
     * @param level1 More than 0
     * @param level2 More than 1
     * @param level3 More than 2
     * @param level4 Most verbose log level
     * @param reportingError Is this an error being reported
     */
    @Override
    public void report(
            String level0, String level1, String level2, String level3, String level4, boolean reportingError) {
        if (childLogger != null) {
            childLogger.report(level0, level1, level2, level3, level4, reportingError);
        }
        String prefix = "";
        if (setDates) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss - ", Locale.ROOT);
            final LocalDateTime date = LocalDateTime.now(ZoneId.systemDefault());
            prefix = formatter.format(date);
        }
        synchronized (locker) {
            switch (consoleLogLevel) {
                case 0 -> {
                    logStore(reportingError, prefix, level0);
                    return;
                }
                case 1 -> {
                    logStore(reportingError, prefix, level1);
                    return;
                }
                case 2 -> {
                    logStore(reportingError, prefix, level2);
                    return;
                }
                case 3 -> {
                    logStore(reportingError, prefix, level3);
                    return;
                }
                case 4 -> {
                    logStore(reportingError, prefix, level4);
                    return;
                }
            }
            logStore(reportingError, prefix, level4);
        }
    }

    private void logStore(boolean error, String prefix, String errorMessage) {
        if (error) {
            dataStoreErrors.add(new TimeStampedLog(prefix + errorMessage, System.currentTimeMillis()));
        } else {
            dataStore.add(new TimeStampedLog(prefix + errorMessage, System.currentTimeMillis()));
        }
    }

    /**
     * Dump all the logs to a list, then delete local cache.
     * @return All logs in a list
     */
    @Override
    public List<TimeStampedLog> getLogNDump() {
        synchronized (locker) {
            final List<TimeStampedLog> tempClone = new ArrayList<>(dataStore);
            dataStore.clear();
            return tempClone;
        }
    }

    /**
     * This logger caches to go between threads, this is to get all logs.
     * and dump cache. E does errors.
     * @return list of log strings
     */
    List<TimeStampedLog> getLogNDumpE() {
        synchronized (locker) {
            final List<TimeStampedLog> tempClone = new ArrayList<>(dataStoreErrors);
            dataStoreErrors.clear();
            return tempClone;
        }
    }

    /**
     * Function to print date/time on the front of the log.
     * @param doSetDate Set if we want date and times added
     */
    @Override
    public void setDates(boolean doSetDate) {
        setDates = doSetDate;
    }
}
