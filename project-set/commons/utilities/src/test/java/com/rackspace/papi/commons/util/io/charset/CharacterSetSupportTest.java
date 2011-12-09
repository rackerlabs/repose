/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.io.charset;

import com.rackspace.papi.commons.util.io.charset.CharacterSetSupport;
import java.security.Permission;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author malconis
 */
public class CharacterSetSupportTest extends TestCase{
    
    @Before
    public void setUp(){
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of checkCharSet method, of class CharacterSetSupport.
     */
    @Test
    public void testCheckCharSet() {
        final SecurityManager catchManager = new SecurityManager() {

                @Override
                public void checkPermission(Permission prmsn) {
                    if (prmsn.getName().contains("exitVM")) {
                        throw new SecurityException();

                    }
                }
                
            };
        final SecurityManager originalManager = System.getSecurityManager();
        String unsupportedCharSet = "UFT-33";
        boolean exitCaught = false;
        try{
            System.setSecurityManager(catchManager);
            CharacterSetSupport.checkCharSet(unsupportedCharSet);
        }catch(SecurityException e){
            exitCaught = true;
        }finally{
            System.setSecurityManager(originalManager);
            assertTrue(exitCaught);
        }

    }
}
