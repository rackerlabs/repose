package features.filters.cors

import framework.ReposeValveTest
import org.apache.http.HttpResponse
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.apache.http.util.EntityUtils
import org.rackspace.deproxy.ClientConnector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.DeproxyHttpRequest
import org.rackspace.deproxy.Header
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.RequestParams
import org.rackspace.deproxy.Response
import spock.lang.Shared

import javax.servlet.http.HttpServletResponse
import java.nio.file.Files

class CorsSameOriginTest extends ReposeValveTest {

    @Shared
    int reposeSslPort

    @Shared
    String reposeSslEndpoint

    def setupSpec() {
        reposeLogSearch.cleanLog()

        reposeSslPort = PortFinder.Singleton.getNextOpenPort()
        reposeSslEndpoint = "https://$properties.targetHostname:$reposeSslPort"

        def params = properties.getDefaultTemplateParams() + [reposeSslPort: reposeSslPort]
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/cors/sameorigin", params)

        // have to manually copy binary files, because the applyConfigs() attempts to substitute template parameters
        // when they are found and it breaks everything. :(
        def serverFileOrig = new File(repose.configurationProvider.configTemplatesDir, "common/server.jks")
        def serverFileDest = new FileOutputStream(new File(repose.configDir, "server.jks"))
        Files.copy(serverFileOrig.toPath(), serverFileDest)

        // add self-signed SSL certificate support to Deproxy
        CloseableHttpClient client = HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(
                        SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build(),
                        NoopHostnameVerifier.INSTANCE))
                .build()
        ClientConnector sslConnector = { Request request, boolean https, def host, def port, RequestParams requestParams ->
            def scheme = https ? 'https' : 'http'
            def deproxyRequest = new DeproxyHttpRequest(request, scheme, host as String, port)

            HttpResponse response = client.execute(deproxyRequest)

            def body
            if (response.entity.contentType != null &&
                    response.entity.contentType.value.toLowerCase().startsWith("text/")) {
                body = EntityUtils.toString(response.getEntity())
            } else {
                body = EntityUtils.toByteArray(response.getEntity())
            }

            new Response(response.statusLine.statusCode,
                    response.statusLine.reasonPhrase,
                    response.getAllHeaders().collect { new Header(it.getName(), it.getValue()) },
                    body)
        }

        deproxy = new Deproxy(null, sslConnector)
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "Can execute a simple request via SSL using Deproxy"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeSslEndpoint, method: "GET")

        then:
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK
    }

    def "Can execute a simple request using Deproxy"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET")

        then:
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK
    }
}
