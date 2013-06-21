package framework.client.http

import org.apache.http.Header

class SimpleHttpResponse {

    def String responseBody
    def statusCode
    def Header[] responseHeaders

    def getHeader(String name) {
        def String foundValue

        responseHeaders.each { header ->
            if (header.name.equals(name)) {
                foundValue = header.value
            }
        }
        return foundValue
    }

    @Override
    String toString() {
        return "response body: " + responseBody + " statusCode: " + statusCode + " responseHeaders: " + responseHeaders.toString()
    }
}