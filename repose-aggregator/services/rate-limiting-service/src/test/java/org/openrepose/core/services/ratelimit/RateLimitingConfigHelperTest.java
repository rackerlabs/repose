package org.openrepose.core.services.ratelimit;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.ratelimit.config.ConfiguredRateLimitWrapper;
import org.openrepose.core.services.ratelimit.config.RateLimitingConfigHelper;
import org.openrepose.core.services.ratelimit.config.RateLimitingConfiguration;
import org.openrepose.core.services.ratelimit.config.ConfiguredLimitGroup;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
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
