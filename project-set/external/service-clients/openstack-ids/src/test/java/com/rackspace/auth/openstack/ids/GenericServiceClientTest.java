package com.rackspace.auth.openstack.ids;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import org.junit.experimental.runners.Enclosed;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.v2.Token;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class GenericServiceClientTest {
    public static class WhenHittingAuth2_0 {
        String endpoint = "";
        String username = "";
        String password = "";
        String tenant = "?";
        String token = "?";
        String userId = "";

//        @Test
//        public void shouldValidateToken() {
//            GenericServiceClient client = new GenericServiceClient(username, password);
//
//            final ServiceClientResponse<Groups> serviceResponse = client.get(endpoint + "/users/" + userId + "/RAX-KSGRP");
//            final int response = serviceResponse.getStatusCode();
//            Groups groups = null;
//
//            OpenStackCoreResponseUnmarshaller responseUnmarshaller = new OpenStackCoreResponseUnmarshaller();
//
//            switch (response) {
//                case 200:
//                    groups = responseUnmarshaller.unmarshall(serviceResponse.getData(), Groups.class);
//            }
//        }

        @Test
        public void shouldGetAdminAuthToken() {

//            GenericServiceClient client = new GenericServiceClient(username, password);
//
//            final ServiceClientResponse<Token> serviceResponse = client.post(endpoint + "/token");
//            final int response = serviceResponse.getStatusCode();
//            Token token = null;
//
//            System.out.println(Integer.toString(response));
//            OpenStackCoreResponseUnmarshaller responseUnmarshaller = new OpenStackCoreResponseUnmarshaller();
//
//            switch (response) {
//                case 200:
//                    token = responseUnmarshaller.unmarshall(serviceResponse.getData(), Token.class);
//            }

        }


        
    }
}
