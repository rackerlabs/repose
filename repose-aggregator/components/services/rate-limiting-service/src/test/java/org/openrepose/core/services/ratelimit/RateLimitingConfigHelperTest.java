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
package org.openrepose.core.services.ratelimit;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.ratelimit.config.ConfiguredLimitGroup;
import org.openrepose.core.services.ratelimit.config.ConfiguredRateLimitWrapper;
import org.openrepose.core.services.ratelimit.config.RateLimitingConfigHelper;
import org.openrepose.core.services.ratelimit.config.RateLimitingConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RateLimitingConfigHelperTest {
    private RateLimitingConfiguration config;
    private RateLimitingConfigHelper helper;

    @Before
    public void setupSpec() {
        this.config = RateLimitingTestSupport.defaultRateLimitingConfiguration();
        this.helper = new RateLimitingConfigHelper(config);
    }

    @Test
    public void shouldGetGroupByRole() {
        List<String> roles = new ArrayList<>();
        roles.add("group");
        roles.add("anotha");

        ConfiguredLimitGroup group = helper.getConfiguredGroupByRole(roles);

        assertEquals(group.getId(), config.getLimitGroup().get(0).getId());
    }

    @Test
    public void getGlobalLimitGroup() {
        assertThat(helper.getGlobalLimitGroup().getLimit().size(), equalTo(1));
        assertThat(helper.getGlobalLimitGroup().getLimit().get(0), instanceOf(ConfiguredRateLimitWrapper.class));
        assertThat(helper.getGlobalLimitGroup().getLimit().get(0).getId(), equalTo("catch-all"));
    }
}
