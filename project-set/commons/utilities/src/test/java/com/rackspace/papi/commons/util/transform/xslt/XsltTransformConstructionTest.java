package com.rackspace.papi.commons.util.transform.xslt;

import com.rackspace.papi.commons.util.pooling.Pool;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class XsltTransformConstructionTest {

    public static class TestParent {

        XsltTransformConstruction xsltTransformConstruction;
        Templates templates;
        Transformer transformer;

        @Before
        public void setUp() throws Exception {
            xsltTransformConstruction = new XsltTransformConstruction();
            transformer = mock(Transformer.class);
            templates = mock(Templates.class);
        }

        @Test
        public void shouldReturnTypePool() throws Exception {
            when(templates.newTransformer()).thenReturn(transformer);
            assertThat(xsltTransformConstruction.generateXsltResourcePool(templates), is(instanceOf(Pool.class)));
        }
    }
}
