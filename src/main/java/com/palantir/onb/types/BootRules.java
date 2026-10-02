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

package com.palantir.onb.types;

import com.google.gson.stream.JsonWriter;
import com.palantir.onb.BootRulesParser;
import com.palantir.onb.tftp.TftpSettings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Rules for the services.
 */
public class BootRules {
        @SuppressWarnings("for-rollout:NullAway")
        private String[] lastInterfaces = null;

    private int broadcastSetting = -1;
    private List<BootRule> ruleSet;
    private TftpSettings tftpSettingSet;
    private int lastConsoleLogLvl = -1;
    private int lastFileLogLvl = -1;
    private String lastFileLogLoc = "";
    private byte enablePxe = 0;
    private byte enableTftp = 0;
    private byte enableHttp = 0;
    private int httpPort = 80;

    private boolean allowIsoExtracting = false;

    // Pending changes, add option to disable ipv6

    public BootRules() {
        ruleSet = new ArrayList<>();
        tftpSettingSet = new TftpSettings();
    }

    /**
     * Get the HTTP port in use.
     *
     * @return int of the http port in use
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Set the port the HTTP server should use.
     *
     * @param httpPort int of a port
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * Get a list of the interfaces last used, usually this is a single String that's comma separated.
     *
     * @return String array of interface names
     */
    public String[] getLastInterfaces() {
        return lastInterfaces;
    }

    /**
     * Set the list of interfaces used.
     *
     * @param lastInterfaces String list of interface names
     */
    public void setLastInterfaces(String[] lastInterfaces) {
        this.lastInterfaces = lastInterfaces;
    }

    /**
     * Override the interfaces with a comma separated list.
     *
     * @param commaSepInterfaces String of interfaces
     */
    public void addInterfaces(String commaSepInterfaces) {
        lastInterfaces = commaSepInterfaces.split(",");
    }

    /**
     * Get how the system should broadcast, one is to just the subnet, one is to the whole network. Different OS give
     * different results.
     *
     * @return broadcast setting
     */
    public int getBroadcastSetting() {
        return broadcastSetting;
    }

    /**
     * Set how PXE service should broadcast.
     *
     * broadcastmode - With broadcast mode 0, selected adapters will get a DHCP packet for a received request sent
     * out of them. Because of how Java networking works, we send it to ALL selected interfaces, but since there is a
     * transaction ID, most networks just ignore it. When running broadcast mode 1, which is needed for most UEFI roms,
     * the packets will only go out the systems default gateway. If you have a system that is dual homed, or
     * hooked up to a network without a default gateway, then this may not work.
     *
     * @param broadcastSetting broadcast setting
     */
    public void setBroadcastSetting(int broadcastSetting) {
        this.broadcastSetting = broadcastSetting;
    }

    /**
     * Should PXE be enabled on system start. This is a byte because user can override the setting in conf.
     *
     * @return 0 is not set, 1 is don't start, 2 is start
     */
    public byte getEnablePxe() {
        return enablePxe;
    }

    /**
     * Set startup setting for PXE.
     *
     * @param enablePxe int of PXE setting
     */
    public void setEnablePxe(byte enablePxe) {
        this.enablePxe = enablePxe;
    }

    /**
     * Get boot setting for TFTP. This is a byte because user can override the setting in conf.
     *
     * @return 0 is not set, 1 is don't start, 2 is start
     */
    public byte getEnableTftp() {
        return enableTftp;
    }

    /**
     * Set enableTftpSetting.
     *
     * @param enableTftp setting for TFTP to start
     */
    public void setEnableTftp(byte enableTftp) {
        this.enableTftp = enableTftp;
    }

    /**
     * Get boot setting for HTTP. This is a byte because user can override the setting in conf.
     *
     * @return 0 is not set, 1 is don't start, 2 is start
     */
    public byte getEnableHttp() {
        return enableHttp;
    }

    /**
     * Set starting HTTP setting.
     *
     * @param enableHttp byte of http setting
     */
    public void setEnableHttp(byte enableHttp) {
        this.enableHttp = enableHttp;
    }

    /**
     * Get the value of console logging.
     *
     * @return int of the log level
     */
    public int getLastConsoleLogLvl() {
        return lastConsoleLogLvl;
    }

    /**
     * Set the logging level of the console.
     *
     * @param lastConsoleLogLvl int of console log level
     */
    public void setLastConsoleLogLvl(int lastConsoleLogLvl) {
        this.lastConsoleLogLvl = lastConsoleLogLvl;
    }

    /**
     * File logging level.
     *
     * @return int of file logging level
     */
    public int getLastFileLogLvl() {
        return lastFileLogLvl;
    }

    /**
     * Set the file logging level.
     *
     * @param lastFileLogLvl Int of logging level
     */
    public void setLastFileLogLvl(int lastFileLogLvl) {
        this.lastFileLogLvl = lastFileLogLvl;
    }

    /**
     * Place to put the file log.
     *
     * @return get string of the log file loc
     */
    public String getLastFileLogLoc() {
        return lastFileLogLoc;
    }

    /**
     * Set the location of the log file.
     *
     * @param lastFileLogLoc String of the file location relative to startup of the app
     */
    public void setLastFileLogLoc(String lastFileLogLoc) {
        this.lastFileLogLoc = lastFileLogLoc;
    }

    /**
     * Get a List of the bootrules.
     *
     * @return List of the boot rules
     */
    public List<BootRule> getRuleSet() {
        return ruleSet;
    }

    /**
     * Replace the list of bootrules with a new list.
     *
     * @param ruleSet list of boot rules
     */
    public void setRuleSet(List<BootRule> ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * TftpSettings to use when starting the TFTP server.
     *
     * @return get the tftp boot rules
     */
    public TftpSettings getTftpSettingSet() {
        return tftpSettingSet;
    }

    /**
     * Set new rules to use when booting Tftp Server.
     *
     * @param tftpSettingSet new tftp server settings
     */
    public void setTftpSettingSet(TftpSettings tftpSettingSet) {
        this.tftpSettingSet = tftpSettingSet;
    }

    /**
     * Should we allow the HTTP server to go into ISOs/Zips and extract files?.
     *
     * @return boolean yes we should allow, or not
     */
    public boolean isAllowIsoExtracting() {
        return allowIsoExtracting;
    }

    /**
     * Set if ISO/Zip processing is allowed.
     *
     * @param allowIsoExtracting setting
     */
    public void setAllowIsoExtracting(boolean allowIsoExtracting) {
        this.allowIsoExtracting = allowIsoExtracting;
    }

    /**
     * Save the rules to a string.
     * @return String of json for rules
     */
    public String saveRules() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonWriter jsonReturnData;
        jsonReturnData = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        jsonReturnData.beginObject();
        jsonReturnData.setIndent("\t");

        jsonReturnData.name("compVersion").value(BootRulesParser.getLastSupported());
        jsonReturnData.name("enablepxe").value(enablePxe == 2);
        jsonReturnData.name("enabletftp").value(enableTftp == 2);
        jsonReturnData.name("enablehttp").value(enableHttp == 2);
        jsonReturnData.name("lastconloglevel").value(lastConsoleLogLvl);
        jsonReturnData.name("lastfileloglevel").value(lastFileLogLvl);
        jsonReturnData.name("lastfilelogloc").value(lastFileLogLoc);
        StringBuilder tempBuffer = new StringBuilder();
        for (String singleInt : lastInterfaces) {
            if (singleInt.isEmpty()) {
                continue;
            }
            tempBuffer.append(singleInt);
            tempBuffer.append(",");
        }
        jsonReturnData.name("lastints").value(tempBuffer.toString());
        jsonReturnData.name("httpport").value(getHttpPort());
        jsonReturnData.name("broadcastmode").value(broadcastSetting);
        jsonReturnData.name("allowisoextracting").value(isAllowIsoExtracting());
        jsonReturnData.name("tftp").beginObject();
        jsonReturnData.name("rootfolder").value(tftpSettingSet.getRootFs().getPath());

        jsonReturnData.endObject();

        jsonReturnData.name("pxerules").beginArray();

        for (BootRule aRuleSet : ruleSet) {
            jsonReturnData.beginObject();
            if (!aRuleSet.getHardwareAddress().isEmpty()) {
                jsonReturnData.name("hardwareid").value(aRuleSet.getHardwareAddress());
            }

            if (!aRuleSet.getClient().isEmpty()) {
                jsonReturnData.name("clientid").value(aRuleSet.getClient());
            }

            if (!aRuleSet.getServerIp().isEmpty()) {
                jsonReturnData.name("serverip").value(aRuleSet.getServerIp());
            }

            if (!aRuleSet.getTftpIp().isEmpty()) {
                jsonReturnData.name("tftpip").value(aRuleSet.getTftpIp());
            }

            if (aRuleSet.getHardwareTypes().length > 0) {
                StringBuilder tempBuilder = new StringBuilder();
                for (int k = 0; k < aRuleSet.getHardwareTypes().length; k++) {
                    tempBuilder.append(aRuleSet.getHardwareTypes()[k]);
                    tempBuilder.append(",");
                }
                jsonReturnData.name("arch").value(tempBuilder.toString());
            }

            // This is by design because each rule should have a file it points to
            jsonReturnData.name("bootfile").value(aRuleSet.getBootFile());

            jsonReturnData.endObject();
        }

        jsonReturnData.endArray();

        jsonReturnData.endObject();
        jsonReturnData.flush();
        jsonReturnData.close();

        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Save the current rules out to the file location specified.
     * @param settingsData Settings data as a string
     * @param saveFileLoc Location to save to
     * @return Did the save work
     */
    public boolean saveFile(String settingsData, String saveFileLoc) {
        try (PrintWriter outing =
                new PrintWriter(Files.newBufferedWriter(Paths.get(saveFileLoc), StandardCharsets.UTF_8))) {
            outing.println(settingsData);
            return true;
        } catch (final IOException e) {
            return false;
        }
    }
}
