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

/**
 * Functions that are used multiple times.
 */
public final class GeneralTools {
    private GeneralTools() {}

    /**
     * Convert from the relative filenames that the packet has in them, to a real system path.
     *
     * @param requestPacket raw packet location request
     * @return file requested, or null if the path escapes the root directory
     */
    @SuppressWarnings("for-rollout:NullAway")
    public static File getRealFileName(String requestPacket, LogStandard localLogger, File rootFs) {
        String parsingRequestPacket = requestPacket;
        if (File.separatorChar == '\\') {
            if (parsingRequestPacket.contains("/")) {
                parsingRequestPacket = parsingRequestPacket.replace('/', '\\');
            }
        } else {
            if (File.separatorChar == '/') {
                if (parsingRequestPacket.contains("\\")) {
                    parsingRequestPacket = parsingRequestPacket.replace('\\', '/');
                }
            }
        }
        String filename = rootFs.getAbsolutePath() + File.separatorChar + parsingRequestPacket;
        File resolved = new File(filename);
        try {
            String canonicalRoot = rootFs.getCanonicalPath() + File.separator;
            String canonicalResolved = resolved.getCanonicalPath();
            if (!canonicalResolved.startsWith(canonicalRoot) && !canonicalResolved.equals(rootFs.getCanonicalPath())) {
                localLogger.simpleReport("Packet attempted escalation out of tftp folder", true);
                return null;
            }
        } catch (IOException e) {
            localLogger.simpleReport("Failed to resolve canonical path", true);
            return null;
        }
        return resolved;
    }

    /**
     * Convert a byte array to string for Ips. Make the default byte[] human readable.
     * @param passedArray byte array
     * @return string of ip
     */
    public static String arrayToString(byte[] passedArray) {
        // (ConvertedToNice.yourIP[0] & 0xFF)
        StringBuilder returningData = new StringBuilder();
        for (int i = 0; i < passedArray.length; i++) {
            if (i == 0) {
                returningData.append(passedArray[i] & 0xFF);
                continue;
            }
            returningData.append(".");
            returningData.append(passedArray[i] & 0xFF);
        }
        return returningData.toString();
    }

    public static String hardwareMacToString(byte[] hardwareAddress, byte length) {
        StringBuilder tempString = new StringBuilder();
        for (byte i = 0; i < length; i++) {
            String hexKey = Integer.toHexString(hardwareAddress[i] & 0xFF);
            if (hexKey.length() < 2) {
                hexKey = "0" + hexKey;
            }
            tempString.append(hexKey);
            tempString.append(':');
        }
        if (tempString.length() > 0) {
            tempString.deleteCharAt(tempString.length() - 1);
        }
        return tempString.toString();
    }
}
