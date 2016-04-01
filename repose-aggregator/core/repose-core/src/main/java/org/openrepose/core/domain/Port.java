/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.domain;

public class Port {
    private static final int BASE_HASH = 3;
    private static final int PRIME = 71;
    private final String protocol;
    private final int port;

    public Port(String protocol, int port) {
        this.protocol = protocol;
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Port)) {
            return false;
        }

        Port p = (Port) other;

        if (protocol != null) {
            return port == p.getPort() && protocol.equalsIgnoreCase(p.getProtocol());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = BASE_HASH;
        hash = PRIME * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
        hash = PRIME * hash + this.port;
        return hash;
    }
}
