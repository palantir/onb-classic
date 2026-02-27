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
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to handle a TftpService option ack.
 */
class TftpOptionAck {
    private final Map<String, byte[]> options;

    TftpOptionAck() {
        options = new LinkedHashMap<>();
    }

    void addOption(String optName, byte[] value) {
        options.put(optName, value);
    }

    final boolean hasOptions() {
        return options.entrySet().size() > 0;
    }

    @SuppressWarnings("DefaultCharset")
    DatagramPacket getDatagram() {
        int packetLength = 2;
        Iterator<Map.Entry<String, byte[]>> it = options.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, byte[]> item = it.next();
            packetLength += item.getKey().getBytes().length;
            packetLength += 1; // This is for the 0 in between
            packetLength += item.getValue().length;
            packetLength += 1; // This is for the 0 trailing
        }
        final ByteBuffer buffer = ByteBuffer.allocate(packetLength);

        buffer.put((byte) 0);
        buffer.put((byte) 0x06);
        it = options.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, byte[]> item = it.next();
            buffer.put(item.getKey().getBytes());
            buffer.put((byte) 0);
            buffer.put(item.getValue());
            buffer.put((byte) 0);
        }
        return new DatagramPacket(buffer.array(), buffer.array().length);
    }
}
