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

import com.palantir.onb.GeneralTools;
import com.palantir.onb.InterfaceData;
import com.palantir.onb.LogStandard;
import com.palantir.onb.types.BootRules;
import com.palantir.onb.types.ProcessedPacket;
import com.palantir.onb.types.PxeOptions;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;

/**
 * Runnable for the DHCP Proxy Service.
 */
public class PxeService implements Runnable {
    private String listenerAddress = "";
    private int listenPort = 67;
    private String[] interfaceName = new String[0];
    private BootRules loadingRules = new BootRules();

        @SuppressWarnings("for-rollout:NullAway")
        private LogStandard logger;

    /**
     * Pass in a logger.
     * @param newLogger logger to use with thread
     */
    public void setLogger(LogStandard newLogger) {
        logger = newLogger;
    }

    /**
     * Set listener address, usually 0.0.0.0.
     * @param passedListener address to listen on
     */
    public void setListenerAddress(String passedListener) {
        listenerAddress = passedListener;
    }

    /**
     * Set which port to listen on, usually 67 or 4011.
     * @param passedPort which port
     */
    public void setListerPort(int passedPort) {
        listenPort = passedPort;
    }

    /**
     * Set Nics to respond on.
     * @param passedNic list of interfaces
     */
    public void setNicName(String[] passedNic) {
        interfaceName = passedNic;
    }

    /**
     * Rules to use.
     * @param passedRules BootRules Object
     */
    public void setBootRules(BootRules passedRules) {
        loadingRules = passedRules;
    }

    /**
     * Listener for that port.
     */
    @Override
    @SuppressWarnings("DnsLookup") // This has worked for years, and errors with suggestions to change
    public void run() {
        // Bug in jdk makes this take up to 30 seconds to respond
        final String hostname = getHostname();
        String tempInterfaceNames = getInterfaces();
        logger.simpleReport(
                Thread.currentThread().getName() + " - Listener Starting on Port " + listenPort
                        + ". Interfaces responding " + tempInterfaceNames,
                false);
        try (DatagramSocket udpSocket = new DatagramSocket(null)) {
            udpSocket.setBroadcast(true);
            udpSocket.setReuseAddress(true);
            final SocketAddress me = new InetSocketAddress(listenerAddress, listenPort);
            udpSocket.bind(me);

            DatagramPacket packet;
            while (!Thread.currentThread().isInterrupted()) {
                final byte[] buf = new byte[2048];
                packet = new DatagramPacket(buf, buf.length);
                udpSocket.setSoTimeout(200);
                try {
                    udpSocket.receive(packet);
                } catch (final IOException e) {
                    // This happens whenever the timeout is hit
                    continue;
                }

                if (!PxeInteraction.validPacketToConvert(packet)) {
                    // Invalid Packet
                    logger.simpleReport(
                            Thread.currentThread().getName() + " - Invalid Packet came in, ignoring...", true);
                    continue;
                }
                final ProcessedPacket transformingOriginal = PxeInteraction.convertPacketToPretty(packet);
                if (transformingOriginal == null) {
                    logger.simpleReport("Packet could not be parsed", true);
                    continue;
                }
                if (!transformingOriginal.requiresResponse()) {
                    logger.report(
                            "",
                            "",
                            "",
                            Thread.currentThread().getName() + " - Packet did not request PxeService information",
                            packet.getAddress().toString() + "/"
                                    + GeneralTools.hardwareMacToString(
                                            transformingOriginal.getClientHardwareAddress(),
                                            transformingOriginal.getHardwareLength())
                                    + " - " + Thread.currentThread().getName()
                                    + " - packet did not request PxeService information",
                            false);
                    continue;
                }
                logger.report(
                        "",
                        "",
                        "Packet Received",
                        Thread.currentThread().getName() + "\tPacket Received",
                        packet.getAddress().toString() + "/"
                                + GeneralTools.hardwareMacToString(
                                        transformingOriginal.getClientHardwareAddress(),
                                        transformingOriginal.getHardwareLength())
                                + " - " + Thread.currentThread().getName() + " - "
                                + packet.getLength() + " Byte Packet Received",
                        false);

                loopOverInterfacesAndSend(hostname, transformingOriginal, packet, udpSocket);
            }
        } catch (final Exception e) {
            logger.simpleReport(e.toString(), true);
        }
        logger.simpleReport(Thread.currentThread().getName() + " - Stopping Thread", false);
    }

    @SuppressWarnings("ReverseDnsLookup")
    private String getHostname() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("mac") || osName.contains("darwin")) {
            logger.simpleReport(
                    Thread.currentThread().getName()
                            + " - MacOS Java seems to have a bug that delays some system variable retrieval "
                            + "ONB needs, we will fetch those now.",
                    false);
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.simpleReport(e.toString(), true);
            return "pxe-server";
        }
    }

    private String getInterfaces() {
        if (interfaceName.length == 0) {
            // No interface was given to filter
            interfaceName = InterfaceData.getInterfaces();
        }

        StringBuilder tempInterfaceNames = new StringBuilder();
        for (String singleInt : interfaceName) {
            tempInterfaceNames.append(singleInt);
            tempInterfaceNames.append(',');
        }
        return tempInterfaceNames.toString();
    }

    private void loopOverInterfacesAndSend(
            String hostname, ProcessedPacket transformingOriginal, DatagramPacket packet, DatagramSocket udpSocket) {
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loop back interface
                }

                for (final String singleInt : interfaceName) {
                    if (networkInterface.getName().equals(singleInt)) {
                        sendPacketForInterface(networkInterface, hostname, transformingOriginal, packet, udpSocket);
                    }
                }
            }
        } catch (final SocketException e) {
            logger.simpleReport(
                    Thread.currentThread().getName() + " - ERROR PXE002: Attempted to start main "
                            + "DHCP proxy service and failed",
                    true);
            logger.simpleReport(e.toString(), true);
        }
    }

    private void sendPacketForInterface(
            final NetworkInterface networkInterface,
            String hostname,
            ProcessedPacket transformingOriginal,
            DatagramPacket packet,
            DatagramSocket udpSocket) {
        for (final InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
            final PxeOptions interactionBootInfo = RuleSelector.ruleMaker(
                    hostname, transformingOriginal, loadingRules, interfaceAddress.getAddress(), logger);
            final ProcessedPacket packetToReturn =
                    PxeInteraction.genReplyPretty(transformingOriginal, interactionBootInfo);
            if (packetToReturn == null) {
                logger.simpleReport("Error processing packet", true);
                return;
            }
            final DatagramPacket buildToShip;
            try {
                buildToShip = PxeInteraction.prettyPacketToDatagram(packetToReturn);
            } catch (UnknownHostException e) {
                logger.simpleReport("Unknown host error, " + e, true);
                return;
            }

            final InetAddress broadcast = interfaceAddress.getBroadcast();
            if (broadcast == null) {
                continue;
            }

            // Send the broadcast packet.
            try (DatagramChannel handle = DatagramChannel.open()) {
                if (packet.getSocketAddress().toString().equals("/0:0:0:0:0:0:0:0:68")
                        || "/0.0.0.0:68".equals(packet.getSocketAddress().toString())) {
                    // This came in from a broadcast
                    processBroadcastDhcpPacket(
                            handle,
                            interfaceAddress,
                            buildToShip,
                            transformingOriginal,
                            udpSocket,
                            broadcast,
                            interactionBootInfo);
                } else {
                    // This is the second stage of boot data OR a DHCP relay
                    processUnicastDhcpPacket(
                            packet,
                            buildToShip,
                            transformingOriginal,
                            udpSocket,
                            networkInterface,
                            interactionBootInfo);
                }

            } catch (final Exception e) {
                logger.simpleReport("ERROR PXE003: " + listenPort + e, true);
            }
        }
    }

    private void sendNetworkBroadcast(
            DatagramChannel handle, InterfaceAddress interfaceAddress, DatagramPacket buildToShip) throws IOException {
        DatagramSocket socket = handle.socket();
        socket.setBroadcast(true);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(interfaceAddress.getAddress(), 67));

        buildToShip.setAddress(InetAddress.getByAddress(new byte[] {-1, -1, -1, -1}));
        socket.send(buildToShip);
        socket.close();
    }

    private void processBroadcastDhcpPacket(
            DatagramChannel handle,
            InterfaceAddress interfaceAddress,
            DatagramPacket buildToShip,
            ProcessedPacket transformingOriginal,
            DatagramSocket udpSocket,
            InetAddress broadcast,
            final PxeOptions interactionBootInfo)
            throws IOException {
        if (loadingRules.getBroadcastSetting() > 0) {
            sendNetworkBroadcast(handle, interfaceAddress, buildToShip);
        }
        // TODO(#2): this hasn't been tested with a real relay agent
        if (!Arrays.equals(transformingOriginal.getRelayAgentIp(), new byte[] {0, 0, 0, 0})) {
            // relay agent in use
            buildToShip.setAddress(InetAddress.getByAddress(transformingOriginal.getRelayAgentIp()));
            udpSocket.send(buildToShip);
            logger.simpleReport("Relay agent", false);
        } else {
            if (loadingRules.getBroadcastSetting() == 0 || loadingRules.getBroadcastSetting() == 2) {
                buildToShip.setAddress(broadcast);
                udpSocket.send(buildToShip);
            }
        }

        logger.report(
                "",
                "",
                "Response sent",
                Thread.currentThread().getName() + " - Broadcast "
                        + loadingRules.getBroadcastSetting()
                        + " response sent from "
                        + listenPort + buildToShip.getAddress(),
                Thread.currentThread().getName() + " - Broadcast "
                        + loadingRules.getBroadcastSetting()
                        + " response sent from "
                        + listenPort + buildToShip.getAddress()
                        + " Containing " + interactionBootInfo.getBootFile(),
                false);
    }

    private void processUnicastDhcpPacket(
            DatagramPacket packet,
            DatagramPacket buildToShip,
            ProcessedPacket transformingOriginal,
            DatagramSocket udpSocket,
            final NetworkInterface networkInterface,
            final PxeOptions interactionBootInfo)
            throws IOException {
        if (packet.getPort() == 67) {
            // Unicast, stage 1
            buildToShip.setAddress(InetAddress.getByAddress(transformingOriginal.getRelayAgentIp()));
            buildToShip.setPort(67);
            udpSocket.send(buildToShip);
            // Stopped here, this is experimental
        } else {
            // This is stage 2 of proxy DHCP
            buildToShip.setAddress(packet.getAddress());
            buildToShip.setPort(4011);
            udpSocket.send(buildToShip);
            logger.report(
                    "",
                    "",
                    "Response sent",
                    "Unicast response sent from " + listenPort,
                    packet.getAddress().toString() + " - "
                            + Thread.currentThread().getName() + "\t"
                            + "Interface: " + networkInterface.getDisplayName()
                            + ", File:"
                            + interactionBootInfo.getBootFile(),
                    false);
        }
    }
}
