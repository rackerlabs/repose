/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.string;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import static org.mockito.Mockito.*;

/**
 *
 * @author kush5342
 */
public class PasswordSensitiveToStringStrategyTest {
    
   
    /**
     * Test of appendField method, of class PasswordSensitiveToStringStrategy.
     */
    @Test
    public void testAppendField() {
       
        ObjectLocator objectLocator = mock( ObjectLocator.class);
        Object o = null;
        String s = "abcd";
        StringBuilder stringBuilder = new StringBuilder();
        Object o1 = mock(Object.class);
        PasswordSensitiveToStringStrategy instance = new PasswordSensitiveToStringStrategy();
        StringBuilder result = instance.appendField(objectLocator, o, "password", stringBuilder, o1);
        assertEquals("password=*******, ", result.toString());
       
    }
}
