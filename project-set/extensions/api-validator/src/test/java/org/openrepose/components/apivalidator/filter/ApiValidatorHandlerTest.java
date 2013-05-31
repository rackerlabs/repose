package org.openrepose.components.apivalidator.filter;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.FilterChain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ApiValidatorHandlerTest {

    public static class WhenApplyingValidators {
        private ValidatorInfo defaultValidatorInfo;
        private ValidatorInfo role1ValidatorInfo;
        private ValidatorInfo role2ValidatorInfo;
        private Validator defaultValidator;
        private Validator role1Validator;
        private Validator role2Validator;
        private ApiValidatorHandler instance;
        private FilterChain chain;
        private MutableHttpServletRequest request;
        private MutableHttpServletResponse response;
        private ValidatorInfo nullValidatorInfo;
        private ValidatorInfo blowupValidatorInfo;
        private Validator blowupValidator;

        @Before
        public void setup() {
            chain = mock(FilterChain.class);
            request = mock(MutableHttpServletRequest.class);
            response = mock(MutableHttpServletResponse.class);

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
            when(blowupValidator.validate(request, response, chain)).thenThrow(new RuntimeException("Test"));
            blowupValidatorInfo = new ValidatorInfo(Arrays.asList("blowupValidator"), "blowupWadl", null, null);
            blowupValidatorInfo.setValidator(blowupValidator);

            List<ValidatorInfo> validators = new ArrayList<ValidatorInfo>();
            validators.add(defaultValidatorInfo);
            validators.add(role1ValidatorInfo);
            validators.add(role2ValidatorInfo);
            validators.add(nullValidatorInfo);
            validators.add(blowupValidatorInfo);
            
            instance = new ApiValidatorHandler(defaultValidatorInfo, validators, false);
            instance.setFilterChain(chain);

        }
        
        @Test
        public void shouldCallDefaultValidatorWhenNoRoleMatch() {
            
            instance.handleRequest(request, response);
            verify(defaultValidator).validate(request, response, chain);
        }
        
        @Test
        public void shouldCallValidatorForRole() {
            List<HeaderValue> roles = new ArrayList<HeaderValue>();
            roles.add(new HeaderValueImpl("role1"));
            
            when(request.getPreferredHeaderValues(eq(OpenStackServiceHeader.ROLES.toString()), any(HeaderValueImpl.class))).thenReturn(roles);
            
            instance.handleRequest(request, response);
            verify(role1Validator).validate(request, response, chain);
        }

        @Test
        public void shouldHandleNullValidators() {
            List<HeaderValue> roles = new ArrayList<HeaderValue>();
            roles.add(new HeaderValueImpl("nullValidator"));
            
            when(request.getPreferredHeaderValues(eq(OpenStackServiceHeader.ROLES.toString()), any(HeaderValueImpl.class))).thenReturn(roles);
            FilterDirector director = instance.handleRequest(request, response);
            verify(nullValidatorInfo).getValidator();
            assertEquals(HttpStatusCode.BAD_GATEWAY, director.getResponseStatus());
        }

        @Test
        public void shouldHandleExceptionsInValidators() {
            List<HeaderValue> roles = new ArrayList<HeaderValue>();
            roles.add(new HeaderValueImpl("blowupValidator"));
            when(request.getPreferredHeaderValues(eq(OpenStackServiceHeader.ROLES.toString()), any(HeaderValueImpl.class))).thenReturn(roles);
            
            FilterDirector director = instance.handleRequest(request, response);
            verify(blowupValidator).validate(request, response, chain);
            assertEquals(HttpStatusCode.BAD_GATEWAY, director.getResponseStatus());
        }

        @Test
        public void shouldAddDefaultValidatorAsLeastPriorityWhenMultiMatch() {
            List<HeaderValue> roles = new ArrayList<HeaderValue>();
            roles.add(new HeaderValueImpl("role1"));

            List<ValidatorInfo> validators = new ArrayList<ValidatorInfo>();
            validators.add(role1ValidatorInfo);
            validators.add(role2ValidatorInfo);

            instance = new ApiValidatorHandler(defaultValidatorInfo, validators, true);

            List<ValidatorInfo> validatorsForRole = instance.getValidatorsForRole(roles);

            assertEquals(validatorsForRole.get(0), defaultValidatorInfo);
            assertEquals(validatorsForRole.get(1), role1ValidatorInfo);
        }

        @Test
        public void shouldRetainValidatorOrderWhenMultiMatchAndHasDefaultRole() {
            List<HeaderValue> roles = new ArrayList<HeaderValue>();
            roles.add(new HeaderValueImpl("role1"));
            roles.add(new HeaderValueImpl("role2"));
            roles.add(new HeaderValueImpl("defaultrole"));

            List<ValidatorInfo> validators = new ArrayList<ValidatorInfo>();
            validators.add(role1ValidatorInfo);
            validators.add(defaultValidatorInfo);
            validators.add(role2ValidatorInfo);

            instance = new ApiValidatorHandler(defaultValidatorInfo, validators, true);

            List<ValidatorInfo> validatorsForRole = instance.getValidatorsForRole(roles);

            assertEquals(validatorsForRole.get(0), role1ValidatorInfo);
            assertEquals(validatorsForRole.get(1), defaultValidatorInfo);
            assertEquals(validatorsForRole.get(2), role2ValidatorInfo);
        }

    }
}
