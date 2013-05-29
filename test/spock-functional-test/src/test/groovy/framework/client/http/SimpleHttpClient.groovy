package framework.client.http

import java.nio.charset.Charset
import org.apache.http.client.HttpClient
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.client.methods.*

abstract class SimpleHttpClient {

    List<RequestState> previousRequests = new ArrayList()

    def String endpoint = ""

    def String name = "SimpleHttpClient"

    SimpleHttpClient(String endpoint) {
        this.endpoint = endpoint
    }

    def reset() {
        previousRequests = new ArrayList()
    }


    def makeCall(HttpUriRequest httpMethod, HashMap requestParams, String requestPath) {
        RequestState requestState = new RequestState()
        requestState.requestParams = requestParams
        requestState.requestPath = requestPath

        def SimpleHttpResponse simpleResponse = new SimpleHttpResponse()
        def HttpClient client

        try {
            client = new DefaultHttpClient()

            if (requestParams.followRedirects == false){
                client.setRedirectStrategy(new DefaultRedirectStrategy() {
                    public boolean isRedirected(request, response, context) {
                        return false;
                    }
                });
            }


            if (requestParams.accept)
                httpMethod.addHeader("Accept", requestParams.accept)

            if (requestParams.contentType)
                httpMethod.addHeader("Content-Type", requestParams.contentType)

            if (requestParams.authToken)
                httpMethod.addHeader("X-Auth-Token", requestParams.authToken)

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

    def doGet(String path, HashMap requestParams) {

        def String requestPath

        if (!path.startsWith("http")) {
            requestPath = endpoint + path
        } else {
            requestPath = path
        }

        HttpGet httpGet = new HttpGet(requestPath)

        if (requestParams.followRedirects == "false")
            httpGet.setFollowRedirects(false)

        makeCall(httpGet, requestParams, requestPath)
    }


    def doPut(String path, HashMap requestParams) {
        def requestPath = endpoint + path

        HttpPut httpPut = new HttpPut(requestPath)
        if (requestParams.payload) {
            httpPut.setEntity(new StringEntity(requestParams.payload, Charset.forName("UTF-8")))
        }

        makeCall(httpPut, requestParams, requestPath)
    }

    def doDelete(String path, HashMap requestParams) {
        def requestPath = endpoint + path

         EntityEnclosingDelete httpDelete = new EntityEnclosingDelete()
         URI uri = URI.create(requestPath)
         httpDelete.setURI(uri)
        if (requestParams.payload) {
            httpDelete.setEntity(new StringEntity(requestParams.payload, Charset.forName("UTF-8")))
        }

        makeCall(httpDelete, requestParams, requestPath)
    }



    def doPost(String path, HashMap requestParams) {
        def requestPath = endpoint + path

        HttpPost httpPost = new HttpPost(requestPath)
        if (requestParams.payload) {
            httpPost.setEntity(new StringEntity(requestParams.payload, Charset.forName("UTF-8")))
        }

        makeCall(httpPost, requestParams, requestPath)
    }

    String logState() {
        if (previousRequests.size() == 0) {
            return
        }

        println("   ------- " + name + " state -------\n")

        def counter = 0
        previousRequests.each { it ->
            counter++
            printf("   ==============================================\n")
            printf("   REQUEST #" + counter + "\n")
            printf("   ==============================================\n")
            printf("   %18s: %s\n", "Request URL", it.requestPath)
            it.requestParams.each() { key, value ->
                printf("   %18s: %s\n", key, value)
            }

            if (it.response != null) {
                printf("   %18s: %s\n", "Response code", it.response.statusCode)
                if (it.response.responseHeaders != null && it.response.responseHeaders.size() > 0) {
                    printf("   %18s: %s\n", "Response headers", it.response.responseHeaders.toString())
                }

                if (it.response.responseBody != null) {
                    printf("   %18s:\n", "Response body")
                    printf("   %s", it.response.responseBody)
                }
            }
            println("\n")
        }

        println("   ------- " + name + " state -------\n")
    }
}

class RequestState {
    String requestPath
    Map<String, String> requestParams
    SimpleHttpResponse response
    String method
}


class EntityEnclosingDelete extends HttpEntityEnclosingRequestBase {

    @Override
    public String getMethod() {
        return "DELETE";
    }

}
