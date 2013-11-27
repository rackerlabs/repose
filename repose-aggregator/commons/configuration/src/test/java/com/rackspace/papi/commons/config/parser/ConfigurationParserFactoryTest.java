/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.config.parser;

import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.commons.config.parser.inputstream.InputStreamConfigurationParser;
import com.rackspace.papi.commons.config.parser.jaxb.Element;
import com.rackspace.papi.commons.config.parser.jaxb.JaxbConfigurationParser;
import com.rackspace.papi.commons.config.parser.properties.PropertiesFileConfigurationParser;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author kush5342
 */
public class ConfigurationParserFactoryTest {
    
    
    /**
     * Test of newConfigurationParser method, of class ConfigurationParserFactory.
     */
    @Test
    public void testNewConfigurationParser() {
       
        Throwable caught = null;
      
       try {
            ConfigurationParser result = ConfigurationParserFactory.newConfigurationParser(ConfigurationParserType.valueOf("test"),null);
         } catch (Throwable t) {
            caught = t;
         }
         assertNotNull(caught);
         assertSame(IllegalArgumentException.class, caught.getClass());
    }

    @Test
    public void testNewConfigurationParserWithRawType() {
        ConfigurationParser result = ConfigurationParserFactory.newConfigurationParser(ConfigurationParserType.RAW, null);
        assertThat(result, IsInstanceOf.instanceOf(InputStreamConfigurationParser.class));
    }

    @Test
    public void testNewConfigurationParserWithPropertiesType() {
        ConfigurationParser result = ConfigurationParserFactory.newConfigurationParser(ConfigurationParserType.PROPERTIES, null);
        assertThat(result, IsInstanceOf.instanceOf(PropertiesFileConfigurationParser.class));
    }

    /**
     * Test of newInputStreamConfigurationParser method, of class ConfigurationParserFactory.
     */
    @Test
    public void testNewInputStreamConfigurationParser() {

        ConfigurationParser result = ConfigurationParserFactory.newInputStreamConfigurationParser();
        assertThat(result,IsInstanceOf.instanceOf(InputStreamConfigurationParser.class));
      
        
    }

    /**
     * Test of newPropertiesFileConfigurationParser method, of class ConfigurationParserFactory.
     */
    @Test
    public void testNewPropertiesFileConfigurationParser() {
       
        ConfigurationParser result = ConfigurationParserFactory.newPropertiesFileConfigurationParser();
        assertThat(result,IsInstanceOf.instanceOf(PropertiesFileConfigurationParser.class));
    }

    /**
     * Test of getXmlConfigurationParser method, of class ConfigurationParserFactory.
     */
    @Test
    public void testGetXmlConfigurationParser() {
     Throwable caught = null;
      
        try {
        JaxbConfigurationParser result = ConfigurationParserFactory.getXmlConfigurationParser(Element.class,  null);
        
         } catch (Throwable t) {
            caught = t;
         }
         assertNotNull(caught);
         assertSame(ConfigurationResourceException.class, caught.getClass());
        
    }
}
