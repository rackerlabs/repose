package com.rackspace.papi.service.httpclient.impl

import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.params.ClientPNames
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.params.CoreConnectionPNames
import org.junit.Before
import org.junit.Test

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ClientDecommissionerTest {

    ClientDecommissionManager decomm;
    HttpClient client;
    ServerSocket welcomeSocket1, welcomeSocket2;
    int socketPort;
    boolean listen;
    PoolingClientConnectionManager connMan
    URI uri1, uri2;



    @Before
    void setUp() {

        welcomeSocket1 = new ServerSocket(0);
        welcomeSocket2 = new ServerSocket(0);

        println("port1: " + welcomeSocket1.getLocalPort())
        println("port2: " + welcomeSocket2.getLocalPort())
        listen = true;

        decomm = new ClientDecommissionManager();

        connMan = new PoolingClientConnectionManager();
        connMan.setMaxTotal(10);
        connMan.setDefaultMaxPerRoute(10)


        client = new DefaultHttpClient(connMan);
        client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);


        uri1 = new URI("http://localhost:" + welcomeSocket1.getLocalPort() + "/blah");
        uri2 = new URI("http://localhost:" + welcomeSocket2.getLocalPort() + "/");

        decomm.startThread()

    }

    @Test
    void testAddClientToBeDecommissioned() {

        Thread serverThread = Thread.start {

            //Wait for connection
            Socket connSocket1 = welcomeSocket1.accept();

            //Connection Established
            try {
                BufferedReader inBound = new BufferedReader(new InputStreamReader(connSocket1.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(connSocket1.getOutputStream());

                String inputLine;
                while (!(inputLine = inBound.readLine()).equals(""))
                    System.out.println(inputLine);
                inBound.close();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                }

                PrintWriter writer = new PrintWriter(outToClient, true)
                String strresponse = "HTTP/1.1 200 OK"

                writer.write( strresponse)
                writer.write("\r\n")

                //headers
                writer.write( "server:myown\r\ncontent-length:6\r\ncontent-type:text/plain")

                writer.write("sample")
                outToClient.flush()
                outToClient.close()

            } finally {
                println("closing...")
                connSocket1.close()


                println("Socket1 shutdown? " + connSocket1.isClosed())
                println("Socket1 connected? " + connSocket1.isConnected())
            }
        }

        Thread serverThread2 = Thread.start {

            //Wait for connection
            Socket connSocket2 = welcomeSocket2.accept();

            //Connection Established
            BufferedReader inBound = new BufferedReader(new InputStreamReader(connSocket2.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connSocket2.getOutputStream());


            String inputLine;
            while (!(inputLine = inBound.readLine()).equals(""))
                System.out.println(inputLine);
            inBound.close();

            String strresponse = "HTTP/1.1 200 OK"
            PrintWriter writer = new PrintWriter(outToClient, true)

            writer.write( strresponse)
            writer.write("\r\n")

            //headers
            writer.write( "server:myown\r\ncontent-length:6\r\ncontent-type:text/plain")

            writer.write("sample")
            outToClient.flush()

            connSocket2.close()
        }

        assert connMan.getTotalStats().max == 10

        Thread getThread = Thread.start {
            HttpGet get = new HttpGet(uri1);
            HttpResponse rsp;

            try {

                get.addHeader("Request", "One")
                get.addHeader("host", "localhost:" + welcomeSocket1.getLocalPort())
                get.addHeader("connection", "close")

//                GetMethod get1 = new GetMethod("http://localhost:" + welcomeSocket1.getLocalPort() + "/")
//                get1.setFollowRedirects(true)

                rsp = client.execute(get);

            } catch (IOException ex) {
            } finally {
                get.releaseConnection()
            }

            Thread.currentThread().interrupt()
            return;
        }

        decomm.decommissionClient(client)

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
        }

        Thread get2 = Thread.start {

            HttpGet get = new HttpGet(uri2);
            get.addHeader("Request", "Two")

            HttpResponse response = client.execute(get);

        }

        //Makes sure that only one connection is established.
        assert connMan.getTotalStats().leased == 1
        assert connMan.getTotalStats().max == 1

        serverThread.interrupt()
        getThread.interrupt()

        serverThread2.interrupt()
        get2.interrupt()

        decomm.stopThread()

    }
}
