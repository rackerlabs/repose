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

    private PoolType poolType1, poolType2;
    private final static int CONN_TIMEOUT = 30000;
    private final static int MAX_HEADERS = 100;
    private final static int MAX_LINE = 50;
    private final static int SOC_TIMEOUT = 40000;
    private final static boolean TCP_NODELAY = false;
    private final static boolean TCP_NODELAY2 = true;
    private final static int SOC_BUFF_SZ = 1023;
    private final static int MAX_PER_ROUTE = 50;
    private final static int MAX_TOTAL = 300;


    HttpConnectionPoolServiceImpl httpClientService;
    HttpConnectionPoolConfig poolCfg;

    @Before
    void setUp() {

        poolType1 = new PoolType();
        poolType1.setHttpConnectionMaxHeaderCount(MAX_HEADERS);
        poolType1.setHttpConnectionMaxLineLength(MAX_LINE);
        poolType1.setHttpConnectionMaxStatusLineGarbage(10);
        poolType1.setHttpConnectionTimeout(CONN_TIMEOUT);
        poolType1.setHttpConnManagerMaxPerRoute(MAX_PER_ROUTE);
        poolType1.setHttpConnManagerMaxTotal(MAX_TOTAL);
        poolType1.setHttpSocketBufferSize(SOC_BUFF_SZ);
        poolType1.setHttpSocketTimeout(SOC_TIMEOUT);
        poolType1.setHttpTcpNodelay(TCP_NODELAY);
        poolType1.setId("pool1");

        poolType2 = new PoolType();
        poolType2.setHttpConnectionMaxHeaderCount(MAX_HEADERS);
        poolType2.setHttpConnectionMaxLineLength(MAX_LINE);
        poolType2.setHttpConnectionMaxStatusLineGarbage(10);
        poolType2.setHttpConnectionTimeout(CONN_TIMEOUT);
        poolType2.setHttpConnManagerMaxPerRoute(MAX_PER_ROUTE);
        poolType2.setHttpConnManagerMaxTotal(MAX_TOTAL);
        poolType2.setHttpSocketBufferSize(SOC_BUFF_SZ);
        poolType2.setHttpSocketTimeout(SOC_TIMEOUT);
        poolType2.setHttpTcpNodelay(TCP_NODELAY2);
        poolType2.setDefault(true);
        poolType2.setId("pool2");

        List<PoolType> pools = new ArrayList<PoolType>();

        pools.add(poolType2);
        pools.add(poolType1);

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

        URI uri1 = new URI("http://localhost:" + serverSocket.getLocalPort() + "/blah");
        Random rand = new Random()

        int totalErrors = 0
        List<Thread> clientThreads = new ArrayList<Thread>()

        for (x in 1..5) {
            println("Starting client: " + x)
            String threadName = "Thread:" + x

            Thread thread = Thread.start {

                for (y in 1..2) {
                    HttpClientResponse clientResponse = httpClientService.getClient("pool1")
                    HttpClient httpClient = clientResponse.getHttpClient();
                    HttpGet get
                    try {
                        get = new HttpGet(uri1);
                        get.addHeader("Thread", threadName)

                        println("STARTED Thread: " + threadName + " Call: " + y)
                        Thread.sleep(500 + rand.nextInt(3000))
                        HttpResponse rsp = httpClient.execute(get);
                        println("COMPLETED Thread: " + threadName + " Call: " + y)

                        if (rsp.getStatusLine().getStatusCode() != 200) {
                            totalErrors++
                        }
                    } catch (Exception e) {
                        println("Got an exception: " + e)
                        totalErrors++
                    } finally {
                        get.releaseConnection()
                        httpClientService.releaseClient(clientResponse)
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
        }

        // wait until all client threads have finished
        clientThreads*.join()

        // once all client threads are done, stop the reconfiguring
        keepReconfiguring = false
        reconfigureThread.join()

        assertEquals(0, totalErrors)
    }
}
