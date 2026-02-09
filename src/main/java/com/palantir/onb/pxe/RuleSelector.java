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

package com.palantir.onb.pxe;

import com.palantir.onb.LogStandard;
import com.palantir.onb.types.BootRules;
import com.palantir.onb.types.DhcpOption;
import com.palantir.onb.types.ProcessedPacket;
import com.palantir.onb.types.PxeOptions;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Rule processor, get the list of rules and find which this packet requires.
 */
final class RuleSelector {
    private RuleSelector() {
    }

    /**
     * Feed in all the details about the host, the packet of what wants to boot, and the rules.
     * Then get the options needing to be set for the return packet.
     * @param hostname This systems hostname, cached for speed
     * @param inputPacket Packet we ware reading against
     * @param bootRules Rule set
     * @param currentServingAddress current system IP
     * @param localLogger logger to make notes
     * @return Options to use
     */
    @SuppressWarnings("DefaultCharset")
    public static PxeOptions ruleMaker(String hostname,
                                       ProcessedPacket inputPacket,
                                       BootRules bootRules,
                                       InetAddress currentServingAddress,
                                       LogStandard localLogger) {
        final PxeOptions returningOptions = new PxeOptions();
        final Optional<DhcpOption> clientIdOption = inputPacket.getOption((byte) 77);

        String clientId;
        clientId = clientIdOption.map(dhcpOption ->
                new String(dhcpOption.getPayload(), StandardCharsets.UTF_8)).orElse("");

        for (int i = 0; i < bootRules.getRuleSet().size(); i++) {
            // We want to know if the mac was required to match, and if so then load IF the client
            // matches as well
            // The so blank is a positive mac match
            boolean macMatched;
            if (!bootRules.getRuleSet().get(i).getHardwareAddress().isEmpty()) {
                final String stripColin =
                        bootRules.getRuleSet().get(i).getHardwareAddress().toUpperCase(Locale.ROOT).replace(":", "");
                macMatched = inputPacket.compareMacs(stripColin.getBytes());
            } else {
                macMatched = true;
            }

            boolean clientMatched = clientId.contains(bootRules.getRuleSet().get(i).getClient());

            // We have been getting some packets where they don't include option 93 but are requesting boot info.
            // Defaulting to not matching arch in that case.
            boolean archMatched = false;
            Optional<DhcpOption> archTypeVar = inputPacket.getOption((byte) 93);
            if (archTypeVar.isPresent()) {
                byte[] tempPaylod = archTypeVar.get().getPayload();
                int archMove = tempPaylod[1];
                archMove += tempPaylod[0] * 256;
                archMatched = matchArch(bootRules.getRuleSet().get(i).getHardwareTypes(), archMove);
            }

            if (macMatched && clientMatched && archMatched) {
                returningOptions.setThisServerIp(
                        stringIpToBytes(bootRules.getRuleSet().get(i).getServerIp(), currentServingAddress));
                returningOptions.setTftpServerIp(
                        stringIpToBytes(bootRules.getRuleSet().get(i).getTftpIp(), currentServingAddress));
                returningOptions.setServerName(hostname);
                returningOptions.setBootFile(bootRules.getRuleSet().get(i).getBootFile());
                return returningOptions;
            }

            if (i == (bootRules.getRuleSet().size() - 1)) {
                localLogger.simpleReport("Could not load a rule to match this system.", true);
            }
        }

        return returningOptions;
    }

    private static boolean matchArch(int[] archs, int arch) {
        if (archs == null || archs.length == 0) {
            return true;
        }

        for (int singleArchAllowed : archs) {
            if (singleArchAllowed == arch) {
                return true;
            }
        }
        return false;
    }

    /**
     * The rule file contains a string for a IP, if that string is 0.0.0.0 it means use this servers
     * IP. Else Convert string to array.
     *
     * @param ruleIp IP to use
     * @return Converted IP, or if all 0s, convert to this systems IP
     */
    private static byte[] stringIpToBytes(String ruleIp, InetAddress currentServerAddress) {
        final byte[] returnData = new byte[4];
        if (ruleIp.equals("0.0.0.0") || ruleIp.equals("")) {
            return currentServerAddress.getAddress();
        }
        final String[] splitIp = ruleIp.split("\\.", -1);
        if (splitIp.length != 4) {
            return new byte[0];
        }

        returnData[0] = (byte) Integer.parseInt(splitIp[0]);
        returnData[1] = (byte) Integer.parseInt(splitIp[1]);
        returnData[2] = (byte) Integer.parseInt(splitIp[2]);
        returnData[3] = (byte) Integer.parseInt(splitIp[3]);
        return returnData;
    }
}
