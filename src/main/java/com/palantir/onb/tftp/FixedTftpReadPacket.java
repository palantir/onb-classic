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

package com.palantir.onb.tftp;

import com.palantir.onb.tftp.commons.TftpPacket;
import com.palantir.onb.tftp.commons.TftpPacketException;
import com.palantir.onb.tftp.commons.TftpReadRequestPacket;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The apache TftpService read request packet just skips options, then if you grab the datagram it just
 * drops them wrote this to include them.
 */
class FixedTftpReadPacket {
    private final DatagramPacket packet;
    private final TftpReadRequestPacket preTypedPacket;
    private String filename;
    private final int readMode;
    private final Map<String, byte[]> options;

    /**
     * Start this read packet with a raw datagram packet.
     * @param passedPacket the raw packet incoming
     * @throws TftpPacketException error if it fails to convert
     */
    FixedTftpReadPacket(DatagramPacket passedPacket) throws TftpPacketException, UnsupportedEncodingException {
        packet = passedPacket;
        options = genOptions();
        filename = ((TftpReadRequestPacket) TftpPacket.newTftpPacket(packet)).getFilename();
        readMode = ((TftpReadRequestPacket) TftpPacket.newTftpPacket(packet)).getMode();
        if (filename.charAt(filename.length() - 1) == 65535) {
            filename = filename.substring(0, filename.length() - 1);
        }
        preTypedPacket = (TftpReadRequestPacket) TftpPacket.newTftpPacket(packet);
    }

    /**
     * Return the packet.
     * @return the packet
     */
    DatagramPacket getDatagramPacket() {
        return packet;
    }

    /**
     * Get the file name this packet wants.
     * @return string of the filename
     */
    String getFileName() {
        return filename;
    }

    /**
     * Get packet with typing laid over it.
     * @return typed packet
     */
    TftpReadRequestPacket getTypedPacket() {
        return preTypedPacket;
    }

    /**
     * Does the packet have options in it.
     * @return bool
     */
    boolean hasOptions() {
        return !options.isEmpty();
    }

    /**
     * If the packet has options, make them a map.
     * @return map of options
     */
    Map<String, byte[]> getOptions() {
        return options;
    }

    /**
     * Which type of read request is this, a octet or binary, ect.
     * @return Read mode 0 is netascii, 1 is binary
     */
    public int getReadMode() {
        return readMode;
    }

    /**
     * Convert raw packet to options.
     * @return hash of the options
     */
    private Map<String, byte[]> genOptions() {
        final Map<String, byte[]> returningData = new LinkedHashMap<>();

        final byte[] dataset = packet.getData();
        int start = getPosForOptions();
        if (start == -1) {
            return returningData;
        }
        boolean isData = false;
        // this is a flip flop, when its false then the data read is the
        // option name, when false its the option value
        String tempBuffer = ""; // This is used when we have a option name but no data yet
        for (int i = start; i < dataset.length; i++) {
            if (dataset[i] == 0x00) {
                if (isData) {
                    // This is the options data
                    returningData.put(tempBuffer, Arrays.copyOfRange(dataset, start, i));
                    tempBuffer = null;
                    start = i + 1;
                    isData = false;
                } else {
                    // This is the name of the option
                    // start to the spot before where I am is the option name
                    tempBuffer = new String(Arrays.copyOfRange(dataset, start, i), StandardCharsets.UTF_8);

                    start = i + 1;
                    isData = true;
                }
            }
        }

        return returningData;
    }

    /**
     * Get the location of the options.
     * @return index
     */
    private int getPosForOptions() {
        final byte[] dataset = packet.getData();
        int zerosSeen = 0;
        final int start = 2; // Skipping op code
        for (int i = start; i < dataset.length; i++) {
            if (zerosSeen == 2) {
                return i;
            }
            if (dataset[i] == 0x00) {
                zerosSeen++;
            }
        }
        return -1;
    }
}
