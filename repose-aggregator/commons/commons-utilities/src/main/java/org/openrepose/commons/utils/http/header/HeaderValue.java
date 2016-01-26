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

import java.util.Map;

/**
 * @author zinic
 */
public interface HeaderValue extends Comparable<HeaderValue> {

    /**
     * Gets the string that represents the header value.
     *
     * @return
     */
    String getValue();

    /**
     * Gets a map of string keyed string parameters associated with this header value.
     *
     * @return
     */
    Map<String, String> getParameters();

    /**
     * Helper method for easily looking up a header value's quality factor, if it
     * exists.
     *
     * @return
     */
    double getQualityFactor();

    /**
     * A HeaderValue toString method must return a correctly formatted string
     * representation of the header value and its associated parameters.
     * <br /><br />
     * Format Example: <strong>"value;parameter=1;other_parameter=2"</strong>
     *
     * @return
     */
    @Override
    String toString();
}
