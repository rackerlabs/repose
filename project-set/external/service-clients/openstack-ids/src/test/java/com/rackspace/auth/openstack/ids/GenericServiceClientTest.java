package com.rackspace.auth.openstack.ids;

import org.junit.experimental.runners.Enclosed;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class GenericServiceClientTest {
    public static class WhenHittingAuth2_0 {
        String endpoint = "http://auth-n01.dev.us.ccp.rackspace.net/v2.0";
        String username = "auth";
        String password = "auth123";
        String tenant = "?";
        String token = "?";
        String userId = "102916";

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
        public void shouldGetAuthToken() {

//            GenericServiceClient client = new GenericServiceClient(username, password);
//
//            final ServiceClientResponse<Token> serviceResponse = client.getAdminToken(endpoint + "/token", "auth", "auth123");
//            final int response = serviceResponse.getStatusCode();
//            Token groups = null;
//
//            OpenStackCoreResponseUnmarshaller responseUnmarshaller = new OpenStackCoreResponseUnmarshaller();
//
//            switch (response) {
//                case 200:
//                    groups = responseUnmarshaller.unmarshall(serviceResponse.getData(), Token.class);
//            }
        }


        
    }
}
