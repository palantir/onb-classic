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

import com.palantir.onb.pxe.PxeInteraction;
import com.palantir.onb.types.DhcpOption;
import com.palantir.onb.types.ProcessedPacket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

/**
 * Interactive diagnostics for DHCP.
 */
public final class VirtualNic implements Runnable {
    //CHECKSTYLE.OFF: RegexpSinglelineJava
    private static final byte[] myMac = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    private NetworkInterface myInterface;
    private InterfaceAddress primaryInet;
    private final List<ProcessedPacket> datastore = new ArrayList<>();
    private Logging logger;

    @SuppressWarnings("for-rollout:NullAway")
    VirtualNic() {}

    public void setLogger(Logging passedLogger) {
        logger = passedLogger;
    }

    @Override
    public void run() {
        System.out.println("Starting VirtualNic, type 'help' for assistance");
        randmac();
        System.out.println("Randomizing MAC");
        printStatus();
        boolean quitCommand = false;
        while (!quitCommand) {
            String[] splitInput = getUserInput();
            switch (splitInput[0]) {
                case "dhclient" -> {
                    dhclient();
                    continue;
                }
                case "randmac" -> {
                    randmac();
                    printStatus();
                    continue;
                }
                case "status" -> {
                    printStatus();
                    continue;
                }
                case "help" -> {
                    printHelp();
                    continue;
                }
                case "lsint" -> {
                    printInts();
                    continue;
                }
                case "setint" -> {
                    if (splitInput.length > 1) {
                        final String tempInterface = splitInput[1];
                        myInterface = getInterface(tempInterface);
                        printStatus();
                    }
                    continue;
                }
                case "quit" -> {
                    quitCommand = true;
                    System.exit(0);
                    continue;
                }
            }
            System.out.println("Unrecognized Command");
        }
    }

    private String[] getUserInput() {
        Scanner userInputScanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.print(">");
        String userText = userInputScanner.nextLine();
        return userText.split(" ");
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("\trandmac - Randomize Fake Mac");
        System.out.println("\tdhclient - Attempt to get a DHCP address");
        System.out.println("\tstatus - see status of card");
        System.out.println("\tlsint - List interfaces");
        System.out.println("\tsetint - Set interface to use");
        System.out.println("\t");
        System.out.println(
                "\tRun 'randmac', then 'lsint' find the interface you want,"
                + " then 'setint %interface'; finally 'dhclient'.");
        System.out.println("\t");
    }

    private void printStatus() {
        String data = "\tMAC: " + byteToHex(myMac[0]) + ":" + byteToHex(myMac[1]) + ":"
                + byteToHex(myMac[2]) + ":" + byteToHex(myMac[3]) + ":" + byteToHex(myMac[4]) + ":"
                + byteToHex(myMac[5]) + "\t\t\n";
        if (myInterface == null) {
            data += "\tInterface: NOT SET";
        } else {
            data += "\tInterface: " + myInterface.getDisplayName();
        }
        System.out.println(data);
    }

    private void printInts() {
        try {
            final Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = interfaces.nextElement();
                System.out.println(
                        "Interface \"" + networkInterface.getDisplayName() + "\" - "
                                + networkInterface.getName() + " - Online:"
                                + networkInterface.isUp());
                final List<InterfaceAddress> listingAddresses =
                        networkInterface.getInterfaceAddresses();
                for (final InterfaceAddress singleAddress : listingAddresses) {
                    System.out.println("\t" + singleAddress.getAddress().getHostAddress());
                }
            }
        } catch (final SocketException e) {
            logger.simpleReport(e.toString(), true);
        }
    }

    /**
     * Get information on a interface passed in.
     * @param interfaceName pass in a interface name
     * @return return network interface object
     */
    @SuppressWarnings("for-rollout:NullAway")
    private NetworkInterface getInterface(String interfaceName) {
        try {
            final NetworkInterface checkingInterface = NetworkInterface.getByName(interfaceName);
            for (final InterfaceAddress interfaceAddress :
                    checkingInterface.getInterfaceAddresses()) {
                if (interfaceAddress.getBroadcast() != null) {
                    primaryInet = interfaceAddress;
                }
            }
            return checkingInterface;
        } catch (final SocketException e) {
            logger.simpleReport(e.toString(), true);
        }
        return null;
    }

    @SuppressWarnings("CyclomaticComplexity")
    private void dhclient() {
        final ProcessedPacket dhcpRequest = newDhcpClientPacket();

        final DhcpOption discovery = new DhcpOption();
        discovery.setLength((byte) 1);
        discovery.setOption((byte) 0x35);
        final byte[] discover = { 1 };
        discovery.setPayload(discover);

        dhcpRequest.getActiveOptions().add(discovery);

        final DhcpOption requestedParts = new DhcpOption();
        requestedParts.setOption((byte) 0x37);
        requestedParts.setLength((byte) 5);
        // subnet, router, Request Ip, ip lease, dhcp id
        final byte[] parts = { 1, 3, 50, 51, 54 };
        requestedParts.setPayload(parts);

        dhcpRequest.getActiveOptions().add(requestedParts);

        DatagramSocket rudpSocket = null;
        final byte[] buf = new byte[1024];
        final DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            rudpSocket = new DatagramSocket(null);
            final SocketAddress me = new InetSocketAddress(primaryInet.getAddress(), 67);
            rudpSocket.bind(me);
        } catch (final IOException e) {
            logger.simpleReport(e.toString(), true);
        }

        if (!sendInitialDhcpPacket(dhcpRequest)) {
            logger.simpleReport("ERROR VNIC001: Failed to send dhcp request", true);
            return;
        }

        try {
            if (rudpSocket == null || rudpSocket.isClosed()) {
                logger.simpleReport("Socket error getting UDP Data", true);
                return;
            }
            rudpSocket.receive(packet);
            if (!rudpSocket.isClosed()) {
                rudpSocket.close();
            }
        } catch (final IOException e) {
            logger.simpleReport(e.toString(), true);
        }

        final ProcessedPacket convertedToNice = PxeInteraction.convertPacketToPretty(packet);
        if (convertedToNice != null && convertedToNice.getYourIp() == null) {
            return;
        }
        datastore.add(convertedToNice);

        if (convertedToNice == null) {
            return;
        }

        System.out.println("IP: \t\t" + GeneralTools.arrayToString(convertedToNice.getYourIp()));
        DhcpOption routerLoc = PxeInteraction.getOption(convertedToNice, (byte) 1);
        if (routerLoc != null) {
            System.out.println(
                    "Subnet: \t" + GeneralTools.arrayToString(routerLoc.getPayload()));
        }
        System.out.println(
                "Gateway: \t" + GeneralTools.arrayToString(convertedToNice.getNextServer()));
        System.out.println();
        routerLoc = PxeInteraction.getOption(convertedToNice, (byte) 54);
        if (routerLoc != null) {
            System.out.println(
                    "DHCP server: \t" + GeneralTools.arrayToString(routerLoc.getPayload()));
        }
    }

    private ProcessedPacket newDhcpClientPacket() {
        ProcessedPacket dhcpRequest = new ProcessedPacket();
        dhcpRequest.setBootRequest((byte) 0x1);
        dhcpRequest.setHardwareType((byte) 0x1);
        dhcpRequest.setHardwareLength((byte) 0x6);
        dhcpRequest.setHopNum((byte) 0x0);
        final byte[] transId = { myMac[2], myMac[3], myMac[4], myMac[5] };
        dhcpRequest.setTransactionId(transId);
        final byte[] time = { 0x00, 0x01 };
        dhcpRequest.setSecondsPassed(time);
        final byte[] flags = { (byte) 128, 0x00 };
        dhcpRequest.setFlags(flags);
        final byte[] blankIp = { 0x00, 0x00, 0x00, 0x00 };
        dhcpRequest.setClientIp(blankIp);
        dhcpRequest.setRelayAgentIp(primaryInet.getAddress().getAddress());
        dhcpRequest.setNextServer(blankIp);
        dhcpRequest.setYourIp(blankIp);
        dhcpRequest.setClientHardwareAddress(Arrays.copyOf(myMac, 16));
        return dhcpRequest;
    }

    private boolean sendInitialDhcpPacket(ProcessedPacket dhcpRequest) {
        System.out.println("Sending packet out " + primaryInet.toString());
        final DatagramPacket converted;
        try (DatagramSocket udpSocket = new DatagramSocket(68)) {
            converted = PxeInteraction.prettyPacketToDatagram(dhcpRequest);
            converted.setAddress(primaryInet.getBroadcast());
            converted.setPort(67);
            udpSocket.send(converted);
        } catch (IOException e) {
            logger.simpleReport(e.toString(), true);
            return false;
        }
        return true;
    }

    private void randmac() {
        final Random myrand = new Random();
        for (int i = 0; i < 6; i++) {
            final byte rand = (byte) myrand.nextInt(256);
            myMac[i] = rand;
        }
    }

    private String byteToHex(byte passed) {
        return String.format(Locale.ROOT, "%02x", passed);
    }
}
