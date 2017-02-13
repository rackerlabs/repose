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

public class LogFormatArgument {
    public static final String PERCENT = "%";
    public static final String REMOTE_ADDRESS = "a";
    public static final String LOCAL_ADDRESS = "A";
    public static final String RESPONSE_CLF_BYTES = "b";
    public static final String RESPONSE_BYTES = "B";
    public static final String REMOTE_HOST = "h";
    public static final String REQUEST_METHOD = "m";
    public static final String CANONICAL_PORT = "p";
    public static final String QUERY_STRING = "q";
    public static final String TIME_RECEIVED = "t";
    public static final String STATUS_CODE = "s";
    public static final String REMOTE_USER = "u";
    public static final String URL_REQUESTED = "U";
    public static final String TRACE_GUID = "g";
    public static final String REQUEST_HEADER = "i";
    public static final String REQUEST_LINE = "r";
    public static final String REQUEST_PROTOCOL = "H";
    public static final String RESPONSE_HEADER = "o";
    public static final String STRING = "STRING";
    public static final String RESPONSE_REASON = "M";
    public static final String RESPONSE_TIME_MICROSECONDS = "D";
    public static final String RESPONSE_TIME_SECONDS = "T";

    private LogFormatArgument() {
        // This class should not be instantiated.
    }
}
