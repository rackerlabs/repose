package features.services.httpconnectionpool

import framework.ReposeValveTest
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.DefaultHandler
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletHandler
import org.rackspace.deproxy.Deproxy

import javax.servlet.ServletException
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HttpClientStateTest extends ReposeValveTest {

    Server server = null;

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        server?.stop()
    }

    def "the connection pool should not persist cookies between clients"() {
        given:

        //Stand up a stupid jetty that can do stuff with a proxy
        server = new Server(0)
        //We should always get a new session here, maybe check the debug logging, otherwise we might have to reach in somehow
        HandlerList handlers = new HandlerList()

        SessionHandler handler = new SessionHandler()
        ServletHandler servlet = new ServletHandler()
        servlet.addServletWithMapping(SimpleServlet.class, "/*")

        DefaultHandler defaultHandler = new DefaultHandler()
        handlers.setHandlers([handler, servlet].toArray() as Handler[])
        server.setHandler(handlers)

        server.start()

        int jettyPort = ((ServerConnector) server.getConnectors()[0]).getLocalPort()

        deproxy = new Deproxy()
        //Don't have deproxy do anything on our jetty port, we've got that covered
        //deproxy.addEndpoint(jettyPort)

        properties.targetPort = jettyPort //OVERRIDE with mine!
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/smallpool", params)
        repose.start()

        waitUntilReadyToServiceRequests()

        //Make a pool, use that pool to do owrk, make sure the sessions are always different!


        def sessionIds = new CopyOnWriteArrayList<String>()
        when:

        //Do it as a thread pool, in order to perhaps exercise the clients...
        def pool = Executors.newFixedThreadPool(20)
        def defer = { c -> pool.submit(c as Callable) }
        //TODO: make a pile of requests, and for all of them each one should have a different session, not one should be reused
        (1..1000).each { count ->
            defer {
                HttpClient client = new DefaultHttpClient()
                def response = client.execute(new HttpGet("http://localhost:${properties.reposePort}/"))
                String content = response.getEntity().getContent().getText()
                sessionIds.add(content) //Just store it for later verification

                //Make one more request using this client, to see if we get the same session id
                def response2 = client.execute(new HttpGet("http://localhost:${properties.reposePort}/"))
                String content2 = response2.getEntity().getContent().getText()

                //TODO: need to check this somehow in the spock assertions
                assert content.equals(content2)

                //Lets not use deproxy this time
//                MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/", headers: [
//                        'x-trace-request': 'true',
//                        'x-count-thingy': count,
//                ]])
            }
        }

        pool.shutdown()
        pool.awaitTermination(100, TimeUnit.SECONDS)

        then: "something?"
        //TODO need to assert that it's always getting a new session
        Set<String> sessionIdSet = new HashSet<>()
        sessionIds.each { id ->
            assert sessionIdSet.add(id)
        }

    }


    public static class SimpleServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            //Get the session in here, and do something to make sure it's always different
            HttpSession session = req.getSession()

            def cookies = req.getCookies()

            def value = session.getAttribute("KEY")
            println("Session ID: ${session.getId()} HEADER COUNT: ${req.getHeader("x-count-thingy")} KEY:${value} cookies: ${cookies}")
            session.setAttribute("KEY", "VALUE")
            def cookie = new Cookie("TESTCOOKIE", "TEH VALUE")
            resp.addCookie(cookie)

            resp.setContentType("text/plain")
            resp.setStatus(HttpServletResponse.SC_OK)
            resp.getWriter().println(session.getId())
        }
    }

}
