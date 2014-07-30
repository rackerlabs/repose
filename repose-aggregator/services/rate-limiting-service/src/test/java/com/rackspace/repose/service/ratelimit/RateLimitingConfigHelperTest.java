package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRateLimitWrapper;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfigHelper;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;
import org.junit.Before;
import org.junit.Test;

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
