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

/**
 * The program (and iPXE support) is driven by rules of which different macs/clientIDs boot to
 * different servers, this is a single rule.
 */
public class BootRule {
    // Client ID, when the PxeService systems starts it will give us a generic PXEClient or iPXE, or maybe
    // newer
    private String client; //Filter
    // This is this server responding
    private String serverIp; //Output from filter
    // TftpService address to get boot file
    private String tftpIp; //Output from filter
    // Which file to load
    private String bootFile; //Output from filter
    // If we are filtering by hardware address, the address goes here
    private String hardwareAddress; //Filer
    // hardware type to boot referenced in https://tools.ietf.org/html/rfc4578
    private int[] hardwareTypes; //filter

    public BootRule() {
        client = "";
        serverIp = "";
        tftpIp = "";
        bootFile = "";
        hardwareAddress = "";
        hardwareTypes = new int[0];
    }

    /**
     * Check if this rule is empty.
     * @return if the rule is empty or not
     */
    public boolean emptyRule() {
        return client.isEmpty() && serverIp.isEmpty() && tftpIp.isEmpty()
                && bootFile.isEmpty() && hardwareAddress.isEmpty() && hardwareTypes.length == 0;
    }

    /**
     * Get the client ID of the boot rule.
     * @return string of the client ID
     */
    public String getClient() {
        return client;
    }

    /**
     * Set the client ID of this rule.
     * @param client setting client ID
     */
    public void setClient(String client) {
        this.client = client;
    }

    /**
     * Get the server IP to send a client matching this rule to.
     * @return String of the server IP
     */
    public String getServerIp() {
        return serverIp;
    }

    /**
     * Set a override server IP.
     * @param serverIp server IP
     */
    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    /**
     * Get the IP of the TFTP server.
     * @return String of the IP of the TFTP server
     */
    public String getTftpIp() {
        return tftpIp;
    }

    /**
     * Set a TFTP server to use when this rule is triggered.
     * @param tftpIp string of hte IP of a server
     */
    public void setTftpIp(String tftpIp) {
        this.tftpIp = tftpIp;
    }

    /**
     * Get the boot file that goes with this rule.
     * @return the boot file as a string, relative to the TFTP folders root
     */
    public String getBootFile() {
        return bootFile;
    }

    /**
     * Set a file to be used with this rule.
     * @param bootFile filename relative to the root of TftpBoot folder
     */
    public void setBootFile(String bootFile) {
        this.bootFile = bootFile;
    }

    /**
     * String of the mac address to match on.
     * @return get the mac address
     */
    public String getHardwareAddress() {
        return hardwareAddress;
    }

    /**
     * Set the mac address to match on.
     * @param hardwareAddress setting mac address
     */
    public void setHardwareAddress(String hardwareAddress) {
        this.hardwareAddress = hardwareAddress;
    }

    /**
     * Hardware types are a array of integers, these are the boot system a system booting is using.
     * This is mostly used for detecting EFI vs BIOS
     *
     * The arch numbers correspond to RFC4578.
     *   Type - Architecture Name
     *    0 - Intel x86PC
     *    1 - NEC/PC98
     *    2 - EFI Itanium
     *    3 - DEC Alpha
     *    4 - Arc x86
     *    5 - Intel Lean Client
     *    6 - EFI IA32
     *    7 - EFI BC
     *    8 - EFI Xscale
     *    9 - EFI x86-64
     *
     * @return The array of hardware types to trigger on
     */
    public int[] getHardwareTypes() {
        return hardwareTypes;
    }

    /**
     * Set a array of hardware types to use.
     * @param hardwareTypes The hardware array
     */
    public void setHardwareTypes(int[] hardwareTypes) {
        this.hardwareTypes = hardwareTypes;
    }
}
