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
package org.openrepose.commons.utils.http.header;


import java.util.*;

public class SplittableHeaderUtil {

    public static final Comparator<String> CASE_INSENSITIVE_COMPARE = Comparator.comparing(String::toLowerCase);
    // Headers available for splitting (According to RFC2616
    private static final String[] DEFAULT_SPLIT = {"accept", "accept-charset", "accept-language", "allow",
            "cache-control", "connection", "content-encoding", "content-language", "expect", "pragma",
            "proxy-authenticate", "te", "trailer", "transfer-encoding", "upgrade",
            "warning", "accept-encoding"};
    private Set<String> splittableHeaders;

    public SplittableHeaderUtil() {
        setDefaultSplittable();
    }

    public SplittableHeaderUtil(String... constant) {
        setDefaultSplittable();

        Collections.addAll(splittableHeaders, constant);
    }

    public SplittableHeaderUtil(String[]... constant) {
        setDefaultSplittable();

        for (String[] cts : constant) {
            Collections.addAll(splittableHeaders, cts);
        }
    }

    private void setDefaultSplittable() {
        // Using a set which us so as to pass a comparator
        splittableHeaders = new TreeSet<>(CASE_INSENSITIVE_COMPARE);
        splittableHeaders.addAll(Arrays.asList(DEFAULT_SPLIT));
    }

    public boolean isSplittable(String st) {
        return splittableHeaders.contains(st.toLowerCase());
    }
}
