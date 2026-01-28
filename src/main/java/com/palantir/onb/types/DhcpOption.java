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
 * In each dhcp packet there are options, like modules, this is a type to contain a single module.
 * http://tools.ietf.org/html/rfc2132
 */
public class DhcpOption {
    // A byte for the option number, 0 to 255 (255 is a end signal)
    private byte option = 0x00;
    // How long will the following data be
    private byte length = 0x00;
    // A byte array of the data payload
    @SuppressWarnings("for-rollout:NullAway")
    private byte[] payload;

    /**
     * Get option code.
     * @return option byte
     */
    public byte getOption() {
        return option;
    }

    /**
     * Set the option data.
     * @param option byte of option data
     */
    public void setOption(byte option) {
        this.option = option;
    }

    /**
     * Length of the data attached according to the packet.
     * @return length of data
     */
    public byte getLength() {
        return length;
    }

    /**
     * Set length of data.
     * @param length of data in payload according to the packet
     */
    public void setLength(byte length) {
        this.length = length;
    }

    /**
     * Get payload data.
     * @return Payload
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Set payload data.
     * @param payload set payload data
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
}
