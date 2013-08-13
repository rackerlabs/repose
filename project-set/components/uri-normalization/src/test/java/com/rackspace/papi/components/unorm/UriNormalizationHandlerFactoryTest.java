package com.rackspace.papi.components.unorm;


import com.rackspace.papi.components.uri.normalization.config.UriNormalizationConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class UriNormalizationHandlerFactoryTest {
    
    private UriNormalizationHandlerFactory instance;
    @Before
    public void setUp() {
        instance = new UriNormalizationHandlerFactory(null);
    }
    

    
    @Test
    public void shouldCreateNewConfigListener() {
        int expected = 1;
         assertEquals("Should have a config listener", expected, instance.getListeners().size());
    }

    
    @Test
    public void shouldCreateNewInstanceOfContentNormalizationHandler() {
     
       UriNormalizationConfig config=new UriNormalizationConfig();
       instance.configurationUpdated(config);
       
        UriNormalizationHandler handler = instance.buildHandler();
        assertNotNull("Instance of Content Normalization Handler should not be null", handler);
    }
}
