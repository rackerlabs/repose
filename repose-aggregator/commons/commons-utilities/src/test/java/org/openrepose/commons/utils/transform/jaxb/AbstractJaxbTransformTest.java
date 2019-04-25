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
package org.openrepose.commons.utils.transform.jaxb;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Apr 25, 2011
 * Time: 6:03:36 PM
 */
public class AbstractJaxbTransformTest {
    private AbstractJaxbTransform jaxbTransform;
    private JAXBContext jaxbContext;

    @Before
    public void setup() {
        jaxbContext = mock(JAXBContext.class);

        jaxbTransform = new SampleJaxbTransform(jaxbContext);
    }

    @Test
    public void shouldReturnNonNullForMarshallerPool() {
        assertNotNull(jaxbTransform.getMarshallerPool());
    }

    @Test(expected = ResourceConstructionException.class)
    public void shouldThrowExceptionIfCanNotCreateMarshallerPool() throws Exception {
        when(jaxbContext.createMarshaller()).thenThrow(new JAXBException("test"));

        jaxbTransform.getMarshallerPool().borrowObject();
    }

    @Test
    public void shouldReturnNonNullForUnmarshallerPool() {
        assertNotNull(jaxbTransform.getUnmarshallerPool());
    }

    @Test(expected = ResourceConstructionException.class)
    public void shouldThrowExceptionIfCanNotCreateUnmarshallerPool() throws Exception {
        when(jaxbContext.createUnmarshaller()).thenThrow(new JAXBException("test"));

        jaxbTransform.getUnmarshallerPool().borrowObject();
    }

    static class SampleJaxbTransform extends AbstractJaxbTransform {
        public SampleJaxbTransform(JAXBContext ctx) {
            super(ctx);
        }
    }
}
