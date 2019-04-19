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
package org.openrepose.filters.ratelimiting.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author fran
 */
public class LimitsEntityStreamTransformerTest {

    private static final String XML_LIMITS = "<limits xmlns=\"http://docs.openstack.org/common/api/v1.0\">" +
            "<rates>" +
            "<rate uri=\"/v1.0/*\" regex=\"^/1.0/.*\">" +
            "<limit verb=\"GET\" value=\"600000\" remaining=\"426852\" unit=\"HOUR\" next-available=\"2011-02-22T19:32:43.835Z\"/>" +
            "</rate>" +
            "</rates>" +
            "</limits>";

    private final ByteArrayInputStream inputStream = new ByteArrayInputStream(XML_LIMITS.getBytes());

    private LimitsEntityStreamTransformer transformer = new LimitsEntityStreamTransformer();

    @Test
    public void shouldStreamOpenStackFormat() {
        final String JSON_LIMITS = "{\"limits\" : {\"rate\" : [{\"uri\" : \"/v1.0/*\",\"regex\" : \"^/1.0/.*\"," +
                "\"limit\" : [{\"verb\" : \"GET\",\"value\" : 600000,\"remaining\" : 426852,\"unit\" : \"HOUR\"," +
                "\"next-available\" : \"2011-02-22T19:32:43.835Z\"}]}]}}";


        final OutputStream outputStream = new ByteArrayOutputStream();
        transformer.streamAsJson(inputStream, outputStream);

        assertEquals(JSON_LIMITS.replaceAll("\\s", ""), outputStream.toString().replaceAll("\\s", ""));
    }
}
