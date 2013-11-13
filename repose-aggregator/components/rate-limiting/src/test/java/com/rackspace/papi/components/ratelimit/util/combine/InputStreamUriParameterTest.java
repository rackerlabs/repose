package com.rackspace.papi.components.ratelimit.util.combine;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.transform.TransformerException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 9, 2011
 * Time: 4:16:16 PM
 */
@RunWith(Enclosed.class)
public class InputStreamUriParameterTest {
    public static class WhenResolvingUrls {
        private InputStreamUriParameter inputStreamUriParameter;
        private InputStream inputStreamReference;

        @Before
        public void setup() {
            inputStreamReference = mock(InputStream.class);
            when(inputStreamReference.toString()).thenReturn("streamRef");

            inputStreamUriParameter = new InputStreamUriParameter(inputStreamReference);
        }

        @Test
        public void shouldReturnNewStreamSourceIfIsMatchingHref() throws TransformerException {
            String validHref = "reference:jio:streamRef";

            assertNotNull(inputStreamUriParameter.resolve(validHref, null));
        }

        @Test(expected= CombinedLimitsTransformerException.class)
        public void shouldThrowExceptionIfIsNotMatchingHref() throws TransformerException {
            String validHref = "reference:jio:invalidStreamRef";

            assertNotNull(inputStreamUriParameter.resolve(validHref, null));
        }
    }
}
