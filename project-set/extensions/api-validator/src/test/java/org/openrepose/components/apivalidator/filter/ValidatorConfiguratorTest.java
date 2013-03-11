package org.openrepose.components.apivalidator.filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem;

import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class ValidatorConfiguratorTest {

    public static class WhenConfiguringValidator {
        ValidatorConfigurator validatorConfigurator;
        ValidatorItem validatorItem;
        String configRoot;

        @Before
        public void setUp() throws Exception {
            validatorItem = new ValidatorItem();
            configRoot = "configRoot";
            validatorConfigurator = new ValidatorConfigurator(validatorItem, false, configRoot);
        }

        @Test
        public void validatorCheckerShouldAlwaysBeTrue() {
            assertTrue(validatorConfigurator.getConfiguration().getValidateChecker());
        }
    }
}
