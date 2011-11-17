package com.rackspace.auth.openstack.ids;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Token;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
//            ResponseUnmarshaller responseUnmarshaller = new ResponseUnmarshaller();
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
//            ResponseUnmarshaller responseUnmarshaller = new ResponseUnmarshaller();
//
//            switch (response) {
//                case 200:
//                    groups = responseUnmarshaller.unmarshall(serviceResponse.getData(), Token.class);
//            }
        }


        
    }
}
