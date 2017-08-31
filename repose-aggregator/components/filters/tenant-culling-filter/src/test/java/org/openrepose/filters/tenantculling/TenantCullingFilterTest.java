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
package org.openrepose.filters.tenantculling;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.filters.keystonev2.KeystoneRequestHandler;
import org.openrepose.filters.keystonev2.KeystoneV2Filter;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import scala.Option;
import scala.collection.JavaConverters;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static com.rackspace.lefty.tenant.TenantCullingFilter.RELEVANT_ROLES;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID;

/**
 * Created by adrian on 6/12/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ObjectSerializer.class)
public class TenantCullingFilterTest {

    private TenantCullingFilter filter;
    private Datastore datastore;

    @Before
    public void setUp() throws Exception {
        datastore = mock(Datastore.class);
        DatastoreService datastoreService = mock(DatastoreService.class);
        when(datastoreService.getDefaultDatastore()).thenReturn(datastore);

        filter = new TenantCullingFilter(datastoreService);
    }

    @Test
    public void constructorGetsTheLocalDatastore() throws Exception {
        DatastoreService datastoreService = mock(DatastoreService.class);

        new TenantCullingFilter(datastoreService);

        verify(datastoreService).getDefaultDatastore();
    }

    @Test
    public void doFilterSendsTenantThatDontHaveRole() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeystoneV2Filter.AuthTokenKey(), "cachekey");
        KeystoneRequestHandler.ValidToken token = tokenWithTenant("123456");
        when(datastore.get("cachekey")).thenReturn(token);

        filter.doFilter(request, mock(HttpServletResponse.class), filterChain);

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(captor.capture(), any());
        assertThat(captor.getValue().getHeader(TENANT_ID), equalTo("123456"));
    }

    @Test
    public void doFilterSendsTenantThatMatchesSingleRole() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeystoneV2Filter.AuthTokenKey(), "cachekey");
        request.addHeader(RELEVANT_ROLES, "role-name");
        List<KeystoneRequestHandler.Role> roles = new ArrayList<>();
        roles.add(new KeystoneRequestHandler.Role("role-name", Option.apply("123456")));
        roles.add(new KeystoneRequestHandler.Role("role-name", Option.apply(null)));
        roles.add(new KeystoneRequestHandler.Role("other-name", Option.apply("098765")));
        KeystoneRequestHandler.ValidToken token = tokenWithRoles(roles);
        when(datastore.get("cachekey")).thenReturn(token);

        filter.doFilter(request, mock(HttpServletResponse.class), filterChain);

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(captor.capture(), any());
        assertThat(captor.getValue().getHeaders(TENANT_ID), hasValue("123456"));
        assertThat(captor.getValue().getHeaders(TENANT_ID), not(hasValue("098765")));
    }

    @Test
    public void doFilterSendsMultipleTenantsThatMatchSingleRole() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeystoneV2Filter.AuthTokenKey(), "cachekey");
        request.addHeader(RELEVANT_ROLES, "role-name");
        List<KeystoneRequestHandler.Role> roles = new ArrayList<>();
        roles.add(new KeystoneRequestHandler.Role("role-name", Option.apply("123456")));
        roles.add(new KeystoneRequestHandler.Role("role-name", Option.apply("098765")));
        KeystoneRequestHandler.ValidToken token = tokenWithRoles(roles);
        when(datastore.get("cachekey")).thenReturn(token);

        filter.doFilter(request, mock(HttpServletResponse.class), filterChain);

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(captor.capture(), any());
        assertThat(captor.getValue().getHeaders(TENANT_ID), hasValue("123456"));
        assertThat(captor.getValue().getHeaders(TENANT_ID), hasValue("098765"));
    }

    @Test
    public void doFilterSendsMultipleTenantsThatMatchMultipleRoles() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeystoneV2Filter.AuthTokenKey(), "cachekey");
        request.addHeader(RELEVANT_ROLES, "role-name,other-name");
        List<KeystoneRequestHandler.Role> roles = new ArrayList<>();
        roles.add(new KeystoneRequestHandler.Role("role-name", Option.apply("123456")));
        roles.add(new KeystoneRequestHandler.Role("other-name", Option.apply("098765")));
        KeystoneRequestHandler.ValidToken token = tokenWithRoles(roles);
        when(datastore.get("cachekey")).thenReturn(token);

        filter.doFilter(request, mock(HttpServletResponse.class), filterChain);

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(captor.capture(), any());
        assertThat(captor.getValue().getHeaders(TENANT_ID), hasValue("123456"));
        assertThat(captor.getValue().getHeaders(TENANT_ID), hasValue("098765"));
    }

    @Test
    public void doFilterSendsTenantsWithAndWithoutRoles() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeystoneV2Filter.AuthTokenKey(), "cachekey");
        request.addHeader(RELEVANT_ROLES, "role-name");
        List<KeystoneRequestHandler.Role> roles = new ArrayList<>();
        roles.add(new KeystoneRequestHandler.Role("role-name", Option.apply("123456")));
        roles.add(new KeystoneRequestHandler.Role("other-name", Option.apply("098765")));
        KeystoneRequestHandler.ValidToken token = tokenWithTenantAndRoles("abcdef", roles);
        when(datastore.get("cachekey")).thenReturn(token);

        filter.doFilter(request, mock(HttpServletResponse.class), filterChain);

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(captor.capture(), any());
        assertThat(captor.getValue().getHeaders(TENANT_ID), hasValue("abcdef"));
        assertThat(captor.getValue().getHeaders(TENANT_ID), hasValue("123456"));
        assertThat(captor.getValue().getHeaders(TENANT_ID), not(hasValue("098765")));
    }

    @Test
    public void doFilterReplacesTenant() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeystoneV2Filter.AuthTokenKey(), "cachekey");
        request.addHeader(TENANT_ID, "abcdef");
        KeystoneRequestHandler.ValidToken token = tokenWithTenant("123456");
        when(datastore.get("cachekey")).thenReturn(token);

        filter.doFilter(request, mock(HttpServletResponse.class), filterChain);

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(captor.capture(), any());
        assertThat(captor.getValue().getHeaders(TENANT_ID), not(hasValue("abcdef")));
        assertThat(captor.getValue().getHeaders(TENANT_ID), hasValue("123456"));
    }

    @Test
    public void doFilterCacheMissReturnsUnauthorized() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(KeystoneV2Filter.AuthTokenKey(), "cachekey");
        when(datastore.get("cachekey")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verifyZeroInteractions(filterChain);
        assertThat(response.getStatus(), equalTo(SC_UNAUTHORIZED));
    }

    @Test
    public void doFilterNoCacheKeyReturnsUnauthorized() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyZeroInteractions(filterChain);
        assertThat(response.getStatus(), equalTo(SC_UNAUTHORIZED));
    }

    @Test
    public void doFilterClassNotFoundExceptionReturnsInternalError() throws Exception {
        ObjectSerializer objectSerializer = (ObjectSerializer) ReflectionTestUtils.getField(filter, "objectSerializer");
        ObjectSerializer spySerializer = PowerMockito.spy(objectSerializer);
        ReflectionTestUtils.setField(filter, "objectSerializer", spySerializer);
        PowerMockito.doThrow(new ClassNotFoundException("test exception")).when(spySerializer).readObject(any(byte[].class));

        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(KeystoneV2Filter.AuthTokenKey(), "cachekey");
        KeystoneRequestHandler.ValidToken token = tokenWithTenant("123456");
        when(datastore.get("cachekey")).thenReturn(token);

        filter.doFilter(request, response, filterChain);

        verifyZeroInteractions(filterChain);
        assertThat(response.getStatus(), equalTo(SC_INTERNAL_SERVER_ERROR));
    }

    private KeystoneRequestHandler.ValidToken tokenWithTenant(String tenant) {
        return tokenWithTenantAndRoles(tenant, new ArrayList<>());
    }

    private KeystoneRequestHandler.ValidToken tokenWithRoles(List<KeystoneRequestHandler.Role> roles) {
        return tokenWithTenantAndRoles(null, roles);
    }

    private KeystoneRequestHandler.ValidToken tokenWithTenantAndRoles(String tenant, List<KeystoneRequestHandler.Role> roles) {
        return new KeystoneRequestHandler.ValidToken(
                null, null, JavaConverters.asScalaBufferConverter(roles).asScala().toSeq(),
                Option.apply(null), Option.apply(null), Option.apply(tenant),
                JavaConverters.asScalaBufferConverter(new ArrayList<String>()).asScala().toSeq(), Option.apply(null), Option.apply(null),
                JavaConverters.asScalaBufferConverter(new ArrayList<String>()).asScala().toSeq(), Option.apply(null), Option.apply(null),
                Option.apply(null));
    }

    private TypeSafeMatcher<Enumeration<String>> hasValue(String value) {
        return new TypeSafeMatcher<Enumeration<String>>() {
            @Override
            protected boolean matchesSafely(Enumeration<String> headers) {
                boolean result = false;
                while (headers.hasMoreElements()) {
                    result = (headers.nextElement().equals(value)) || result;
                }
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("an enumeration that contained a value of ").appendText(value);
            }

            @Override
            protected void describeMismatchSafely(Enumeration<String> enumeration, Description mismatchDescription) {
                mismatchDescription.appendText("an enumeration containing ");
                while (enumeration.hasMoreElements()) {
                    mismatchDescription.appendText(enumeration.nextElement()).appendText(", ");
                }
            }
        };
    }
}
