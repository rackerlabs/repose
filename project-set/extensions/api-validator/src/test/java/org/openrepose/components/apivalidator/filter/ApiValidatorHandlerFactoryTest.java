package org.openrepose.components.apivalidator.filter;

import com.rackspace.papi.commons.config.parser.generic.GenericResourceConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.service.config.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.components.apivalidator.servlet.config.BaseValidatorConfiguration;
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration2;
import org.openrepose.components.apivalidator.servlet.config.BaseValidatorItem;
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ApiValidatorHandlerFactoryTest {

    public static class WhenCreatingHandlers {
        private ConfigurationService configService;
        private final String wadl = "default.wadl";
        private final String dot = "default.dot";
        private final String role = "testRole";
        private final String defaultRole = "defaultRole";
        private ApiValidatorHandlerFactory instance;
        private List<HeaderValue> roles;
        
        @Before
        public void setup() {
            ValidatorConfiguration2 config = new ValidatorConfiguration2();
            ValidatorItem2 item = new ValidatorItem2();
            item.setWadl(wadl);
            List<String> role1=item.getRole();
            role1.add(role);
            config.getValidator().add(item);

            ValidatorItem2 defaultItem = new ValidatorItem2();
            defaultItem.setWadl(wadl);
            List<String> role2=defaultItem.getRole();
            role2.add(defaultRole);
        
            defaultItem.setDefault(Boolean.TRUE);
            defaultItem.setDotOutput(dot);
            config.getValidator().add(defaultItem);

            configService = mock(ConfigurationService.class);
            URL resource = this.getClass().getClassLoader().getResource("");
            instance = new ApiValidatorHandlerFactory(configService, resource.getPath(), "");

            instance.configurationUpdated(config);
            
            roles = new ArrayList<HeaderValue>();
            roles.add(new HeaderValueImpl(role));
            
        }
        
       @Test
        public void shouldBuildValidatorListAndSubscribeToWadl() {
            ApiValidatorHandler handler = instance.buildHandler();
            assertNotNull("Should build handler", handler);

            List<ValidatorInfo> validatorsForRole = handler.getValidatorsForRole(roles);
            assertNotNull(validatorsForRole);
            
            for(ValidatorInfo validatorForRole : validatorsForRole){
             assertEquals("Should get validator for role", role, validatorForRole.getRoles().get(0));
            }
            verify(configService, times(2)).subscribeTo(eq("api-validator"),eq(instance.getWadlPath(wadl)), any(ApiValidatorHandlerFactory.ApiValidatorWadlListener.class), any(GenericResourceConfigurationParser.class));
        }

        @Test
        public void shouldSetDefaultValidator() {
            ApiValidatorHandler handler = instance.buildHandler();
            assertNotNull("Should build handler", handler);
            List<ValidatorInfo> validatorsForRole = handler.getValidatorsForRole(new ArrayList<HeaderValue>());
            assertNotNull(validatorsForRole);
            assertEquals("Should get validator for default role", defaultRole, validatorsForRole.get(0).getRoles().get(0));
        }
    }
    
    public static class WhenWadlChanges {
        private ConfigurationService configService;
        private final String wadl = "default.wadl";
        private final String wadl1 = "default1.wadl";
        private final String role1 = "role1";
        private final String wadl2 = "default2.wadl";
        private final String role2 = "role2";
        private ApiValidatorHandlerFactory instance;
        private ValidatorInfo info1;
        private ValidatorInfo info2;
        
        @Before
        public void setup() {
            configService = mock(ConfigurationService.class);
            URL resource = this.getClass().getClassLoader().getResource(wadl);
            instance = new ApiValidatorHandlerFactory(configService, resource.getPath(), "");

            List<ValidatorInfo> validators = new ArrayList<ValidatorInfo>();
            info1 = mock(ValidatorInfo.class);
            when(info1.getUri()).thenReturn(instance.getWadlPath(wadl1));
            when(info1.getRoles()).thenReturn(Arrays.asList(role1));
            validators.add(info1);
            
            info2 = mock(ValidatorInfo.class);
            when(info2.getUri()).thenReturn(instance.getWadlPath(wadl2));
            when(info2.getRoles()).thenReturn(Arrays.asList(role2));
            validators.add(info2);

            instance.setValidators(validators);
        }
        
        @Test
        public void shouldClearMatchedValidator() throws MalformedURLException {
            String wadl2Path = new URL(instance.getWadlPath(wadl2)).toString();
            ConfigurationResource resource = mock(ConfigurationResource.class);
            when(resource.name()).thenReturn(wadl2Path);
            
            instance.getWadlListener().configurationUpdated(resource);
            
            verify(info1, times(0)).reinitValidator();
            verify(info2).reinitValidator();
        }
        
        @Test
        public void shouldClearAllValidatorsIfNoMatch() throws MalformedURLException {
            String wadl2Path = new URL(instance.getWadlPath("doesn'texist.wadl")).toString();
            ConfigurationResource resource = mock(ConfigurationResource.class);
            when(resource.name()).thenReturn(wadl2Path);
            
            instance.getWadlListener().configurationUpdated(resource);
            
            verify(info1).reinitValidator();
            verify(info2).reinitValidator();
        }
    }
}
