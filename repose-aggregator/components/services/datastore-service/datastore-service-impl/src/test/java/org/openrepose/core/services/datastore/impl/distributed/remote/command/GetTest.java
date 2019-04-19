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
package org.openrepose.core.services.datastore.impl.distributed.remote.command;

import org.junit.Assert;
import org.junit.Test;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.impl.distributed.CacheRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetTest {
    //Since we're serializing objects here for the dist datastore, we must have the dist datastore objects in our classpath
    static final ObjectSerializer objectSerializer = new ObjectSerializer(GetTest.class.getClassLoader());

    @Test
    public void shouldTargetCorrectDeletionUrl() throws UnknownHostException {
        final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);

        Assert.assertEquals("Get command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + "object-key", getCommand.getUrl());
    }

    @Test
    public void shouldReturnTrueOnSuccess() throws Exception {
        final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);

        // RemoteBehavior.ALLOW_FORWARDING
        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        final String responseData = "Response Data";

        ByteArrayInputStream bt = new ByteArrayInputStream(objectSerializer.writeObject(responseData));

        when(response.getData()).thenReturn(bt);
        when(response.getStatus()).thenReturn(200);

        assertThat((String) getCommand.handleResponse(response), equalTo(responseData));
    }

    @Test(expected = DatastoreOperationException.class)
    public void shouldThrowExeptionOnUnauthorized() throws Exception {
        final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), null, false);

        final ServiceClientResponse response = mock(ServiceClientResponse.class);
        when(response.getStatus()).thenReturn(HttpServletResponse.SC_UNAUTHORIZED);

        getCommand.handleResponse(response);
    }
}
