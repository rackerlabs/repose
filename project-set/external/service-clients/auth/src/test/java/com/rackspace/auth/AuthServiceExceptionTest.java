/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.auth;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kush5342
 */
public class AuthServiceExceptionTest {

    @Test(expected=AuthServiceException.class)
    public void testAuthServiceExceptionWtihCause() {
        // TODO review the generated test code and remove the default call to fail.
        
         throw new AuthServiceException("test Exception", new Exception());

      }
    
  @Test(expected=AuthServiceException.class)
    public void testAuthServiceExceptionNoCause() {
        // TODO review the generated test code and remove the default call to fail.
        
         throw new AuthServiceException("test Exception");

      }
}