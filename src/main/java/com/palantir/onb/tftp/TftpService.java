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

import com.palantir.onb.LogStandard;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * Runnable for the TftpService service. This is the main service that then contains interactive runners.
 */
public class TftpService implements Runnable {
    private final TftpSettings settingsFile;
    private final LogStandard localLogger;

    /**
     * Entry point for tftp server.
     * @param passedSettings settings to run server
     * @param passedLogger where to log
     */
    public TftpService(TftpSettings passedSettings, LogStandard passedLogger) {
        settingsFile = passedSettings;
        localLogger = passedLogger;
    }

    /**
     * Looping runnable.
     */
    @Override
    public void run() {
        boolean closeApp = false;
        localLogger.simpleReport("TftpService - Listener Starting on Port 69. Responding on all interfaces.", false);
        try (DatagramSocket udpSocket = new DatagramSocket(69)) {
            // TODO(#4): Limit interfaces on this service
            udpSocket.setSoTimeout(1000);
            while (!closeApp) {
                final byte[] buf = new byte[1024];
                final DatagramPacket rawPacket = new DatagramPacket(buf, buf.length);
                try {
                    udpSocket.receive(rawPacket);
                    rawPacket.setData(Arrays.copyOf(rawPacket.getData(), rawPacket.getLength()));
                    final Runnable temp = new InteractionRunner(rawPacket, settingsFile, localLogger);
                    final Thread tempStarterThread = new Thread(temp);
                    tempStarterThread.setName("TftpService File Handler Thread");
                    tempStarterThread.start();
                } catch (final SocketTimeoutException e) {
                    // This happens all the time, I dont care
                } catch (final IOException e) {
                    // This happened because we failed to get a packet
                    localLogger.simpleReport(e.toString(), true);
                }
                if (Thread.currentThread().isInterrupted()) {
                    closeApp = true;
                }
            }
            if (!udpSocket.isClosed()) {
                udpSocket.close();
            }
        } catch (final SocketException e1) {
            localLogger.simpleReport(
                    "TftpService - ERROR TFTP017: Attempted to create core TftpService server, and failed", true);
            localLogger.simpleReport(e1.toString(), true);
        }
        localLogger.simpleReport("Stopping Thread on 69", false);
    }
}
