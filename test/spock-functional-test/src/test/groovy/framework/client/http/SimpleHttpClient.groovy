package framework.client.http

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.*
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient

import java.nio.charset.Charset

class SimpleHttpClient {

    List<RequestState> previousRequests = new ArrayList()

    def String endpoint = ""

    def String name = "SimpleHttpClient"

    SimpleHttpClient(String endpoint) {
        this.endpoint = endpoint
    }

    def reset() {
        previousRequests = new ArrayList()
    }


    def makeCall(HttpUriRequest httpMethod, Map requestHeaders, String requestPath) {
        RequestState requestState = new RequestState()
        requestState.requestHeaders = requestHeaders
        requestState.requestPath = requestPath

        def SimpleHttpResponse simpleResponse = new SimpleHttpResponse()
        def HttpClient client

        try {
            client = new DefaultHttpClient()

            requestHeaders.each { key, value ->
                httpMethod.addHeader(key, value)
            }

            def response = client.execute(httpMethod)

            simpleResponse.statusCode = response.statusLine.statusCode
            if (response.entity) {
                simpleResponse.responseBody = response.entity.content.getText()
            }
            simpleResponse.responseHeaders = response.getAllHeaders()

        } finally {
            if (client != null && client.getConnectionManager() != null) {
                client.getConnectionManager().shutdown();
            }
        }

        requestState.response = simpleResponse
        previousRequests.add(requestState)

        return simpleResponse
    }

    def doGet(String path, Map headers = new HashMap()) {

        def String requestPath

        if (!path.startsWith("http")) {
            requestPath = endpoint + path
        } else {
            requestPath = path
        }

        HttpGet httpGet = new HttpGet(requestPath)

        makeCall(httpGet, headers, requestPath)
    }


    def doPut(String path, Map headers, String payload) {
        def requestPath = endpoint + path

        HttpPut httpPut = new HttpPut(requestPath)
        if (payload) {
            httpPut.setEntity(new StringEntity(payload, Charset.forName("UTF-8")))
        }

        makeCall(httpPut, headers, requestPath)
    }

    def doDelete(String path, Map headers, String payload) {
        def requestPath = endpoint + path

         EntityEnclosingDelete httpDelete = new EntityEnclosingDelete()
         URI uri = URI.create(requestPath)
         httpDelete.setURI(uri)
        if (payload) {
            httpDelete.setEntity(new StringEntity(payload, Charset.forName("UTF-8")))
        }

        makeCall(httpDelete, headers, requestPath)
    }



    def doPost(String path, Map headers, String payload) {
        def requestPath = endpoint + path

        HttpPost httpPost = new HttpPost(requestPath)
        if (payload) {
            httpPost.setEntity(new StringEntity(payload, Charset.forName("UTF-8")))
        }

        makeCall(httpPost, headers, requestPath)
    }

    String logState() {
        if (previousRequests.size() == 0) {
            return
        }

        println(" ------- " + name + " state -------\n")

        def counter = 0
        previousRequests.each { it ->
            counter++
            printf(" ==============================================\n")
            printf(" REQUEST #" + counter + "\n")
            printf(" ==============================================\n")
            printf(" %18s: %s\n", "Request URL", it.requestPath)
            it.requestHeaders.each() { key, value ->
                printf(" %18s: %s\n", key, value)
            }

            if (it.response != null) {
                printf(" %18s: %s\n", "Response code", it.response.statusCode)
                if (it.response.responseHeaders != null && it.response.responseHeaders.size() > 0) {
                    printf(" %18s: %s\n", "Response headers", it.response.responseHeaders.toString())
                }

                if (it.response.responseBody != null) {
                    printf(" %18s:\n", "Response body")
                    printf(" %s", it.response.responseBody)
                }
            }
            println("\n")
        }

        println(" ------- " + name + " state -------\n")
    }
}

class RequestState {
    String requestPath
    Map<String,String> requestHeaders
    SimpleHttpResponse response
    String method
}


class EntityEnclosingDelete extends HttpEntityEnclosingRequestBase {

    @Override
    public String getMethod() {
        return "DELETE";
    }

}