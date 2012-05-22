/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.unorm;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class UriNormalizationHandlerFactoryTest {
    
    private UriNormalizationHandlerFactory instance;
    @Before
    public void setUp() {
        instance = new UriNormalizationHandlerFactory();
    }
    

    
    @Test
    public void shouldCreateNewConfigListener() {
        int expected = 1;
         assertEquals("Should have a config listener", expected, instance.getListeners().size());
    }

    
    @Test
    public void shouldCreateNewInstanceOfContentNormalizationHandler() {
        UriNormalizationHandler handler = instance.buildHandler();
        assertNotNull("Instance of Content Normalization Handler should not be null", handler);
    }
}
