/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.logging.util;

import java.security.Permission;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.tools.ant.ExitException;
/**
 *
 * @author malconis
 */
public class CharacterSetSupportTest extends TestCase{

    private static class NoExitSecurityManager extends SecurityManager 
    {
        @Override
        public void checkPermission(Permission perm) 
        {
                // allow anything.
        }
        @Override
        public void checkPermission(Permission perm, Object context) 
        {
                // allow anything.
        }
        @Override
        public void checkExit(int status) 
        {
                super.checkExit(status);
                throw new ExitException(status);
        }
    }
    
    @Before
    public void setUp() throws Exception{
        super.setUp();
        System.setSecurityManager(new NoExitSecurityManager());
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of checkCharSet method, of class CharacterSetSupport.
     */
    @Test
    public void testCheckCharSet() {
        String unsupportedCharSet = "UFT-33";
        boolean exitCaught = false;
        try{
            CharacterSetSupport.checkCharSet(unsupportedCharSet);
        }catch(ExitException e){
            exitCaught=true;
        }
        assertTrue(exitCaught);

    }
}
