package com.rackspace.papi.service.proxy.httpcomponent

import com.rackspace.papi.filter.SystemModelInterrogator
import com.rackspace.papi.service.config.impl.PowerApiConfigurationManager
import com.rackspace.papi.service.healthcheck.HealthCheckService
import com.rackspace.papi.service.httpclient.HttpClientResponse
import com.rackspace.papi.service.httpclient.HttpClientService
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPatch
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import spock.lang.Specification

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/22/14
 * Time: 2:46 PM
 */
class RequestProxyServiceImplTest extends Specification {
    RequestProxyServiceImpl requestProxyService
    HttpClient httpClient

    def setup() {
        httpClient = mock(HttpClient)
        HttpClientResponse httpClientResponse = mock(HttpClientResponse)
        when(httpClientResponse.getHttpClient()).thenReturn(httpClient)
        HttpClientService httpClientService = mock(HttpClientService)
        when(httpClientService.getClient(Mockito.any(String))).thenReturn(httpClientResponse)
        requestProxyService = new RequestProxyServiceImpl(mock(PowerApiConfigurationManager.class), mock(SystemModelInterrogator.class), mock(HealthCheckService.class))
        requestProxyService.setHttpClientService(httpClientService)
    }

    def "Send a patch request with expected body and headers and return expected response"() {
        given:
        StatusLine statusLine = mock(StatusLine)
        when(statusLine.getStatusCode()).thenReturn(418)
        HttpEntity httpEntity = mock(HttpEntity)
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream([1, 2, 3] as byte[]))
        HttpResponse httpResponse = mock(HttpResponse)
        when(httpResponse.getStatusLine()).thenReturn(statusLine)
        when(httpResponse.getEntity()).thenReturn(httpEntity)
        ArgumentCaptor<HttpPatch> captor = ArgumentCaptor.forClass(HttpPatch)
        when(httpClient.execute(captor.capture())).thenReturn(httpResponse)

        when:
        byte[] sentBytes = [4, 5, 6] as byte[]
        def response = requestProxyService.patch("http://www.google.com", "key", ["thing": "other thing"], sentBytes)
        def request = captor.getValue()
        byte[] readBytes =  new byte[3]
        request.getEntity().getContent().read(readBytes)
        byte[] returnedBytes = new byte[3]
        response.data.read(returnedBytes)

        then:
        request.getMethod() == "PATCH"
        request.getURI().toString() == "http://www.google.com/key"
        request.getHeaders("thing").first().value == "other thing"
        readBytes == sentBytes

        response.statusCode == 418
        returnedBytes == [1, 2, 3] as byte[]
    }
}
