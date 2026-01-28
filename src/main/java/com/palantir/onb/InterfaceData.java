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

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * Collection of functions for interfaces.
 */
public final class InterfaceData {

    private InterfaceData() {
    }

    /**
     * Output list of interfaces to the console.
     */
    static void printInterfaces() {
        try {
            final Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = interfaces.nextElement();
                Manager.getManagerLogger().simpleReport(
                        "Interface \"" + networkInterface.getDisplayName() + "\" - "
                                + networkInterface.getName() + " - Online:"
                                + networkInterface.isUp(), false);
                final List<InterfaceAddress> listingAddress =
                        networkInterface.getInterfaceAddresses();
                for (final InterfaceAddress singleAddress : listingAddress) {
                    Manager.getManagerLogger().simpleReport("\t" + singleAddress.getAddress().getHostAddress(),
                            false);
                }
            }
        } catch (final SocketException e) {
            Manager.getManagerLogger().simpleReport(e.toString(), true);
        }
    }

    /**
     * Get an interface name and make sure it's still valid.
     * @param intString interface name
     * @return exists
     */
    public static boolean verifyNics(String intString) {
        if (intString.contains("-")) {
            Manager.getManagerLogger().simpleReport("It appears a option was given instead of "
                    + "a list of interfaces?", true);
            return false;
        }
        final List<String> singleInterfaces = new ArrayList<>(Arrays.asList(intString.split(",")));
        if (singleInterfaces.isEmpty()) {
            Manager.getManagerLogger().simpleReport("0 Interfaces found in list", false);
            return false;
        }
        try {
            final Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = interfaces.nextElement();
                singleInterfaces.remove(networkInterface.getName());
            }
        } catch (final SocketException e) {
            Manager.getManagerLogger().simpleReport(e.toString(), true);
        }

        if (singleInterfaces.isEmpty()) {
            return true;
        } else {
            Manager.getManagerLogger().simpleReport("Error Finding Interfaces:", true);
            for (final String singleInterface : singleInterfaces) {
                Manager.getManagerLogger().simpleReport("\t" + singleInterface, true);
            }
            return false;
        }
    }

    /**
     * Get interfaces available in an String array.
     * @return Array of strings of interfaces
     */
    public static String[] getInterfaces() {
        List<String> ints = new ArrayList<>();
        try {
            final Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = interfaces.nextElement();
                ints.add(networkInterface.getName());
            }
        } catch (final Throwable e) {
            Manager.getManagerLogger().simpleReport(e.toString(), true);
        }
        return ints.toArray(new String[0]);
    }
}
