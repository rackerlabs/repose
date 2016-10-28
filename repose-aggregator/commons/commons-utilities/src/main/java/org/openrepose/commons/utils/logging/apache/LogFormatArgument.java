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
package org.openrepose.commons.utils.logging.apache;

import java.util.Map;
import java.util.TreeMap;

public enum LogFormatArgument {

    PERCENT("%"),
    REMOTE_ADDRESS("a"),
    LOCAL_ADDRESS("A"),
    RESPONSE_CLF_BYTES("b"),
    RESPONSE_BYTES("B"),
    REMOTE_HOST("h"),
    REQUEST_METHOD("m"),
    CANONICAL_PORT("p"),
    QUERY_STRING("q"),
    TIME_RECEIVED("t"),
    STATUS_CODE("s"),
    REMOTE_USER("u"),
    URL_REQUESTED("U"),
    TRACE_GUID("g"),
    REQUEST_HEADER("i"),
    REQUEST_LINE("r"),
    REQUEST_PROTOCOL("H"),
    RESPONSE_HEADER("o"),
    STRING("STRING"),
    RESPONSE_REASON("M"),
    RESPONSE_TIME_MICROSECONDS("D"),
    RESPONSE_TIME_SECONDS("T");

    private final String argument;

    LogFormatArgument(String argument) {
        this.argument = argument;

        ReverseLookup.addLookup(argument, this);
    }

    public static LogFormatArgument fromString(String st) {
        return ReverseLookup.LOOKUP_MAP.get(st);
    }

    @Override
    public String toString() {
        return argument;
    }

    private static final class ReverseLookup {

        public static final Map<String, LogFormatArgument> LOOKUP_MAP = new TreeMap<>();

        private ReverseLookup() {
        }

        public static void addLookup(String st, LogFormatArgument arg) {
            LOOKUP_MAP.put(st, arg);
        }
    }
}
