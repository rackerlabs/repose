package com.rackspace.auth.v2_0;

import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    String endpoint = "http://auth-n01.dev.us.ccp.rackspace.net/v2.0/tokens";    
    String username = "auth";
    String password = "auth123";
    String tenant = "?";
    String token = "?";

    @Test
    public void shouldValidateToken() {
        GenericServiceClient client = new GenericServiceClient(username, password);
    }   
}
