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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This type represents a dhcp packet broken into parts, so that it is easier to work with and
 * manipulate. <a href="http://www.ietf.org/rfc/rfc2131.txt">RFC</a>
 */
public class ProcessedPacket {
    private byte bootRequest = 0x00;

    /**
     * Get Packet DHCP session type.
     * @return byte of 1 is a request, 2 is a reply
     */
    public byte getBootRequest() {
        return bootRequest;
    }

    /**
     * Set a dhcp session type byte.
     * @param bootRequest byte of type
     */
    public void setBootRequest(byte bootRequest) {
        this.bootRequest = bootRequest;
    }

    private byte hardwareType = 0x00;

    /**
     * Get hardware type, this is designed for 1 which is ethernet, its expected to always be that.
     * @return hardware type as byte
     */
    public byte getHardwareType() {
        return hardwareType;
    }

    /**
     * Set the hardware type, probably 1.
     * @param hardwareType byte of new hardware type
     */
    public void setHardwareType(byte hardwareType) {
        this.hardwareType = hardwareType;
    }

    private byte hardwareLength = 0x00;

    /**
     * Length of the hardware address, for ethernet this will always be 6.
     * @return length of hardware address
     */
    public byte getHardwareLength() {
        return hardwareLength;
    }

    /**
     * Set the hardware address length.
     * @param hardwareLength Length as a byte
     */
    public void setHardwareLength(byte hardwareLength) {
        this.hardwareLength = hardwareLength;
    }

    private byte hopNum = 0x00;

    /**
     * Number of hops to get to client.
     * @return byte of hops
     */
    public byte getHopNum() {
        return hopNum;
    }

    /**
     * Set the hop count.
     * @param hopNum byte of hops
     */
    public void setHopNum(byte hopNum) {
        this.hopNum = hopNum;
    }

    private byte[] transactionId = new byte[4];

    /**
     * 4 byte, client chosen, transaction ID.
     * @return get the transaction id
     */
    public byte[] getTransactionId() {
        return transactionId;
    }

    /**
     * Set the transaction id.
     * @param transactionId bytes of transaction id
     */
    public void setTransactionId(byte[] transactionId) {
        this.transactionId = transactionId;
    }

    private byte[] secondsPassed = new byte[2];

    /**
     * Client sends how many seconds they have been trying to get a IP, server respidns with same
     * number.
     * @return Seconds passed as a byte array
     */
    public byte[] getSecondsPassed() {
        return secondsPassed;
    }

    /**
     * Set how long its been.
     * @param secondsPassed seconds passed
     */
    public void setSecondsPassed(byte[] secondsPassed) {
        this.secondsPassed = secondsPassed;
    }

    private long metaTimeDelivered = 0;

    /**
     * This is used for responding to packets, we can calc the time between in and out.
     * @return time in long
     */
    public long getMetaTimeDelivered() {
        return metaTimeDelivered;
    }

    /**
     * Set the time in long.
     * @param metaTimeDelivered set time
     */
    public void setMetaTimeDelivered(long metaTimeDelivered) {
        this.metaTimeDelivered = metaTimeDelivered;
    }

    private byte[] flags = new byte[2];

    /**
     * First bit is either 1 for broadcast (0x8000) or 0 for unicast (0x0000), rest is 0.
     * @return flags on packet
     */
    public byte[] getFlags() {
        return flags;
    }

    /**
     * Set the type of transmission this packet was.
     * @param flags set teh flags as bytes
     */
    public void setFlags(byte[] flags) {
        this.flags = flags;
    }

    private byte[] clientIp = new byte[4];

    /**
     * Client IP address; only filled in if client is in BOUND, RENEW or REBINDING state and can
     * respond to ARP requests.
     * @return get the IP in byte form
     */
    public byte[] getClientIp() {
        return clientIp;
    }

    /**
     * Set the IP of the client.
     * @param clientIp client ip as bytes
     */
    public void setClientIp(byte[] clientIp) {
        this.clientIp = clientIp;
    }

    private byte[] yourIp = new byte[4];

    /**
     * 'your' (client) IP address. <-RFC helpfulness
     * @return get your ip as bytes
     */
    public byte[] getYourIp() {
        return yourIp;
    }

    /**
     * Set 'your' ip as bytes.
     * @param yourIp your IP you are setting
     */
    public void setYourIp(byte[] yourIp) {
        this.yourIp = yourIp;
    }

    private byte[] nextServer = new byte[4];

    /**
     * PxeService will need to offer this. The next server a system should use to boot.
     * @return bytes of server IP to boot from
     */
    public byte[] getNextServer() {
        return nextServer;
    }

    /**
     * Set what the next server is.
     * @param nextServer next server IP as bytes
     */
    public void setNextServer(byte[] nextServer) {
        this.nextServer = nextServer;
    }

    private byte[] relayAgentIp = new byte[4];

    /**
     * This is only used if a DHCP relay is in use.
     * @return bytes of relay ip
     */
    public byte[] getRelayAgentIp() {
        return relayAgentIp;
    }

    /**
     * Set the relays IP as bytes.
     * @param relayAgentIp relay ip
     */
    public void setRelayAgentIp(byte[] relayAgentIp) {
        this.relayAgentIp = relayAgentIp;
    }

    private byte[] clientHardwareAddress = new byte[16];

    /**
     * We will use first 6 bytes for mac then the rest is padding of 0s.
     * @return hardware address
     */
    public byte[] getClientHardwareAddress() {
        return clientHardwareAddress;
    }

    /**
     * Set hardware address.
     * @param clientHardwareAddress hardware address
     */
    public void setClientHardwareAddress(byte[] clientHardwareAddress) {
        this.clientHardwareAddress = clientHardwareAddress;
    }

    private byte[] serverName = new byte[64];

    /**
     * A null terminating string for hostname of server.
     * @return get server name as bytes
     */
    public byte[] getServerName() {
        return serverName;
    }

    /**
     * Set the server hostname as bytes.
     * @param serverName servername
     */
    public void setServerName(byte[] serverName) {
        this.serverName = serverName;
    }

    private byte[] bootFile = new byte[128];

    /**
     * Bytes of boot file name.
     * @return boot file name in bytes
     */
    public byte[] getBootFile() {
        return bootFile;
    }

    /**
     * Set the boot file as bytes.
     * @param bootFile boot file
     */
    public void setBootFile(byte[] bootFile) {
        this.bootFile = bootFile;
    }

    private List<DhcpOption> activeOptions = new ArrayList<>();

    /**
     * <a href="http://tools.ietf.org/html/rfc2132">RFC</a> DHCP packet options.
     * @return List of options
     */
    public List<DhcpOption> getActiveOptions() {
        return activeOptions;
    }

    /**
     * Set a new list of options.
     * @param activeOptions options
     */
    public void setActiveOptions(List<DhcpOption> activeOptions) {
        this.activeOptions = activeOptions;
    }

    private byte metaStatus = 0x00;

    /**
     * In Bound 0x00 is brand new 0x01 is being handled? 0x02 is a response is in the queue 0x03
     * response sent Outbound 0x10 is brand new 0x11 is being processed 0x12 needs to be sent 0x13
     * has been sent.
     * @return metadata on status
     */
    public byte getMetaStatus() {
        return metaStatus;
    }

    /**
     * Set the packet metadata.
     * @param metaStatus packet status
     */
    public void setMetaStatus(byte metaStatus) {
        this.metaStatus = metaStatus;
    }

    private byte[] metaUdpSource = new byte[4];

    /**
     *  As of version 0.1 this does not work, but I was hoping to get the actual source address and
     *  store it here Broadcasts are making that not possible, since I just see 0.0.0.0.
     * @return UDP data source
     */
    public byte[] getMetaUdpSource() {
        return metaUdpSource;
    }

    /**
     * Set data source ip.
     * @param metaUdpSource bytes of udp source
     */
    public void setMetaUdpSource(byte[] metaUdpSource) {
        this.metaUdpSource = metaUdpSource;
    }

    /**
     * There are times when the program gets a byte array and that data has to be an int
     * <a href="http://stackoverflow.com/questions/1026761/how-to-convert-a-byte-array-to-its-numeric-value-java">ref</a>.
     *
     * @param passedData Byte array
     * @return Int of data from byte array
     */
    public int convertBytesToInt(byte[] passedData) {
        int value = 0;
        for (byte aPassedData : passedData) {
            value = (value << 8) + (aPassedData & 0xff);
        }
        return value;
    }

    /**
     * Check if this type of packet needs a pxe response.
     * @return should we restart
     */
    public boolean requiresResponse() {
        for (final DhcpOption oneOption : activeOptions) {
            if (oneOption.getOption() == (byte) 55) {
                for (final byte requestedOption : oneOption.getPayload()) {
                    if (packetsToRespondTo(requestedOption)) {
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    /**
     * Look for network booting options in DHCP to figure out if we need to respond to this packet.
     * @param requestOption Option code
     * @return true or false to respond
     */
    private boolean packetsToRespondTo(byte requestOption) {
        boolean needsResponse = false;
        switch (requestOption) {
            case (byte) 67, (byte) 129, (byte) 130, (byte) 131, (byte) 132, (byte) 133, (byte) 134, (byte) 135 ->
                    needsResponse = true;
        }
        return needsResponse;
    }

    /**
     * Find a specific DHCP option and return it.
     * @param option Which option is requested
     * @return That options data
     */
    public Optional<DhcpOption> getOption(byte option) {
        for (final DhcpOption singleOption : activeOptions) {
            if (singleOption.getOption() == option) {
                return Optional.of(singleOption);
            }
        }
        return Optional.empty();
    }

    /**
     * Simple compare for two binary mac addresses.
     * @param mac2 Second mac
     * @return do they match
     */
    @SuppressWarnings("NarrowingCompoundAssignment")
    public boolean compareMacs(byte[] mac2) {
        byte[] mac1 = clientHardwareAddress;
        // MAC1 comes from the packet and is in goo shape minus being too long
        // MAC2 is a string that has been turned into bytes, so instead of 00:00:00... its
        // 48,48,48,48,48,48
        final byte[] convertedBytes = new byte[16];
        for (int i = 0; i < 12; i += 2) {
            byte char1 = mac2[i];
            char1 -= 48;
            if (char1 > 9) {
                char1 -= 7;
            }
            byte char2 = mac2[i + 1];
            char2 -= 48;
            if (char2 > 9) {
                char2 -= 7;
            }
            convertedBytes[i / 2] = (byte) (char1 << 4);
            convertedBytes[i / 2] += char2;
        }
        return Arrays.equals(convertedBytes, mac1);
    }
}
