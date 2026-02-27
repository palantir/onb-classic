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

import java.util.List;

/**
 * Generic interface for logging, this allows multiple use cases for all components.
 */
public interface LogStandard {
    void setLogConsole(int logLevel);

    void setLogFile(int logLevel);

    void setLogFileLocation(String logLevelLoc);

    void setDates(boolean doSetDate);

    String getLogFileLocation();

    int getLogConsole();

    int getLogFile();

    void simpleReport(String allLevels, boolean reportingError);

    void report(String level0, String level1, String level2, String level3, String level4, boolean reportingError);

    List<TimeStampedLog> getLogNDump();

    class TimeStampedLog {
        private final String log;
        private final long timestamp;

        public TimeStampedLog(String log, long timestamp) {
            this.log = log;
            this.timestamp = timestamp;
        }

        /**
         * Returns one line of log data as string.
         *
         * @return the line of log data
         */
        public String getLog() {
            return log;
        }

        /**
         * Return the time that log occurred.
         *
         * @return long of the time of the log
         */
        public long getTimestamp() {
            return timestamp;
        }
    }
}
