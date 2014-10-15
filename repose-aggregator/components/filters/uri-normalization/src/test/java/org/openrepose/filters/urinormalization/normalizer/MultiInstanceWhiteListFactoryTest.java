package org.openrepose.filters.urinormalization.normalizer;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.filters.urinormalization.config.HttpUriParameterList;
import org.openrepose.filters.urinormalization.config.UriParameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
