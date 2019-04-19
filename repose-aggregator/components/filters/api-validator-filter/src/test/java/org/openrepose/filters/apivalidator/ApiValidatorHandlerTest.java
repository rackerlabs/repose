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
package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.ValidatorException;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ApiValidatorHandlerTest {

    private ValidatorInfo defaultValidatorInfo;
    private ValidatorInfo role1ValidatorInfo;
    private ValidatorInfo role2ValidatorInfo;
    private Validator defaultValidator;
    private Validator role1Validator;
    private Validator role2Validator;
    private ApiValidatorHandler instance;
    private FilterChain chain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ValidatorInfo nullValidatorInfo;
    private ValidatorInfo blowupValidatorInfo;
    private Validator blowupValidator;

    @Before
    public void setup() {
        chain = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        defaultValidator = mock(Validator.class);
        defaultValidatorInfo = new ValidatorInfo(Arrays.asList("defaultrole"), "defaultwadl", null, null);
        defaultValidatorInfo.setValidator(defaultValidator);

        role1Validator = mock(Validator.class);
        role1ValidatorInfo = new ValidatorInfo(Arrays.asList("role1"), "role1wadl", null, null);
        role1ValidatorInfo.setValidator(role1Validator);

        role2Validator = mock(Validator.class);
        role2ValidatorInfo = new ValidatorInfo(Arrays.asList("role2"), "role2wadl", null, null);
        role2ValidatorInfo.setValidator(role2Validator);

        nullValidatorInfo = mock(ValidatorInfo.class);
        when(nullValidatorInfo.getRoles()).thenReturn(Arrays.asList("nullValidator"));
        when(nullValidatorInfo.getValidator()).thenReturn(null);

        blowupValidator = mock(Validator.class);
        when(blowupValidator.validate(any(HttpServletRequestWrapper.class), eq(response), eq(chain))).thenThrow(new ValidatorException("Test", new RuntimeException("Test")));
        blowupValidatorInfo = new ValidatorInfo(Arrays.asList("blowupValidator"), "blowupWadl", null, null);
        blowupValidatorInfo.setValidator(blowupValidator);

        List<ValidatorInfo> validators = new ArrayList<>();
        validators.add(defaultValidatorInfo);
        validators.add(role1ValidatorInfo);
        validators.add(role2ValidatorInfo);
        validators.add(nullValidatorInfo);
        validators.add(blowupValidatorInfo);

        instance = new ApiValidatorHandler(defaultValidatorInfo, validators, false, false, Optional.empty());

        request.setRequestURI("/path/to/resource");
    }

    @Test
    public void shouldCallDefaultValidatorWhenNoRoleMatch() {
        instance.doFilter(request, response, chain);
        verify(defaultValidator).validate(any(HttpServletRequestWrapper.class), eq(response), eq(chain));
    }

    @Test
    public void shouldCallValidatorForRole() {
        request.addHeader(OpenStackServiceHeader.ROLES, "junk;q=0.8,role1;q=0.9,bbq;q=0.9,stuff;q=0.7");

        instance.doFilter(request, response, chain);
        verify(role1Validator).validate(any(HttpServletRequestWrapper.class), eq(response), eq(chain));
    }

    @Test
    public void shouldHandleNullValidators() {
        request.addHeader(OpenStackServiceHeader.ROLES, "nullValidator");

        instance.doFilter(request, response, chain);
        verify(nullValidatorInfo).getValidator();
        assertEquals(HttpServletResponse.SC_BAD_GATEWAY, response.getStatus());
    }

    @Test
    public void shouldHandleExceptionsInValidators() {
        request.addHeader(OpenStackServiceHeader.ROLES, "blowupValidator");

        instance.doFilter(request, response, chain);
        verify(blowupValidator).validate(any(HttpServletRequestWrapper.class), eq(response), eq(chain));
        assertEquals(HttpServletResponse.SC_BAD_GATEWAY, response.getStatus());
    }

    @Test
    public void shouldAddDefaultValidatorAsLeastPriorityWhenMultiMatch() {
        List<String> roles = Collections.singletonList("role1");

        List<ValidatorInfo> validators = new ArrayList<>();
        validators.add(role1ValidatorInfo);
        validators.add(role2ValidatorInfo);

        instance = new ApiValidatorHandler(defaultValidatorInfo, validators, true, false, Optional.empty());
        List<ValidatorInfo> validatorsForRole = instance.getValidatorsForRoles(roles);
        assertEquals(validatorsForRole.get(0), defaultValidatorInfo);
        assertEquals(validatorsForRole.get(1), role1ValidatorInfo);
    }

    @Test
    public void shouldRetainValidatorOrderWhenMultiMatchAndHasDefaultRole() {
        List<String> roles = Arrays.asList("role1", "role2", "defaultrole");

        List<ValidatorInfo> validators = new ArrayList<>();
        validators.add(role1ValidatorInfo);
        validators.add(defaultValidatorInfo);
        validators.add(role2ValidatorInfo);

        instance = new ApiValidatorHandler(defaultValidatorInfo, validators, true, false, Optional.empty());

        List<ValidatorInfo> validatorsForRole = instance.getValidatorsForRoles(roles);

        assertEquals(validatorsForRole.get(0), role1ValidatorInfo);
        assertEquals(validatorsForRole.get(1), defaultValidatorInfo);
        assertEquals(validatorsForRole.get(2), role2ValidatorInfo);
    }
}
