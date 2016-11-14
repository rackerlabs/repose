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
package org.openrepose.filters.translation.httpx.processor.cdata;

import org.openrepose.commons.utils.io.InputStreamMerger;
import org.openrepose.filters.translation.httpx.processor.common.InputStreamProcessor;

import java.io.InputStream;

public class UnknownContentStreamProcessor implements InputStreamProcessor {

    private static final String UNKNOWN_PREFIX = "<httpx:unknown-content xmlns:httpx=\"http://docs.openrepose.org/repose/httpx/v1.0\"><![CDATA[";
    private static final String UNKNOWN_SUFFIX = "]]></httpx:unknown-content>";

    @Override
    public InputStream process(InputStream sourceStream) {
        // TODO better way to "wrap" unknown data in an xml tag?
        return InputStreamMerger.merge(
                InputStreamMerger.wrap(UNKNOWN_PREFIX),
                sourceStream,
                InputStreamMerger.wrap(UNKNOWN_SUFFIX));
    }
}
