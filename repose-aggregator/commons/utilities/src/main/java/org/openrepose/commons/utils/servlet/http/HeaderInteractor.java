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
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 4/27/15
 * Time: 10:31 AM
 */
public interface HeaderInteractor {
    List<String> getHeaderList(String headerName);
    List<String> getSplittableHeader(String headerName);
    String getPrefferedHeader(String headerName);
    String getPreffereedSplittableHeader(String headerName);
    void addHeader(String headerName, String headerValue);
    void addHeader(String headerName, String headerValue, double quality);
    void replaceHeader(String headerName, String headerValue);
    void replaceHeader(String headerName, String headerValue, double quality);
    void appendHeader(String headerName, String headerValue);
    void appendHeader(String headerName, String headerValue, double quality);
    void removeHeader(String headerName);
}
