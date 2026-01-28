/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.onb.tftp.commons;

import java.io.Serial;

/**
 * A class used to signify the occurrence of an error in the creation of
 * a TFTP packet.  It is not declared final so that it may be subclassed
 * to identify more specific errors.  You would only want to do this if
 * you were building your own TFTP client or server on top of the
 * TFTP class if you wanted more functionality than the  receiveFile()
 * and sendFile() methods provide.
 *
 *
 * @see TftpPacket
 */

public class TftpPacketException extends Exception {

    @Serial
    private static final long serialVersionUID = -8114699256840851439L;

    /**
     * Simply calls the corresponding constructor of its superclass.
     */
    public TftpPacketException() {
    }

    /**
     * Simply calls the corresponding constructor of its superclass.
     * @param message the message
     */
    public TftpPacketException(final String message) {
        super(message);
    }
}
