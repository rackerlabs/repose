/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.rackspace.papi.commons.util.logging.apache;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * 
 */
public enum LogFormatArgument {

    PERCENT("%"),
    REMOTE_ADDRESS("a"),
    LOCAL_ADDRESS("A"),
    RESPONSE_BYTES("b"),
    REMOTE_HOST("h"),
    REQUEST_METHOD("m"),
    CANONICAL_PORT("p"),
    QUERY_STRING("q"),
    TIME_RECIEVED("t"),
    STATUS_CODE("s"),
    REMOTE_USER("u"),
    URL_REQUESTED("U");

    public static LogFormatArgument fromString(String st) {
        return ReverseLookup.LOOKUP_MAP.get(st);
    }
    private final String argument;

    private LogFormatArgument(String argument) {
        this.argument = argument;

        ReverseLookup.addLookup(argument, this);
    }

    @Override
    public String toString() {
        return argument;
    }

    private final static class ReverseLookup {

        public static final Map<String, LogFormatArgument> LOOKUP_MAP = new TreeMap<String, LogFormatArgument>();

        private ReverseLookup() {
        }

        public static void addLookup(String st, LogFormatArgument arg) {
            LOOKUP_MAP.put(st, arg);
        }
    }
}
