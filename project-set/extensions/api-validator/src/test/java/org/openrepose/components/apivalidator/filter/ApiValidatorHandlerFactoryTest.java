package org.openrepose.components.apivalidator.filter;

import com.rackspace.papi.commons.config.parser.generic.GenericResourceConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.service.config.ConfigurationService;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration;
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem;

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
            ValidatorConfiguration config = new ValidatorConfiguration();
            ValidatorItem item = new ValidatorItem();
            item.setWadl(wadl);
            item.setRole(role);
            config.getValidator().add(item);

            ValidatorItem defaultItem = new ValidatorItem();
            defaultItem.setWadl(wadl);
            defaultItem.setRole(defaultRole);
            defaultItem.setDefault(Boolean.TRUE);
            defaultItem.setDotOutput(dot);
            config.getValidator().add(defaultItem);
            
            configService = mock(ConfigurationService.class);
            URL resource = this.getClass().getClassLoader().getResource("");
            instance = new ApiValidatorHandlerFactory(configService, resource.getPath());
            
            instance.setValidatorCOnfiguration(config);
            instance.initialize();
            
            roles = new ArrayList<HeaderValue>();
            roles.add(new HeaderValueImpl(role));
        }
        
        @Test
        public void shouldBuildValidatorListAndSubscribeToWadl() {
            ApiValidatorHandler handler = instance.buildHandler();
            assertNotNull("Should build handler", handler);
            ValidatorInfo validatorForRole = handler.getValidatorForRole(roles);
            assertNotNull(validatorForRole);
            assertEquals("Should get validator for role", role, validatorForRole.getRole());
            verify(configService, times(2)).subscribeTo(eq(instance.getWadlPath(wadl)), any(ApiValidatorHandlerFactory.ApiValidatorWadlListener.class), any(GenericResourceConfigurationParser.class));
        }

        @Test
        public void shouldSetDefaultValidator() {
            ApiValidatorHandler handler = instance.buildHandler();
            assertNotNull("Should build handler", handler);
            ValidatorInfo validatorForRole = handler.getValidatorForRole(new ArrayList<HeaderValue>());
            assertNotNull(validatorForRole);
            assertEquals("Should get validator for default role", defaultRole, validatorForRole.getRole());
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
        private ValidatorInfo info2;
        private ValidatorInfo info1;
        
        @Before
        public void setup() {
            
            configService = mock(ConfigurationService.class);
            URL resource = this.getClass().getClassLoader().getResource(wadl);
            instance = new ApiValidatorHandlerFactory(configService, resource.getPath());

            List<ValidatorInfo> validators = new ArrayList<ValidatorInfo>();
            info1 = mock(ValidatorInfo.class);
            when(info1.getUri()).thenReturn(instance.getWadlPath(wadl1));
            when(info1.getRole()).thenReturn(role1);
            validators.add(info1);
            
            info2 = mock(ValidatorInfo.class);
            when(info2.getUri()).thenReturn(instance.getWadlPath(wadl2));
            when(info2.getRole()).thenReturn(role2);
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
