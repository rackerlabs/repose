/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.commons.utils.servlet.http;

import org.openrepose.commons.utils.http.header.HeaderValue;
import java.util.Enumeration;
import java.util.List;

public interface HeaderValues {

    void addHeader(String name, String value);

    void addDateHeader(String name, long value);
    
    String getHeader(String name);
    
    HeaderValue getHeaderValue(String name);

    Enumeration<String> getHeaderNames();

    Enumeration<String> getHeaders(String name);
    
    List<HeaderValue> getHeaderValues(String name);

    List<HeaderValue> getPreferredHeaders(String name, HeaderValue defaultValue);

    List<HeaderValue> getPreferredHeaderValues(String name, HeaderValue defaultValue);

    void removeHeader(String name);

    void replaceHeader(String name, String value);
    
    void replaceDateHeader(String name, long value);
    
    void clearHeaders();
    
    boolean containsHeader(String name);

}
