package com.rackspace.papi.components.translation.resolvers;

import net.sf.saxon.lib.OutputURIResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class OutputStreamUriParameterResolverTest {

    public static class WhenAddingStreams {
        private OutputStreamUriParameterResolver resolver;
        private OutputURIResolver parent;
        private OutputStream output;
        private Result result;

        @Before
        public void setUp() throws TransformerException {
            parent = mock(OutputURIResolver.class);
            output = mock(OutputStream.class);
            resolver = new OutputStreamUriParameterResolver(parent);
            result = mock(Result.class);
            when(parent.resolve(anyString(), anyString())).thenReturn(result);
            
        }
        
        @Test
        public void shouldAddStream() throws TransformerException {
            String name = "out";
            resolver.addStream(output, name);
            String href = resolver.getHref(name);
            assertNotNull("Should return the href for our output stream", href);
            StreamResult result = (StreamResult)resolver.resolve(href, "");
            assertNotNull("Should return a StreamResult which wraps our output stream", result);
            assertNotNull("Source stream should include a path", result.getSystemId());
            assertFalse("Source stream path should not be empty", result.getSystemId().isEmpty());
            assertTrue("Should return our output stream", output == result.getOutputStream());
        }
        
        @Test
        public void shouldCallParentResolver() throws TransformerException {
            String name = "someUri";
            String href = name;
            String base = "somebase";
            Result result = resolver.resolve(href, base);
            
            verify(parent).resolve(href, base);
            assertNotNull(result);
            
        }
        
        @Test(expected=RuntimeException.class)
        public void shouldThrowExceptionWhenCannotResolve() throws TransformerException {
            final String doesntExist = "reference:jio:doesn'tExist";
            resolver.resolve(doesntExist, "somebase");
        }
    }
}
