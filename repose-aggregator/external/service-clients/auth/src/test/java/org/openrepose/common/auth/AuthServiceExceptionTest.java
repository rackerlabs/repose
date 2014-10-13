/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.common.auth;

import org.junit.Test;
import org.openrepose.common.auth.AuthServiceException;

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