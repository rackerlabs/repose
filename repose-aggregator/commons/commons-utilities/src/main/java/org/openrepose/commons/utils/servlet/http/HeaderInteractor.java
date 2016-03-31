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
 * An interface that defines all the operations with headers one might want to do.
 */
public interface HeaderInteractor {
    /**
     * Gets a list of names of all available headers
     *
     * @return the header names
     */
    List<String> getHeaderNamesList();

    /**
     * Gets a list of values associated with a given header. Will return one value per header.
     * Does not split values if splittable.
     *
     * @param headerName the name of the header to get values for
     * @return a list of values one per header entry
     */
    List<String> getHeadersList(String headerName);

    /**
     * Gets a list of values associated with a Header, and splits them on commas as necessary.
     *
     * @param headerName the name of the header to get values for
     * @return a flattened list of values with all of the first header's values appearing before the second, the second before the third, etc.
     */
    List<String> getSplittableHeaders(String headerName);

    /**
     * Returns the header value(s) with the highest quality. The default quality is 1.0 if unspecified.
     * This treats each header entry as if it's one value, should there be multiple values per header entry
     * exceptions are likely to occur.
     *
     * @param headerName the name of the header to get the preferred value for
     * @return the highest quality value(s)
     * @throws QualityFormatException when quantity exists but cannot be parsed
     */
    List<String> getPreferredHeaders(String headerName);

    /**
     * Returns the header value(s) with the highest quality, but will try to split all headers on commas before trying
     * to evaluate.
     * The default quality is 1.0 if unspecified.
     *
     * @param headerName the name of the header to get the preferred value for
     * @return the value(s) with the highest quality after headers have been split
     * @throws QualityFormatException when quantity is present but cannot be parsed
     */
    List<String> getPreferredSplittableHeaders(String headerName);

    /**
     * Returns the header value(s) with the highest quality. The default quality is 1.0 if unspecified.
     * This treats each header entry as if it's one value, should there be multiple values per header entry
     * exceptions are likely to occur.
     *
     * @param headerName the name of the header to get the preferred value for
     * @return the highest quality value(s) with parameters included
     * @throws QualityFormatException when quantity exists but cannot be parsed
     */
    List<String> getPreferredHeadersWithParameters(String headerName);

    /**
     * Returns the header value(s) with the highest quality, but will try to split all headers on commas before trying
     * to evaluate.
     * The default quality is 1.0 if unspecified.
     *
     * @param headerName the name of the header to get the preferred value for
     * @return the value(s) with the highest quality after headers have been split with parameters included
     * @throws QualityFormatException when quantity is present but cannot be parsed
     */
    List<String> getPreferredSplittableHeadersWithParameters(String headerName);

    /**
     * Adds the specified header with the header name and header value pair.
     * This creates a new header entry under the given name.
     *
     * @param headerName  the name of the header
     * @param headerValue the value of the header
     */
    void addHeader(String headerName, String headerValue);

    /**
     * Adds the specified header with the header name and header value pair.
     * The quality is appended to headerValue.
     * This creates a new header entry under the given name.
     *
     * @param headerName  the name of the header
     * @param headerValue the value of the header
     * @param quality     a double from 0.0 to 1.0
     */
    void addHeader(String headerName, String headerValue, double quality);

    /**
     * Replaces the specified header with the header name and header value pair.
     * This replaces all values of a header with that name.
     *
     * @param headerName  the name of the header
     * @param headerValue the value of the header
     */
    void replaceHeader(String headerName, String headerValue);

    /**
     * Replaces the specified header with the header name and header value pair.
     * The quality is appended to header value.
     * This replaces all values of a header with that name.
     *
     * @param headerName  the name of the header
     * @param headerValue the value of the header
     * @param quality     a double from 0.0 to 1.0
     */
    void replaceHeader(String headerName, String headerValue, double quality);

    /**
     * Appends header value to the end of the first header value for the specified header.
     * This is accomplished by adding a comma first, and then the new value.
     *
     * @param headerName  the name of the header
     * @param headerValue the value of the header
     */
    void appendHeader(String headerName, String headerValue);

    /**
     * Appends header value to the end of the first header value for the specified header.
     * The quality is appended to header value.
     * This is accomplished by adding a comma first, and then the new value.
     *
     * @param headerName  the name of the header
     * @param headerValue the value of the header
     * @param quality     a double from 0.0 to 1.0
     */
    void appendHeader(String headerName, String headerValue, double quality);

    /**
     * Removes the specified header and all associated values.
     *
     * @param headerName the name of the header
     */
    void removeHeader(String headerName);
}
