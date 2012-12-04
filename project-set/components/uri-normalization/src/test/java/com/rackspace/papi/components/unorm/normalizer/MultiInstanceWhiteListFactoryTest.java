package com.rackspace.papi.components.unorm.normalizer;

import com.rackspace.papi.components.uri.normalization.config.HttpUriParameterList;
import com.rackspace.papi.components.uri.normalization.config.UriParameter;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class MultiInstanceWhiteListFactoryTest {

    public static class WhenCreatingWhiteLists {

        private MultiInstanceWhiteListFactory instance;
        private HttpUriParameterList params;

        @Before
        public void setup() {
            params = new HttpUriParameterList();
            UriParameter param = new UriParameter();
            param.setCaseSensitive(false);
            param.setName("param1");
            param.setMultiplicity(10);
            params.getParameter().add(param);
            param = new UriParameter();
            param.setCaseSensitive(false);
            param.setName("param2");
            param.setMultiplicity(2);
            params.getParameter().add(param);

            instance = new MultiInstanceWhiteListFactory(params);
        }

        @Test
        public void shouldCreateNewInstance() {
            assertNotNull(instance);
        }

        @Test
        public void shouldPassOurParameterList() {
            MultiInstanceWhiteList filter = (MultiInstanceWhiteList) instance.newInstance();
            assertEquals(params.getParameter().size(), filter.getParameterList().getParameter().size());
            
            int i = 0;
            for (UriParameter param: params.getParameter()) {
                assertEquals(param.getName(), filter.getParameterList().getParameter().get(i++).getName());
            }
        }
    }
}
