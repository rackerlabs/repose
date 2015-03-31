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
package org.openrepose.external.testing.mocks

import org.junit.Test
import org.openrepose.commons.utils.test.mocks.HeaderList
import org.openrepose.commons.utils.test.mocks.NameValuePair
import org.openrepose.commons.utils.test.mocks.ObjectFactory
import org.openrepose.commons.utils.test.mocks.RequestInformation
import org.openrepose.commons.utils.test.mocks.util.RequestInfo

class RequestInfoTest {

    @Test
    void testGettingAllHeaders() {

        ObjectFactory factory = new ObjectFactory();
        RequestInformation req = factory.createRequestInformation();

        req.uri = "http://test.openrepose.org/path/to/resource"
        req.path = "/path/to/resource"
        req.method = "PATCH"
        req.body = "<some><body/>blah</some>"
        HeaderList headerList = factory.createHeaderList();
        List<NameValuePair> headers = new ArrayList<NameValuePair>()
        NameValuePair h1 = factory.createNameValuePair()
        h1.name = "accept"
        h1.value = "application/xml"
        NameValuePair h2 = factory.createNameValuePair()
        h2.name = "Accept"
        h2.value = "application/json"
        headers.add(h1)
        headers.add(h2)
        headerList.header = headers
        req.headers = headerList

        RequestInfo info = new RequestInfo(req)

        assert info.getHeaders().get("accept").size() == 2
        assert info.getHeaders().get("AccePt").size() == 2

    }

}
