/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.urinormalization;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.filters.urinormalization.config.UriNormalizationConfig;

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
    public void shouldCreateNewInstanceOfContentNormalizationHandler() throws Exception {
     
       UriNormalizationConfig config=new UriNormalizationConfig();
       instance.configurationUpdated(config);
       
        UriNormalizationHandler handler = instance.buildHandler();
        assertNotNull("Instance of Content Normalization Handler should not be null", handler);
    }
}
