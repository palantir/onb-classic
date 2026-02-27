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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.palantir.onb.tftp.TftpSettings;
import com.palantir.onb.types.BootRule;
import com.palantir.onb.types.BootRules;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * This class has all the functions to parse rules in from a rule file.
 */
public class BootRulesParser {
    private final LogStandard logger;
    private final String onbVersion;
    private static final String LAST_SUPPORTED = "2.0.0";
    private JsonReader handleJson;
    private BootRules newRuleSet;

    /**
     * Load the logger, and which version we are running of ONB.
     * @param newLogger passed in logger
     */
    @SuppressWarnings("for-rollout:NullAway")
    public BootRulesParser(LogStandard newLogger) {
        this.logger = newLogger;
        onbVersion = Core.ONB_VERSION;
    }

    /**
     * Get the latest supported version for this config.
     * @return string of version
     */
    public static String getLastSupported() {
        return LAST_SUPPORTED;
    }

    /**
     * This is just a bunch of simple checks to see if this rule file is valid.
     *
     * @param filename Path to rule file
     * @return if the rule file is valid
     */
    private boolean validRuleFile(String filename) {
        final File inputRuleFile = new File(filename);
        return inputRuleFile.exists() && inputRuleFile.canRead();
    }

    /**
     * Build a generic rules file to save.
     *
     * @return bootRules in a object
     */
    public static BootRules stockBlankRules() {
        final BootRules tempRules = new BootRules();

        tempRules.setEnableHttp((byte) 2);
        tempRules.setEnablePxe((byte) 2);
        tempRules.setEnableTftp((byte) 2);

        tempRules.setLastConsoleLogLvl(4);
        tempRules.setLastFileLogLvl(4);
        tempRules.setLastFileLogLoc("log.txt");

        tempRules.setHttpPort(80);

        tempRules.setLastInterfaces(new String[0]);

        tempRules.setTftpSettingSet(new TftpSettings("./tftpboot/"));

        tempRules.setBroadcastSetting(2);

        tempRules.setAllowIsoExtracting(false);

        final BootRule ruleOne = new BootRule();
        ruleOne.setClient("iPXE");
        ruleOne.setBootFile("menu.ipxe");
        tempRules.getRuleSet().add(ruleOne);

        final BootRule ruleTwo = new BootRule();
        ruleTwo.setBootFile("ipxe.pxe");
        tempRules.getRuleSet().add(ruleTwo);

        return tempRules;
    }

    /**
     * Load rules from a file into a string.
     * @param filename the file to load
     * @return string of that file
     */
    final String loadRulesFile(String filename) {
        logger.simpleReport("Loading Rules from: " + filename, false);
        if (!validRuleFile(filename)) {
            return "";
        }
        final File inputRuleFile = new File(filename);
        String totalData;
        try {
            totalData = Files.readString(inputRuleFile.toPath());
        } catch (final IOException e) {
            logger.simpleReport("Can not read rules file", true);
            return "";
        }

        return totalData;
    }

    /**
     * Get a string of rule data and convert to BootRules.
     *
     * @param fileData string the rule data
     * @return a bootrules object
     */
    @SuppressWarnings("for-rollout:NullAway")
    public BootRules loadRules(String fileData) {
        newRuleSet = new BootRules();
        handleJson = new JsonReader(new StringReader(fileData.replace("\\", "\\\\")));
        try {
            while (handleJson.hasNext()) {
                JsonToken tempToken = handleJson.peek();
                if (tempToken.equals(JsonToken.BEGIN_OBJECT)) {
                    handleJson.beginObject();
                    continue;
                }
                if (tempToken.equals(JsonToken.NAME)) {
                    if (!nameProcessor()) {
                        return null;
                    }
                }
            }
        } catch (final Throwable e) {
            logger.simpleReport(e.toString(), true);
        }

        logger.simpleReport(newRuleSet.getRuleSet().size() + " rule[s] successfully loaded", false);
        return newRuleSet;
    }

    private boolean nameProcessor() throws IOException {
        String nextName = handleJson.nextName();

        if ("compversion".equalsIgnoreCase(nextName)) {
            if (!checkVersionNumber(handleJson.nextString())) {
                logger.simpleReport(
                        "Failed to load rules file, rules seem to be " + "from a unsupported version.", true);
                return false;
            } else {
                return true;
            }
        }
        return processJsonData(nextName);
    }

    private boolean processJsonData(String nextName) throws IOException {
        if (nextName.startsWith("enable")) {
            return parseEnables(nextName);
        } else if (nextName.endsWith("loglevel")) {
            return parseLogLevels(nextName);
        }
        switch (nextName.toLowerCase(Locale.ROOT)) {
            case "httpport" -> {
                newRuleSet.setHttpPort(handleJson.nextInt());
                return true;
            }
            case "broadcastmode" -> {
                newRuleSet.setBroadcastSetting(handleJson.nextInt());
                return true;
            }
            case "allowisoextracting" -> {
                newRuleSet.setAllowIsoExtracting(handleJson.nextBoolean());
                return true;
            }
            case "lastfilelogloc" -> {
                newRuleSet.setLastFileLogLoc(fileLocParser(handleJson.nextString()));
                return true;
            }
            case "lastints" -> {
                newRuleSet.setLastInterfaces(
                        Arrays.stream(handleJson.nextString().split(",", -1))
                                .filter(m -> !m.isEmpty())
                                .toArray(String[]::new));
                return true;
            }
            case "tftp" -> {
                return parseTftp();
            }
            case "pxerules" -> {
                return parsePxe();
            }
        }
        logger.simpleReport("Error reading option in rules file", true);
        return false;
    }

    /**
     * This appears to cut down the number of backslashes if you are on windows and put multiple into a filepath.
     * @param tempLogFileLoc original file path
     * @return cleaned one
     */
    private String fileLocParser(String tempLogFileLoc) {
        String newTempFileLoc = tempLogFileLoc;
        while (newTempFileLoc.contains("\\\\")) {
            final int pos = newTempFileLoc.indexOf('\\');
            if (pos + 1 < newTempFileLoc.length()) {
                if (newTempFileLoc.charAt(pos + 1) == '\\') {
                    newTempFileLoc = newTempFileLoc.substring(0, pos) + newTempFileLoc.substring(pos + 1);
                }
            }
        }
        return newTempFileLoc;
    }

    private boolean parseLogLevels(String nextName) throws IOException {
        if ("lastconloglevel".equals(nextName)) {
            newRuleSet.setLastConsoleLogLvl(handleJson.nextInt());
        } else if ("lastfileloglevel".equals(nextName)) {
            newRuleSet.setLastFileLogLvl(handleJson.nextInt());
        } else {
            return false;
        }
        return true;
    }

    private boolean parseEnables(String nextName) throws IOException {
        switch (nextName) {
            case "enabletftp" -> {
                newRuleSet.setEnableTftp((byte) (1 + (handleJson.nextBoolean() ? 1 : 0)));
                return true;
            }
            case "enablepxe" -> {
                newRuleSet.setEnablePxe((byte) (1 + (handleJson.nextBoolean() ? 1 : 0)));
                return true;
            }
            case "enablehttp" -> {
                newRuleSet.setEnableHttp((byte) (1 + (handleJson.nextBoolean() ? 1 : 0)));
                return true;
            }
        }
        return false;
    }

    private boolean parseTftp() throws IOException {
        if (handleJson.peek().equals(JsonToken.BEGIN_OBJECT)) {
            handleJson.beginObject();
            newRuleSet.setTftpSettingSet(parseTftp(handleJson));
            if (newRuleSet.getTftpSettingSet() == null) {
                logger.simpleReport("Error tftp option in rules file", true);
                return false;
            }
            handleJson.endObject();
        } else {
            logger.simpleReport("Error tftp option in rules file", true);
            return false;
        }
        return true;
    }

    @SuppressWarnings("for-rollout:NullAway")
    private TftpSettings parseTftp(JsonReader passedReader) {
        JsonToken nextStage;
        try {
            nextStage = passedReader.peek();
            final TftpSettings returnSettings = new TftpSettings();
            while (!nextStage.equals(JsonToken.END_OBJECT) && passedReader.hasNext()) {
                if (nextStage.equals(JsonToken.NAME)) {
                    if ("rootfolder".equals(passedReader.nextName())) {
                        returnSettings.setRootFs(new File(passedReader.nextString()));
                    } else {
                        logger.simpleReport("Unrecognized TftpService option", true);
                    }
                }
                nextStage = passedReader.peek();
            }
            return returnSettings;
        } catch (final IOException e) {
            logger.simpleReport("Failed to read TftpService option " + e, true);
            return null;
        }
    }

    private boolean parsePxe() throws IOException {
        if (handleJson.peek().equals(JsonToken.BEGIN_ARRAY)) {
            handleJson.beginArray();
            newRuleSet.setRuleSet(parsePxeRules(handleJson));
            if (newRuleSet.getRuleSet() == null) {
                logger.simpleReport("Error pxe rules in rules file", true);
                return false;
            }
            handleJson.endArray();
        } else {
            logger.simpleReport("Error pxe option in rules file", true);
            return false;
        }
        return true;
    }

    /**
     * Compare last changed settings file version to the version passed on to make sure they are
     * compatible.
     *
     * @param readVersion string of version
     * @return bool if the version of config is compatible
     */
    private boolean checkVersionNumber(String readVersion) {
        final String[] supportedSplitVal = LAST_SUPPORTED.split("\\.", -1);
        final String[] currentSplitVal = onbVersion.split("\\.", -1);
        final String[] readSplitVal = readVersion.split("\\.", -1);
        if (readSplitVal.length <= 3) {

            // If at least three separate digits
            for (int i = 0; i < readSplitVal.length; i++) {
                if (Integer.parseInt(readSplitVal[i]) == Integer.parseInt(supportedSplitVal[i])) {
                    // Digit is the same lets go to next digit
                    continue;
                }

                // The digit is not the same
                if (Integer.parseInt(readSplitVal[i]) < Integer.parseInt(supportedSplitVal[i])) {
                    // Read number is smaller than supported
                    return false;
                } else {
                    // Read number is higher than supported, check against higher
                    for (int k = 0; k < readSplitVal.length; k++) {
                        if (Integer.parseInt(readSplitVal[k]) == Integer.parseInt(currentSplitVal[k])) {
                            continue;
                        }
                        // The digit is not the same
                        return Integer.parseInt(readSplitVal[k]) < Integer.parseInt(currentSplitVal[k]);
                    }
                }
            }
            // Only get here if everything was the same
            return true;

        } else {
            logger.simpleReport("ERROR PARSING RULES FILE", true);
            return false;
        }
    }

    /**
     * Go through the PxeService rules and parse each object.
     * @param passedReader the json reader we are using
     * @return a list of rules
     */
    @SuppressWarnings("for-rollout:NullAway")
    private List<BootRule> parsePxeRules(JsonReader passedReader) {
        final List<BootRule> returningRules = new ArrayList<>();

        JsonToken nextStage;
        try {
            nextStage = passedReader.peek();
            while (!nextStage.equals(JsonToken.END_ARRAY) && passedReader.hasNext()) {
                if (nextStage.equals(JsonToken.BEGIN_OBJECT)) {
                    passedReader.beginObject();
                    final BootRule tempRule = parseSinglePxe(passedReader);
                    if (tempRule == null) {
                        continue;
                    }
                    if (tempRule.emptyRule()) {
                        logger.simpleReport("Empty rule read in", true);
                    }
                    returningRules.add(tempRule);
                    passedReader.endObject();
                } else {
                    logger.simpleReport("Error reading rule file", true);
                }
            }
            return returningRules;
        } catch (final IOException e) {
            logger.simpleReport(e.toString(), true);
            return null;
        }
    }

    /**
     * Parse the interior of a single rule.
     * @param passedReader Passed Reader
     * @return single boot rule
     */
    @SuppressWarnings({"CyclomaticComplexity", "for-rollout:NullAway"})
    private BootRule parseSinglePxe(JsonReader passedReader) throws IOException {
        BootRule tempRule = new BootRule();

        JsonToken nextStage = passedReader.peek();
        while (!nextStage.equals(JsonToken.END_OBJECT) && passedReader.hasNext()) {
            if (!nextStage.equals(JsonToken.NAME)) {
                continue;
            }
            boolean parsedRules = false;
            switch (passedReader.nextName().toLowerCase(Locale.ROOT)) {
                case "clientid" -> {
                    tempRule.setClient(passedReader.nextString());
                    parsedRules = true;
                }
                case "bootfile" -> {
                    tempRule.setBootFile(passedReader.nextString());
                    parsedRules = true;
                }
                case "hardwareid" -> {
                    tempRule.setHardwareAddress(passedReader.nextString());
                    parsedRules = true;
                }
                case "serverip" -> {
                    tempRule.setServerIp(passedReader.nextString());
                    parsedRules = true;
                }
                case "tftpip" -> {
                    tempRule.setTftpIp(passedReader.nextString());
                    parsedRules = true;
                }
                case "arch" -> {
                    tempRule = parseArch(tempRule, passedReader.nextString());
                    parsedRules = true;
                }
            }
            if (!parsedRules) {
                return null;
            }
        }

        return tempRule;
    }

    private BootRule parseArch(BootRule tempRule, String readerText) {
        if (readerText.contains(",")) {
            String[] digits = readerText.split(",", -1);
            List<Integer> archSupported = new ArrayList<>();
            for (String digit : digits) {
                if (digit.isEmpty()) {
                    continue;
                }
                archSupported.add(Integer.parseInt(digit));
            }
            int[] archArray = archSupported.stream().mapToInt(i -> i).toArray();
            tempRule.setHardwareTypes(archArray);
        } else {
            int[] archs = {Integer.parseInt(readerText)};
            tempRule.setHardwareTypes(archs);
        }
        return tempRule;
    }
}
