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
package org.openrepose.commons.utils.servlet.http;

import java.util.List;

/**
 * An interface for a HttpServletRequestWrapper. It allows the user to
 * edit the HTTP Request Header by adding custom headers.
 */
public interface HeaderInteractor {
    /**
     * Gets a list of names for Request Headers
     *
     * @return
     */
    List<String> getHeaderNamesList();

    /**
     * Gets a list of values associated with a Request Header.
     * <p/>
     * Does not split values if splittable.
     *
     * @param headerName
     * @return
     */
    List<String> getHeadersList(String headerName);

    /**
     * Gets a list of values associated with a Request Header, and splits them if necessary.
     *
     * @param headerName
     * @return
     */
    List<String> getSplittableHeader(String headerName);

    /**
     * Returns the headerValue with the highest quality. The default quality is 1.0.
     *
     * @param headerName
     * @return
     * @throws QualityFormatException when quantity cannot be parsed
     */
    String getPreferredHeader(String headerName);

    /**
     * Returns the headerValue with the highest quality. The default quality is 1.0.
     *
     * @param headerName
     * @return
     * @throws QualityFormatException when quantity cannot be parsed
     */
    String getPreferredSplittableHeader(String headerName);

    /**
     * Adds the specified Request Header with the headerName and headerValue pair.
     *
     * @param headerName
     * @param headerValue
     */
    void addHeader(String headerName, String headerValue);

    /**
     * Adds the specified Request Header with the headerName and headerValue pair.
     * <p/>
     * The quality is appended to headerValue.
     *
     * @param headerName
     * @param headerValue
     * @param quality a double from 0.0 to 1.0
     */
    void addHeader(String headerName, String headerValue, double quality);

    /**
     * Replaces the specified Request Header with the headerName and headerValue pair.
     *
     * @param headerName
     * @param headerValue
     */
    void replaceHeader(String headerName, String headerValue);

    /**
     * Replaces the specified Request Header with the headerName and headerValue pair.
     * <p/>
     * The quality is appended to headerValue.
     *
     * @param headerName
     * @param headerValue
     * @param quality a double from 0.0 to 1.0
     */
    void replaceHeader(String headerName, String headerValue, double quality);

    /**
     * Appends headerValue to the end of the first header line for the specified Request Header.
     *
     * @param headerName
     * @param headerValue
     */
    void appendHeader(String headerName, String headerValue);

    /**
     * Appends headerValue to the end of the first header line for the specified Request Header.
     * <p/>
     * The quality is appended to headerValue.
     *
     * @param headerName
     * @param headerValue
     * @param quality a double from 0.0 to 1.0
     */
    void appendHeader(String headerName, String headerValue, double quality);

    /**
     * Removes the specified Request Header and all associated values.
     *
     * @param headerName
     */
    void removeHeader(String headerName);
}
