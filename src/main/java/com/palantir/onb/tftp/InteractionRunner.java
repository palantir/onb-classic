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

import com.palantir.onb.GeneralTools;
import com.palantir.onb.LogStandard;
import com.palantir.onb.tftp.commons.TftpAckPacket;
import com.palantir.onb.tftp.commons.TftpErrorPacket;
import com.palantir.onb.tftp.commons.TftpPacket;
import com.palantir.onb.tftp.commons.TftpPacketException;
import com.palantir.onb.tftp.commons.TftpReadRequestPacket;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This is a multithreaded TftpService server, for when clients need to get files over TftpService.
 *
 * <a href="http://tools.ietf.org/html/rfc1350">RFC 1350</a>
 * <a href="http://www.faqs.org/rfcs/rfc2347.html">RFC 2347</a>
 * <a href="https://svn.apache.org/repos/asf/commons/proper/net/trunk/src/test/java/org/apache/commons/net/tftp/TFTPServer.java">Apache old example tftp</a>
 *
 */
public class InteractionRunner implements Runnable {
    private final DatagramPacket rawPacketToRespondTo;
    private final TftpSettings internalSettings;
    private DatagramSocket threadSocket;
    private short maxTransmitPacketSize = 512;
    private final LogStandard localLogger;

    /**
     * This function is killed, because if you convert to TFTPPacket then try to read the datagram
     * you lose data, like options Feed in information needed to run the runnable.
     *
     * @param packetToRespondTo The first packet, the one we are responding to then opening
     *        communications with
     * @param settingsFile Settings on how this TftpService server is configured and where the root file
     *        system is
     **/
    @SuppressWarnings("for-rollout:NullAway")
    InteractionRunner(DatagramPacket packetToRespondTo, TftpSettings settingsFile, LogStandard passedLogger) {
        rawPacketToRespondTo = packetToRespondTo;
        internalSettings = settingsFile;
        localLogger = passedLogger;
    }

    /**
     * The runner that does communications.
     */
    @SuppressWarnings("for-rollout:PatternMatchingInstanceof")
    @Override
    public void run() {
        TftpPacket workingPacket;
        try {
            workingPacket = TftpPacket.newTftpPacket(rawPacketToRespondTo);
        } catch (final TftpPacketException e1) {
            localLogger.report(
                    "TftpService - ERROR TFTP001: Attempted to read new TftpService Chain packet and failed :(",
                    "TftpService - ERROR TFTP001: Attempted to read new TftpService Chain packet and failed :(",
                    "TftpService - ERROR TFTP001: Attempted to read new TftpService Chain packet and failed :(",
                    "TftpService - ERROR TFTP001: - Read Request packet error",
                    "TftpService - ERROR TFTP001: - Read Request packet error- "
                            + rawPacketToRespondTo.getAddress().toString(),
                    true);
            return;
        }
        /*
           RFC
           TFTP supports five types of packets, all of which have been mentioned
           above:
                 opcode  operation
                   1     Read request (RRQ)   - Implemented below
                   2     Write request (WRQ)  - Not currently supported
                   3     Data (DATA)          - Handled later
                   4     Acknowledgment (ACK) - This is handled below in
                   5     Error (ERROR)        - Handled later
        */
        if (workingPacket instanceof TftpReadRequestPacket) {
            localLogger.report(
                    "",
                    "",
                    "",
                    " - Read Request - For " + ((TftpReadRequestPacket) workingPacket).getFilename(),
                    workingPacket.getAddress().toString() + " - TftpService - " + "Read Request "
                            + ((TftpReadRequestPacket) workingPacket).getFilename(),
                    false);
            FixedTftpReadPacket newPacket;
            try {
                newPacket = new FixedTftpReadPacket(rawPacketToRespondTo);
            } catch (Exception e) {
                localLogger.simpleReport(e.toString(), true);
                return;
            }
            respondToReadRequest(newPacket);
            return;
        }

        localLogger.simpleReport("Could not find TFTP packet type" + workingPacket.getType(), true);
    }

    /***
     * Start a new connection session to respond to this request.
     */
    private void openConnectionBack() {
        try {
            threadSocket = new DatagramSocket();
            // Hopefully this doesn't cause issues, but we don't want it waiting forever
            threadSocket.setSoTimeout(5000);
        } catch (final SocketException e) {
            localLogger.simpleReport(
                    "TftpService - ERROR TFTP002: Failed to open a returning socket for TftpService", true);
            localLogger.simpleReport(e.toString(), true);
        }
    }

    /***
     * Checks then closes an existing connection.
     */
    private void closeConnectionBack() {
        if (!threadSocket.isClosed()) {
            threadSocket.close();
        }
    }

    /***
     * Sending a formatted TftpService packet in one line.
     *
     * @param sendingPacket Packet to send
     * @return did it work
     */
    private boolean trySend(TftpPacket sendingPacket) {
        try {
            threadSocket.send(sendingPacket.newDatagram());
        } catch (final IOException e) {
            localLogger.simpleReport(
                    "TftpService - ERROR TFTP003: Attempted to send a return TftpService packet and failed", true);
            localLogger.simpleReport(e.toString(), true);
            return false;
        }
        return true;
    }

    /**
     * Sending a raw TftpService packet in one line.
     *
     * @param sendingPacket packet to send
     * @return Does it look like it worked
     */
    private boolean trySend(DatagramPacket sendingPacket) {
        try {
            threadSocket.send(sendingPacket);
        } catch (final IOException e) {
            localLogger.simpleReport(
                    "TftpService - ERROR TFTP004: Attempted to send a return TftpService datagram packet and failed",
                    true);
            localLogger.simpleReport(e.toString(), true);
            return false;
        }
        return true;
    }

    /**
     * Open a port, send one packet then close the socket, used in error packets and such.
     *
     * @param sendingPacket packet to send out.
     */
    private void oneResponseSend(TftpPacket sendingPacket) {
        try {
            // New TftpService server for the communications
            threadSocket = new DatagramSocket();
            threadSocket.setSoTimeout(5000);
            threadSocket.send(sendingPacket.newDatagram());
            threadSocket.close();
        } catch (final IOException e) {
            localLogger.simpleReport(
                    "TftpService - ERROR TFTP005: Attempted to send a return TftpService packet "
                            + "in a single transaction and failed",
                    true);
            localLogger.simpleReport(e.toString(), true);
        }
    }

    /**
     * Attempt to get a new TFTP packet from this connection, if it fails we try again then fail out.
     * @return Packet if gotten or null if we never got a response
     */
    @SuppressWarnings("for-rollout:NullAway")
    private TftpPacket getOneTftpPacket() {
        int retryAttempt = 0;
        while (retryAttempt < 2) {
            final DatagramPacket manualMode = getOnePacket();
            TftpPacket tempData = null;
            try {
                tempData = TftpPacket.newTftpPacket(manualMode);
            } catch (final TftpPacketException e) {
                // We never got a response for the last packet, lets retry
                if (retryAttempt > 0) {
                    closeConnectionBack();
                    return null;
                }
                retryAttempt++;
            }
            if (tempData != null) {
                return tempData;
            }
        }
        return null;
    }

    /**
     * Receive one packet from our open socket.
     *
     * @return our one datagram packet
     */
    private DatagramPacket getOnePacket() {
        final byte[] buffer = new byte[1024];
        final DatagramPacket manualMode = new DatagramPacket(buffer, buffer.length);
        try {
            threadSocket.receive(manualMode);
        } catch (final IOException e) {
            localLogger.simpleReport(
                    "TftpService - ERROR TFTP005: Attempted to receive a return TftpService packet"
                            + " in a single transaction and failed",
                    true);
            localLogger.simpleReport(e.toString(), true);
        }
        return manualMode;
    }

    /**
     * Feed it a block number, then it will return the next - up to - total amount connection will
     * allow.
     *
     * @param requested the file we want t get
     * @param offset where to start in that file
     * @return the bytes after that
     */
    private byte[] getDataChunk(File requested, long offset) {
        int buffSize;
        final long totalSize = requested.length();
        if ((totalSize - offset) < 0) {
            // Something bad happened... We are asking for data beyond the size of teh file
            return new byte[0];
        }
        if ((totalSize - offset) > maxTransmitPacketSize) {
            buffSize = maxTransmitPacketSize;
        } else {
            buffSize = (int) (totalSize - offset);
        }

        final byte[] myBuffer = new byte[buffSize];
        try (FileInputStream readBits = new FileInputStream(requested)) {
            long skippedBytes = readBits.skip(offset);
            if (skippedBytes != offset) {
                localLogger.simpleReport(
                        "TftpService - ERROR TFTP0##: " + "Could not seek to proper file location", true);
            }
            skippedBytes = readBits.read(myBuffer, 0, buffSize);
            if (skippedBytes != buffSize) {
                localLogger.simpleReport(
                        "TftpService - ERROR TFTP0##: " + "Could not read all of data wanted in file", true);
            }
        } catch (final IOException e) {
            localLogger.simpleReport(
                    "TftpService - ERROR TFTP006: Attempted to read file to send over TftpService and failed", true);
            localLogger.simpleReport(e.toString(), true);
        }
        return myBuffer;
    }

    // CHECKSTYLE.OFF: CyclomaticComplexity
    /**
     * This function manages the entire process for handling a read request.
     */
    private void respondToReadRequest(FixedTftpReadPacket workingPacket) {
        File requestedFile = getRequestedFileOrReturnNull(workingPacket);
        if (requestedFile == null) {
            return;
        }
        // TODO(#3): Right now this only works with octet mode
        openConnectionBack();
        // we may have to advance the starting block if we have options
        int lastPort = rawPacketToRespondTo.getPort();

        if (workingPacket.getReadMode() == 0) {
            // This is currently not supported
            errorOnNetAsciiReadAttempt(lastPort);
        }
        if (workingPacket.hasOptions()) {
            lastPort = returnOptionsIfNegotiating(workingPacket, requestedFile, lastPort);
            if (lastPort == -1) {
                // Error occurred in negotiating
                closeConnectionBack();
                return;
            }
        }

        // Option neg is over, now data sending
        long filePosition = 0;
        long fileTotalLength = requestedFile.length();
        boolean needToSendFinalEmptyData =
                requestedFile.length() != 0 && (maxTransmitPacketSize % requestedFile.length() == 0);
        // If the file ends on a packet boundary, then we need to send one last data packet with 0 data to trigger the
        // final end ack, if needsToSendFinalEmptyData is true, this needs one more empty packet to set to false and end
        // transaction.
        while (filePosition < fileTotalLength || (filePosition == fileTotalLength && needToSendFinalEmptyData)) {
            // Get data from file to send to client
            byte[] myBuffer = getDataChunk(requestedFile, filePosition);
            // Turn that data into a packet
            FixedTftpDataPacket returningNewData = new FixedTftpDataPacket(
                    workingPacket.getDatagramPacket().getAddress(),
                    lastPort,
                    getNextBlockNumber(filePosition),
                    myBuffer);
            // Send!
            if (!trySend(returningNewData.getDatagram())) {
                localLogger.simpleReport("Error sending packet", true);
            }

            // Now handle the response
            TftpPacket tempData = getOneTftpPacket();
            if (tempData == null) {
                return;
            }
            switch (tempData.getType()) {
                case 4 -> {
                    // Type 3 is data, we are sending so that is not applicable here
                    // Ack packet
                    if (filePosition == fileTotalLength) {
                        // this is the response to the last empty data packet
                        needToSendFinalEmptyData = false;
                    }
                    if (!properPacketIsBeingAcked(tempData, filePosition)) {
                        localLogger.simpleReport("Error responding", true);
                        continue; // This is for out of order file chunk response
                    }
                    filePosition += myBuffer.length;
                }
                case 5 -> {
                    // Error packet
                    localLogger.simpleReport(
                            "ERROR TFTP012: TftpService error packet received, ending communications", true);
                    closeConnectionBack();
                    return;
                }
            }
        }

        localLogger.report(
                "",
                "",
                "",
                "Packet Transferred Successfully",
                workingPacket.getDatagramPacket().getAddress().toString() + " - TftpService - Transfer of "
                        + workingPacket.getFileName() + " complete",
                false);
        closeConnectionBack();
    }
    // CHECKSTYLE.ON

    @SuppressWarnings("for-rollout:NullAway")
    private File getRequestedFileOrReturnNull(FixedTftpReadPacket fullPacket) {
        File crapFileGetter =
                GeneralTools.getRealFileName(fullPacket.getFileName(), localLogger, internalSettings.getRootFs());

        if (crapFileGetter == null) {
            final TftpPacket errorPacket = new TftpErrorPacket(
                    fullPacket.getTypedPacket().getAddress(),
                    fullPacket.getTypedPacket().getPort(),
                    TftpErrorPacket.ACCESS_VIOLATION,
                    "Access Denied");
            oneResponseSend(errorPacket);
            return null;
        }

        if (!crapFileGetter.exists()) {
            // Cant use buffered sender, that removed the null terminator and creates a error
            final TftpPacket test = new TftpErrorPacket(
                    fullPacket.getTypedPacket().getAddress(),
                    fullPacket.getTypedPacket().getPort(),
                    TftpErrorPacket.FILE_NOT_FOUND,
                    "File Not Found");
            localLogger.report(
                    "",
                    "",
                    "",
                    "TftpService - ERROR TFTP014: Attempted to access file program doesn't exist",
                    "TftpService - ERROR TFTP014: Attempted to access file program doesn't exist "
                            + crapFileGetter.getName(),
                    true);
            oneResponseSend(test);
            return null;
        }
        if (!crapFileGetter.canRead()) {
            openConnectionBack();
            if (!trySend(new TftpErrorPacket(
                    fullPacket.getTypedPacket().getAddress(),
                    fullPacket.getTypedPacket().getPort(),
                    TftpErrorPacket.ACCESS_VIOLATION,
                    ""))) {
                localLogger.simpleReport("Failed to send error, could not access file", true);
            }
            localLogger.report(
                    "",
                    "",
                    "",
                    "TftpService - ERROR TFTP013: Attempted to access file program doesn't have permissions for",
                    "TftpService - ERROR TFTP013: Attempted to access file program doesn't have permissions for. "
                            + crapFileGetter.getName(),
                    true);
            closeConnectionBack();
            return null;
        }
        return crapFileGetter;
    }

    private void errorOnNetAsciiReadAttempt(int lastPort) {
        trySend(new TftpErrorPacket(
                rawPacketToRespondTo.getAddress(),
                lastPort,
                0,
                "Server does not support Netascii, switch to octet mode"));
        localLogger.simpleReport("Client requested netascii which is not supported", true);
        closeConnectionBack();
    }

    /**
     * Handle communicating options, if the communication goes well, return true.
     * @param workingPacket packet from first communication
     * @param requestedFile file the user is trying to get
     * @return last port that was used, -1 is error
     */
    @SuppressWarnings("DefaultCharset")
    private int returnOptionsIfNegotiating(FixedTftpReadPacket workingPacket, File requestedFile, int lastPort) {
        TftpOptionAck ackPacket = new TftpOptionAck();
        final Map<String, byte[]> goThroughValues = workingPacket.getOptions();

        for (Entry<String, byte[]> myItem : goThroughValues.entrySet()) {
            switch (myItem.getKey()) {
                case "blksize" -> {
                    final String parsedDataSize = new String(myItem.getValue(), StandardCharsets.UTF_8);
                    maxTransmitPacketSize = Short.parseShort(parsedDataSize);
                    ackPacket.addOption("blksize", myItem.getValue());
                }
                case "tsize" ->
                    // http://tools.ietf.org/html/rfc2349
                    // Return Op code 6, tsize of file in bytes
                    ackPacket.addOption(
                            "tsize", String.valueOf(requestedFile.length()).getBytes());
                default -> {
                    localLogger.simpleReport(
                            "TftpService - ERROR TFTP008: Error in negotiating TftpService terms, [" + myItem.getKey()
                                    + "] options, closing session",
                            true);
                    if (!trySend(new TftpErrorPacket(
                            rawPacketToRespondTo.getAddress(), lastPort, 0, "Option Not Defined"))) {
                        localLogger.simpleReport("Could not send packet, " + "saying options cant be used", true);
                    }
                    return -1;
                }
            }
        }
        int newLastPort = lastPort;
        if (ackPacket.hasOptions()) {
            DatagramPacket ackPacketWithInfo = ackPacket.getDatagram();
            ackPacketWithInfo.setAddress(rawPacketToRespondTo.getAddress());
            ackPacketWithInfo.setPort(lastPort);
            trySend(ackPacketWithInfo);
            final DatagramPacket newPacketCheck = getOnePacket();
            TftpPacket checkingPacket;
            try {
                checkingPacket = TftpPacket.newTftpPacket(newPacketCheck);
                boolean canProcessPacketType = false;
                switch (checkingPacket.getType()) {
                    case TftpPacket.ERROR -> {
                        localLogger.simpleReport(
                                "TftpService - ERROR TFTP010: Attempted to check options negotiation,"
                                        + " and client gave failure",
                                true);
                        return -1;
                    }
                    case TftpPacket.ACKNOWLEDGEMENT -> {
                        newLastPort = checkingPacket.getPort();
                        canProcessPacketType = true;
                    }
                }
                if (!canProcessPacketType) {
                    localLogger.simpleReport("TftpService - ERROR TFTP020: Unknown packet type", true);
                    return -1;
                }
            } catch (final TftpPacketException e) {
                localLogger.simpleReport(
                        "TftpService - ERROR TFTP009: Attempted to check options negotiation,"
                                + " and failed to handle packet",
                        true);
                localLogger.simpleReport(e.toString(), true);
                return -1;
            }
        }
        return newLastPort;
    }

    private int getNextBlockNumber(long filePosition) {
        float rawPacketNumber = (float) filePosition / maxTransmitPacketSize;
        rawPacketNumber++;
        // Max we can have is 65535 so we need to remove that number of it
        double newPacket = Math.floor(rawPacketNumber / 65535);
        double result = rawPacketNumber - newPacket;
        return (int) result;
    }

    private boolean properPacketIsBeingAcked(TftpPacket tempData, long filePosition) {
        // If we have a 1000 byte file with a max transmit of 100 bytes, then our 10th
        // packet will finish the data.
        // Then we need to signal the end, thus we send a null packet.
        final int acking = ((TftpAckPacket) tempData).getBlockNumber();
        int blockNumber = getNextBlockNumber(filePosition);
        return blockNumber == acking;
    }
}
