/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.validate.xsd;

import org.junit.*;

import javax.xml.bind.ValidationEvent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 *
 * @author kush5342
 */
public class JAXBValidatorTest {
    
    public JAXBValidatorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of handleEvent method, of class JAXBValidator.
     */
    @Test
    public void testHandleEvent() {
       
        ValidationEvent event = mock(ValidationEvent.class);
        JAXBValidator instance = new JAXBValidator();
        boolean expResult = true;
        boolean result = instance.handleEvent(event);
        assertEquals(expResult, result);
                 
    }
}
