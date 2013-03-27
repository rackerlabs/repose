/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.config.parser.generic;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author kush5342
 */
public class GenericResourceConfigurationParserTest {
    
   

    /**
     * Test of read method, of class GenericResourceConfigurationParser.
     */
    @Test
    public void testRead() {
    
        ConfigurationResource cr = mock(ConfigurationResource.class);
        GenericResourceConfigurationParser instance = new GenericResourceConfigurationParser();
        ConfigurationResource expResult = null;
        ConfigurationResource result = instance.read(cr);
        assertNotNull(result);
    }
}
