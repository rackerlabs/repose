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
package org.openrepose.core.services.httpclient.impl

import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.junit.Before
import org.junit.Test
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig
import org.openrepose.core.service.httpclient.config.PoolType
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.httpclient.HttpClientContainer

import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.mock

class MultiThreadHttpClientUserTest {

    HttpConnectionPoolServiceImpl httpClientService;
    HttpConnectionPoolConfig poolCfg;

    @Before
    void setUp() {
        List<PoolType> pools = PoolTypeHelper.createListOfPools(2, 2)

        poolCfg = new HttpConnectionPoolConfig();
        poolCfg.pool.addAll(pools);

        httpClientService = new HttpConnectionPoolServiceImpl(mock(ConfigurationService.class), mock(HealthCheckService.class), "");
        httpClientService.configure(poolCfg);
    }

    @Test
    void whenMultipleThreadsConnectAndServiceReconfigsAllClientsCanStillBeUsed() {

        // Create a socket to listen for connections and respond back with 200 OK
        ServerSocket serverSocket = new ServerSocket(0);
        Thread.start {
            while (true) {
                serverSocket.accept { socket ->
                    socket.withStreams { input, output ->
                        def reader = input.newReader()
                        def buffer = reader.readLine()
                        output << "HTTP/1.1 200 OK\r\n"
                        output << "server:unittest\r\ncontent-length:6\r\ncontent-type:text/plain"
                        output << "status"
                    }
                }
            }
        }

        URI uri1 = new URI("http://localhost:" + serverSocket.getLocalPort());
        Random rand = new Random()

        int totalErrors = 0
        List<Thread> clientThreads = new ArrayList<Thread>()

        for (x in 1..50) {
            String threadName = "Thread:" + x

            Thread thread = Thread.start {

                for (y in 1..2) {
                    HttpClientContainer clientResponse = httpClientService.getClient("pool1")
                    HttpClient httpClient = clientResponse.getHttpClient();
                    HttpGet get
                    HttpResponse rsp
                    try {
                        get = new HttpGet(uri1);
                        get.addHeader("Thread", threadName)
                        Thread.sleep(500 + rand.nextInt(3000))
                        rsp = httpClient.execute(get);
                    } catch (Exception e) {
                        totalErrors++
                    } finally {
                        get.releaseConnection()
                        httpClientService.releaseClient(clientResponse)
                    }

                    try {
                        if (rsp != null && rsp.getStatusLine().getStatusCode() != 200) {
                            totalErrors++
                        }
                    } catch (Exception e) {
                        totalErrors++
                    }
                }
            }
            clientThreads.add(thread)
        }

        Boolean keepReconfiguring = true
        int reconfigureCount = 0

        Thread reconfigureThread = Thread.start {
            while (keepReconfiguring) {
                sleep(500)
                httpClientService.configure(poolCfg)
                reconfigureCount++
            }
        }

        // wait until all client threads have finished
        clientThreads*.join()

        // once all client threads are done, stop the reconfiguring
        keepReconfiguring = false
        reconfigureThread.join()

        assertEquals(0, totalErrors)
    }
}
