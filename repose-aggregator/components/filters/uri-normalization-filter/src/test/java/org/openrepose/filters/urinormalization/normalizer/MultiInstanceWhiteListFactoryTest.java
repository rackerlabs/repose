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
            for (UriParameter param : params.getParameter()) {
                assertEquals(param.getName(), filter.getParameterList().getParameter().get(i++).getName());
            }
        }
    }
}
