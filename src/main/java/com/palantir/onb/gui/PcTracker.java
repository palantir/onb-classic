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

/**
 * Class to follow a single PC going through the process.
 */
class PcTracker {
    private String ip;
    private String service;
    private String dateTime;
    private boolean updated;
    private long timestamp;

    /**
     * Init the class.
     * @param ip known IP of the system
     * @param service which service reported this info
     * @param dateTime when did the pc checkin
     * @param updated is this a brand-new log
     * @param timestamp a long timestamp, if logs don't come in proper order
     */
    PcTracker(String ip, String service, String dateTime, boolean updated, long timestamp) {
        this.ip = ip;
        this.service = service;
        this.dateTime = dateTime;
        this.updated = updated;
        this.timestamp = timestamp;
    }

    void update(String passedService, boolean passedUpdated, String passedDateTime, String passedIp) {
        service = passedService;
        updated = passedUpdated;
        dateTime = passedDateTime;
        ip = passedIp;
    }

    boolean isUpdated() {
        return updated;
    }

    void setUpdated(boolean updated) {
        this.updated = updated;
    }

    String getIp() {
        return ip;
    }

    void setIp(String ip) {
        this.ip = ip;
    }

    String getService() {
        return service;
    }

    void setService(String service) {
        this.service = service;
    }

    String getDateTime() {
        return dateTime;
    }

    void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    long getTimestamp() {
        return timestamp;
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
