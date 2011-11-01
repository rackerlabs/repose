package org.openrepose.rnxp.http;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class HttpMessageComponentOrderTest {

    public static class WhenComparingComponentOrder {

        @Test
        public void shouldIdentifyComponentComesBeforeAnother() {
            final HttpMessageComponentOrder componentOrder = HttpMessageComponentOrder.requestOrderInstance();
            
            assertTrue(componentOrder.isBefore(HttpMessageComponent.REQUEST_METHOD, HttpMessageComponent.HEADER));
        }

        @Test
        public void shouldIdentifyComponentComesAfterAnother() {
            final HttpMessageComponentOrder componentOrder = HttpMessageComponentOrder.requestOrderInstance();
            
            assertTrue(componentOrder.isAfter(HttpMessageComponent.HEADER, HttpMessageComponent.REQUEST_METHOD));
        }

        @Test
        public void shouldIdentifyComponentComesAfterOrIsEqualToAnother() {
            final HttpMessageComponentOrder componentOrder = HttpMessageComponentOrder.requestOrderInstance();
            
            assertTrue(componentOrder.isAfterOrEqual(HttpMessageComponent.HEADER, HttpMessageComponent.REQUEST_METHOD));
            assertTrue(componentOrder.isAfterOrEqual(HttpMessageComponent.REQUEST_METHOD, HttpMessageComponent.REQUEST_METHOD));
        }

        @Test
        public void shouldIdentifyEqualComponents() {
            final HttpMessageComponentOrder componentOrder = HttpMessageComponentOrder.requestOrderInstance();
            
            assertTrue(componentOrder.isEqual(HttpMessageComponent.HEADER, HttpMessageComponent.HEADER));
        }
    }
}
