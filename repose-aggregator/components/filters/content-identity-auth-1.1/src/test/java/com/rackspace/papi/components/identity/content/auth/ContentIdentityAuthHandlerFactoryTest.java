package com.rackspace.papi.components.identity.content.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class ContentIdentityAuthHandlerFactoryTest {

    public static class TestParent {

        ContentIdentityAuthHandlerFactory contentIdentityAuthHandlerFactory;

        @Before
        public void setUp() throws Exception {
            contentIdentityAuthHandlerFactory = mock(ContentIdentityAuthHandlerFactory.class);
        }

        @Test
        public void shouldReturnMapOfListeners() {
            assertThat(contentIdentityAuthHandlerFactory.getListeners(), is(instanceOf(HashMap.class)));
        }


        @Test
        public void shouldReturnNullWhenBuildingHandlersWithoutInstantiation() {
            assertNull(contentIdentityAuthHandlerFactory.buildHandler());
        }
    }
}
