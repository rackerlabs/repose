package com.rackspace.papi.service.httpclient.impl
import com.rackspace.papi.service.httpclient.HttpClientResponse
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig
import com.rackspace.papi.service.httpclient.config.PoolType
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class MultiThreadHttpClientUserTest {

    HttpConnectionPoolServiceImpl httpClientService;
    HttpConnectionPoolConfig poolCfg;

    @Before
    void setUp() {
        List<PoolType> pools = PoolTypeHelper.createListOfPools(2, 2)

        poolCfg = new HttpConnectionPoolConfig();
        poolCfg.pool.addAll(pools);

        httpClientService = new HttpConnectionPoolServiceImpl();
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
                        println "server received: $buffer"
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
            println("Starting client: " + x)
            String threadName = "Thread:" + x

            Thread thread = Thread.start {

                for (y in 1..2) {
                    HttpClientResponse clientResponse = httpClientService.getClient("pool1")
                    HttpClient httpClient = clientResponse.getHttpClient();
                    HttpGet get
                    HttpResponse rsp
                    try {
                        get = new HttpGet(uri1);
                        get.addHeader("Thread", threadName)

                        println("STARTED Thread: " + threadName + " Call: " + y)
                        Thread.sleep(500 + rand.nextInt(3000))
                        rsp = httpClient.execute(get);
                        println("COMPLETED Thread: " + threadName + " Call: " + y)
                    } catch (Exception e) {
                        println("Client: " + clientResponse.clientInstanceId + " got an exception: " + e)
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
                            println("EXCEPTION ON RESPONSE: " + e)

                    }

                }
            }
            clientThreads.add(thread)
        }

        Boolean keepReconfiguring = true
        int reconfigureCount = 0

        Thread reconfigureThread = Thread.start {
            while (keepReconfiguring) {
                println("Reconfiguring...")
                sleep(500)
                httpClientService.configure(poolCfg)
                reconfigureCount++
            }
            println("STOPPED RECONFIGURING")
        }

        // wait until all client threads have finished
        clientThreads*.join()

        // once all client threads are done, stop the reconfiguring
        keepReconfiguring = false
        reconfigureThread.join()

        assertEquals(0, totalErrors)
    }
}
