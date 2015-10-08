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

package org.openrepose.commons.utils.http;

public enum CorsHttpHeader implements HeaderConstant {
    ORIGIN("Origin"),
    ACCESS_CONTROL_REQUEST_METHOD("Access-Control-Request-Method"),
    ACCESS_CONTROL_REQUEST_HEADERS("Access-Control-Request-Headers"),
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin"),
    ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials"),
    ACCESS_CONTROL_EXPOSE_HEADERS("Access-Control-Expose-Headers"),
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods"),
    ACCESS_CONTROL_ALLOW_HEADERS("Access-Control-Allow-Headers");

    private final String headerKey;

    CorsHttpHeader(String headerKey) {
        this.headerKey = headerKey.toLowerCase();
    }

    @Override
    public String toString() {
        return headerKey;
    }

    @Override
    public boolean matches(String st) {
        return headerKey.equalsIgnoreCase(st);
    }
}
