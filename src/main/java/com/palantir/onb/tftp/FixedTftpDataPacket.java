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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Hey look its just like TFTPDataPacket, except it's not dumb and doing the 1997 512 byte limit.
 * http://tools.ietf.org/html/rfc2348
 */
class FixedTftpDataPacket {
    private final DatagramPacket internalPacket;

    /**
     * Creates a raw packet out of tftp info.
     * @param sentToAddress sending address
     * @param sendPort port to send to
     * @param tftpBlockNumber ID of tftp block
     * @param data raw data
     */
    FixedTftpDataPacket(InetAddress sentToAddress, int sendPort, int tftpBlockNumber, byte[] data) {
        final ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(ByteBuffer.allocate(2).putShort((byte) 0x03).array());
        buffer.put(ByteBuffer.allocate(2).putShort((short) tftpBlockNumber).array());
        buffer.put(data);
        internalPacket = new DatagramPacket(buffer.array(), buffer.position());
        internalPacket.setPort(sendPort);
        internalPacket.setAddress(sentToAddress);
    }

    DatagramPacket getDatagram() {
        return internalPacket;
    }
}
