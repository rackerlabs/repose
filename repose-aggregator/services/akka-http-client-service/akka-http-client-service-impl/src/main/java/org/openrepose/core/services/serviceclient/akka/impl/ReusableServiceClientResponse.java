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
package org.openrepose.core.services.serviceclient.akka.impl;


import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


public class ReusableServiceClientResponse extends ServiceClientResponse {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ReusableServiceClientResponse.class);
    private byte[] dataArray;

    public ReusableServiceClientResponse(int code, InputStream data) {
        super(code, data);

        try {
            dataArray = IOUtils.toByteArray(data);
        } catch (IOException e) {
            LOG.error("Not able read inputstream to byte array: " + e.getMessage(), e);
        }
    }

    public ReusableServiceClientResponse(int code, Header[] headers, InputStream data) {
        super(code, headers, data);

        try {
            dataArray = IOUtils.toByteArray(data);
        } catch (IOException e) {
            LOG.error("Not able read inputstream to byte array: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getData() {
        return new ByteArrayInputStream(dataArray);
    }


}
