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

import com.palantir.onb.types.DhcpOption;
import com.palantir.onb.types.ProcessedPacket;
import com.palantir.onb.types.PxeOptions;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Brings the packet through the DHCP process.
 */
public final class PxeInteraction {

    private PxeInteraction() {}

    /**
     * Get a single option out of a packet.
     * @param inputPacket Processed packet to go through
     * @param option which option number we want
     * @return the option, or null
     */
        @SuppressWarnings("for-rollout:NullAway")
        public static DhcpOption getOption(ProcessedPacket inputPacket, byte option) {
        for (final DhcpOption singleOption : inputPacket.getActiveOptions()) {
            if (singleOption.getOption() == option) {
                return singleOption;
            }
        }
        return null;
    }

    /**
     * Generate a nicely formatted packet that is a reply for the one given.
     * @param singlePacket the input packet
     * @param bootOption what options to put into our return
     * @return the new, ready to ship packet
     */
    @SuppressWarnings({"DefaultCharset", "for-rollout:NullAway"})
    static ProcessedPacket genReplyPretty(ProcessedPacket singlePacket, PxeOptions bootOption) {
        final ProcessedPacket returningPacket = new ProcessedPacket();
        returningPacket.setBootRequest((byte) 0x2);
        returningPacket.setHardwareType((byte) 0x1);
        returningPacket.setHardwareLength((byte) 0x6);
        returningPacket.setHopNum((byte) 0x0); // not needed but we will set for fun
        returningPacket.setTransactionId(singlePacket.getTransactionId());
        long processedTime = (System.currentTimeMillis() - singlePacket.getMetaTimeDelivered()) / 1000;
        processedTime += singlePacket.convertBytesToInt(singlePacket.getSecondsPassed());

        if (processedTime <= 32768) {
            byte[] seconds;
            // Time is shorter than max of the mem segment
            if (processedTime <= 254) {
                // This data can fit in a single byte
                seconds = new byte[] {0x0, (byte) processedTime};
            } else {
                seconds = new byte[] {(byte) (processedTime >>> 8), (byte) processedTime};
            }
            returningPacket.setSecondsPassed(seconds);
        } else {
            return null;
        }
        if (singlePacket.getFlags()[0] == -128) {
            // this is a broadcast
            returningPacket.setFlags(singlePacket.getFlags());
            returningPacket.setBootFile(stringToByteCon("", 128, false));

            // Offer for broadcasts
            final DhcpOption dhcpMessageType = new DhcpOption();
            dhcpMessageType.setOption((byte) 53);
            dhcpMessageType.setLength((byte) 1);
            byte[] broadcastPayload = {2};
            dhcpMessageType.setPayload(broadcastPayload);
            returningPacket.getActiveOptions().add(dhcpMessageType);
        } else {
            // unicast packet
            returningPacket.setFlags(singlePacket.getFlags());
            returningPacket.setBootFile(stringToByteCon(bootOption.getBootFile(), 128, false));

            // Ack for unicasts
            final DhcpOption dhcpMessageType = new DhcpOption();
            dhcpMessageType.setOption((byte) 53);
            dhcpMessageType.setLength((byte) 1);
            byte[] broadcastPayload = {5};
            dhcpMessageType.setPayload(broadcastPayload);
            returningPacket.getActiveOptions().add(dhcpMessageType);
        }

        returningPacket.setClientIp(singlePacket.getClientIp());
        returningPacket.setYourIp(singlePacket.getYourIp());
        returningPacket.setNextServer(bootOption.getThisServerIp());

        /*
         * This is used for dhcp that bounces through subnets.
         */
        returningPacket.setRelayAgentIp(singlePacket.getRelayAgentIp());

        returningPacket.setClientHardwareAddress(singlePacket.getClientHardwareAddress());

        /*
         * This should be 64 bytes, with data at front of data space, then fill with 0 Also the name
         * can not have spaces for some cards, so we change them to _ .
         */
        returningPacket.setServerName(stringToByteCon(bootOption.getServerName(), 64, true));

        final DhcpOption dhcpIdentifier = new DhcpOption();
        dhcpIdentifier.setOption((byte) 54);
        dhcpIdentifier.setLength((byte) 4);
        dhcpIdentifier.setPayload(bootOption.getThisServerIp());
        returningPacket.getActiveOptions().add(dhcpIdentifier);

        for (final DhcpOption checkingOption : singlePacket.getActiveOptions()) {
            switch (checkingOption.getOption()) {
                case 60 -> {
                    final DhcpOption dcpVendorType = new DhcpOption();
                    dcpVendorType.setOption((byte) 60);
                    final String clientId = new String(checkingOption.getPayload());
                    final String[] clientIdParts = clientId.split(":", -1);
                    dcpVendorType.setPayload(clientIdParts[0].getBytes());
                    dcpVendorType.setLength((byte) dcpVendorType.getPayload().length);
                    returningPacket.getActiveOptions().add(dcpVendorType);
                }
                case 97 -> {
                    final DhcpOption dhcpReturnUid = new DhcpOption();
                    dhcpReturnUid.setOption((byte) 97);
                    dhcpReturnUid.setPayload(checkingOption.getPayload());
                    dhcpReturnUid.setLength((byte) dhcpReturnUid.getPayload().length);
                    returningPacket.getActiveOptions().add(dhcpReturnUid);
                }
            }
        }

        // This is needed for ipxe support, and those packets come in as broadcasts
        final DhcpOption bootfileOption = new DhcpOption();
        bootfileOption.setOption((byte) 67);
        bootfileOption.setLength((byte) bootOption.getBootFile().length());
        bootfileOption.setPayload(stringToByteCon(
                bootOption.getBootFile(), bootOption.getBootFile().length(), false));
        returningPacket.getActiveOptions().add(bootfileOption);

        returningPacket.setMetaStatus((byte) 0x12);
        return returningPacket;
    }

    /**
     * Convert strings to properly padded byte[] arrays.
     * @param passedString String to convert
     * @param arrayLength how long our String is
     * @param removeSpaces should replace spaces with underscores
     * @return byte array
     */
    @SuppressWarnings("DefaultCharset")
    private static byte[] stringToByteCon(String passedString, int arrayLength, boolean removeSpaces) {
        final byte[] returningArray = new byte[arrayLength];
        String workingString = passedString;
        if (removeSpaces) {
            workingString = workingString.trim();
            if (workingString.contains(" ")) {
                workingString = workingString.replace(' ', '_');
            }
        }
        final byte[] stringArray = workingString.getBytes();
        int workingLength = stringArray.length;
        if (workingLength > arrayLength) {
            workingLength = arrayLength;
        }
        System.arraycopy(stringArray, 0, returningArray, 0, workingLength);
        // Pad the rest
        for (int i = workingLength; i < returningArray.length; i++) {
            returningArray[i] = 0x0;
        }
        return returningArray;
    }

    /**
     * Verify the packet is valid.
     * @param passedPacket raw packet
     * @return if its a valid packet
     */
    static boolean validPacketToConvert(DatagramPacket passedPacket) {
        final ProcessedPacket tempPacket = new ProcessedPacket();
        final byte[] rawPacket = passedPacket.getData();
        if (rawPacket.length < 240) {
            // This is smaller than just a dhcp packet with no options, can not be good
            return false;
        }

        for (int i = 240; i < rawPacket.length - 1; ) {
            if (rawPacket[i] == -1) {
                // Ending with end signal, then no more data
                return endOfPacket(rawPacket, i + 1);
            }

            // It's not the end of the packet yet
            final DhcpOption nextOption = new DhcpOption();
            nextOption.setOption(rawPacket[i]);
            nextOption.setLength(rawPacket[i + 1]);
            if (nextOption.getOption() == -1 || endOfPacket(rawPacket, i)) {
                // This is the end of the packet
                break;
            } else {
                if ((i + 2 + (nextOption.getLength() & 0xFF)) <= rawPacket.length) {
                    try {
                        nextOption.setPayload(
                                Arrays.copyOfRange(rawPacket, i + 2, i + 2 + (nextOption.getLength() & 0xFF)));
                        tempPacket.getActiveOptions().add(nextOption);
                        i += 2 + (nextOption.getLength() & 0xFF);
                    } catch (final ArrayIndexOutOfBoundsException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Here we convert a raw datagram packet into our nice organized type.
     *
     * @param passedPacket - The incoming packet
     * @return - Nice organized packet
     */
        @SuppressWarnings("for-rollout:NullAway")
        public static ProcessedPacket convertPacketToPretty(DatagramPacket passedPacket) {
        final ProcessedPacket tempPacket = new ProcessedPacket();
        final byte[] rawPacket = passedPacket.getData();
        if (rawPacket.length < 236) {
            // This is smaller than just a dhcp packet with no options, can not be good
            return null;
        }
        tempPacket.setMetaStatus((byte) 0x00);
        tempPacket.setMetaUdpSource(passedPacket.getAddress().getAddress());
        tempPacket.setBootRequest(rawPacket[0]);
        tempPacket.setHardwareType(rawPacket[1]);
        tempPacket.setHardwareLength(rawPacket[2]);
        tempPacket.setHopNum(rawPacket[3]);
        tempPacket.setTransactionId(Arrays.copyOfRange(rawPacket, 4, 8));
        tempPacket.setSecondsPassed(Arrays.copyOfRange(rawPacket, 8, 10));
        tempPacket.setMetaTimeDelivered(System.currentTimeMillis());
        tempPacket.setFlags(Arrays.copyOfRange(rawPacket, 10, 12));
        tempPacket.setClientIp(Arrays.copyOfRange(rawPacket, 12, 16));
        tempPacket.setYourIp(Arrays.copyOfRange(rawPacket, 16, 20));
        tempPacket.setNextServer(Arrays.copyOfRange(rawPacket, 20, 24));
        tempPacket.setRelayAgentIp(Arrays.copyOfRange(rawPacket, 24, 28));
        tempPacket.setClientHardwareAddress(Arrays.copyOfRange(rawPacket, 28, 44));
        tempPacket.setServerName(Arrays.copyOfRange(rawPacket, 44, 108));
        tempPacket.setBootFile(Arrays.copyOfRange(rawPacket, 108, 236));
        // Here is the options magic packet, 63 82 53 63
        // I had several clients not give options after this, and that forced a crash
        if (rawPacket.length > 242) {
            for (int i = 240; i < rawPacket.length - 1; ) {
                if (rawPacket[i] == -1) {
                    // localLogger.simpleReport("Exited fine", true);
                    i++;
                    if (!endOfPacket(rawPacket, i)) {
                        // localLogger.simpleReport("Exited null", true);
                        return null;
                    } else {
                        // localLogger.simpleReport("NOT EXIT NULL fine", true);
                        return tempPacket;
                    }
                }

                // So it's not the end of the packet yet
                final DhcpOption nextOption = new DhcpOption();
                nextOption.setOption(rawPacket[i]);
                nextOption.setLength(rawPacket[i + 1]);
                if (nextOption.getOption() == -1 || endOfPacket(rawPacket, i)) {
                    // This is the end of the packet
                    return tempPacket;
                } else {
                    if ((i + 2 + (nextOption.getLength() & 0xFF)) <= rawPacket.length) {
                        try {
                            nextOption.setPayload(
                                    Arrays.copyOfRange(rawPacket, i + 2, i + 2 + (nextOption.getLength() & 0xFF)));
                            tempPacket.getActiveOptions().add(nextOption);
                            i += 2 + (nextOption.getLength() & 0xFF);
                        } catch (final ArrayIndexOutOfBoundsException e) {
                            // localLogger.simpleReport("ERROR PXE004: Something weird happened
                            // trying to read packet options, packet too long", true);
                            return null;
                        }
                    } else {
                        // localLogger.simpleReport("ERROR PXE004: Something weird happened trying
                        // to read packet options", true);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private static boolean endOfPacket(byte[] data, int startingOfEnd) {
        for (int k = startingOfEnd; k < data.length; k++) {
            if (data[k] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert a nice organized packet to something we can send over the wire.
     *
     * @param workingPacket - The organized packet
     * @return raw packet
     */
    public static DatagramPacket prettyPacketToDatagram(ProcessedPacket workingPacket) throws UnknownHostException {
        int size = 241; // this is standard heading size, 240 for start, 1 for ff at the end
        for (final DhcpOption options : workingPacket.getActiveOptions()) {
            size += options.getLength() + 2; // one for type, one for length byte
        }
        size += 1; // we will call this the realtek fix
        size += 20; // add 20 bytes of padding at the end
        final byte[] finalPayload = new byte[size];
        finalPayload[0] = workingPacket.getBootRequest();
        finalPayload[1] = workingPacket.getHardwareType();
        finalPayload[2] = workingPacket.getHardwareLength();
        finalPayload[3] = workingPacket.getHopNum();
        System.arraycopy(workingPacket.getTransactionId(), 0, finalPayload, 4, 4);
        System.arraycopy(workingPacket.getSecondsPassed(), 0, finalPayload, 8, 2);
        System.arraycopy(workingPacket.getFlags(), 0, finalPayload, 10, 2);
        System.arraycopy(workingPacket.getClientIp(), 0, finalPayload, 12, 4);
        System.arraycopy(workingPacket.getYourIp(), 0, finalPayload, 16, 4);
        System.arraycopy(workingPacket.getNextServer(), 0, finalPayload, 20, 4);
        System.arraycopy(workingPacket.getRelayAgentIp(), 0, finalPayload, 24, 4);
        System.arraycopy(workingPacket.getClientHardwareAddress(), 0, finalPayload, 28, 16);
        System.arraycopy(workingPacket.getServerName(), 0, finalPayload, 44, 64);
        System.arraycopy(workingPacket.getBootFile(), 0, finalPayload, 108, 128);
        finalPayload[236] = 0x63;
        finalPayload[237] = (byte) 0x82;
        finalPayload[238] = 0x53;
        finalPayload[239] = 0x63;

        int place = 240;
        for (final DhcpOption decomposing : workingPacket.getActiveOptions()) {
            // DEAR REALTEK, "Options containing NVT ASCII data SHOULD NOT include a trailing NULL;
            // however, the receiver of such options MUST be prepared to delete trailing nulls if
            // they exist." READ IT http://tools.ietf.org/html/rfc2132
            // if (Decomposing.option == 67){ continue;}
            finalPayload[place] = decomposing.getOption();
            finalPayload[place + 1] = decomposing.getLength();
            System.arraycopy(decomposing.getPayload(), 0, finalPayload, place + 2, decomposing.getLength());
            place += 2 + decomposing.getLength();
            if (decomposing.getOption() == 67) {
                finalPayload[place] = 0x0;
                place++;
            }
        }
        finalPayload[size - 21] = (byte) 0xff; // close it out
        for (int i = 0; i < 20; i++) {
            finalPayload[size - (20 - i)] = 0x0;
        }

        final byte[] broadcast = new byte[4];
        broadcast[0] = -1;
        broadcast[1] = -1;
        broadcast[2] = -1;
        broadcast[3] = -1;
        InetAddress sourceAddress = InetAddress.getByAddress(broadcast);

        return new DatagramPacket(finalPayload, size, sourceAddress, 68);
    }
}
