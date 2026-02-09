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
 * Each PxeService interaction has to have certain rules so that a proper return packet can be generated,
 * this type handles that return data.
 */
public class PxeOptions {
    private byte[] thisServerIp = new byte[4];

    /**
     * The PxeService dhcp packet will say which server is responding, that data is put in here by teh rule
     * maker.
     * @return Server IP
     */
    public byte[] getThisServerIp() {
        return thisServerIp;
    }

    /**
     * Set the current servers IP for packets.
     * @param thisServerIp server IP in bytes
     */
    public void setThisServerIp(byte[] thisServerIp) {
        this.thisServerIp = thisServerIp;
    }

    private byte[] tftpServerIp = new byte[4];

    /**
     * Location of the initial TftpService server.
     * @return Tftp server IP
     */
    public byte[] getTftpServerIp() {
        return tftpServerIp;
    }

    /**
     * Set Tftp server ip address.
     * @param tftpServerIp ip in bytes
     */
    public void setTftpServerIp(byte[] tftpServerIp) {
        this.tftpServerIp = tftpServerIp;
    }

    private String serverName = "";

    /**
     * Rule maker enters the name of the system here.
     * @return server name as string
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Set the server name.
     * @param serverName server name string
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    private String bootFile = "";

    /**
     * Which file should be specified.
     * @return filename
     */
    public String getBootFile() {
        return bootFile;
    }

    /**
     * Set the transaction filename.
     * @param bootFile filename
     */
    public void setBootFile(String bootFile) {
        this.bootFile = bootFile;
    }
}
