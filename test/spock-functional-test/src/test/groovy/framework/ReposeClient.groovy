package framework

import framework.client.http.SimpleHttpClient

class ReposeClient extends SimpleHttpClient {

    ReposeClient(String endpoint) {
        super(endpoint)
    }

}
