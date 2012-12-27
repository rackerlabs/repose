package com.rackspace.papi.components.cnorm;


import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ContentNormalizationHandlerFactoryTest {
    
    private ContentNormalizationHandlerFactory instance;
    @Before
    public void setUp() {
        instance = new ContentNormalizationHandlerFactory();
    }
    

    
    @Test
    public void shouldCreateNewConfigListener() {
        int expected = 1;
         assertEquals("Should have a config listener", expected, instance.getListeners().size());
    }

    
    @Test
    public void shouldCreateNewInstanceOfContentNormalizationHandler() {
     
       ContentNormalizationConfig config=new ContentNormalizationConfig();
        instance.configurationUpdated(config);
        
        ContentNormalizationHandler handler = instance.buildHandler();
        
        assertNotNull("Instance of Content Normalization Handler should not be null", handler);
    }
}
