package org.openrepose.components.xsdvalidator.filter;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.handler.ServletResultHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ValidatorInfoTest {

    public static class WhenLoadingValidators {

        private final String role = "someRole";
        private final String wadl = "default.wadl";
        private Config config;
        private ValidatorInfo instance;

        private DispatchHandler getHandlers() {
            List<ResultHandler> handlers = new ArrayList<ResultHandler>();
            handlers.add(new ServletResultHandler());
            return new DispatchHandler(handlers.toArray(new ResultHandler[0]));
        }

        @Before
        public void setup() {
            this.config = new Config();
            config.setResultHandler(getHandlers());
            config.setUseSaxonEEValidation(false);
            config.setCheckWellFormed(true);
            config.setCheckXSDGrammar(true);
            config.setCheckElements(true);
            URL resource = this.getClass().getClassLoader().getResource(wadl);

            this.instance = new ValidatorInfo(role, resource.toExternalForm(), config);
        }

        @Test
        public void shouldCreateValidatorOnce() {
            Validator validator = instance.getValidator();
            assertNotNull(validator);
            Validator validator1 = instance.getValidator();
            assertNotNull(validator1);

            assertTrue("Should return exact same validator on each call to getValidator", validator1 == validator);

            instance.clearValidator();
            Validator validator2 = instance.getValidator();
            assertNotNull(validator2);
            assertTrue("Validator2 should be a new instance after clearing", validator2 != validator);

        }
    }
}
