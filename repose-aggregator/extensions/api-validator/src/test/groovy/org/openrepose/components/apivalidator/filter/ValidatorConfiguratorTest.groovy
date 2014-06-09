package org.openrepose.components.apivalidator.filter

import com.rackspace.com.papi.components.checker.handler.InstrumentedHandler
import com.rackspace.com.papi.components.checker.handler.ResultHandler
import org.junit.Before
import org.junit.Test
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem


class ValidatorConfiguratorTest {

    ValidatorConfiguration cnf = new ValidatorConfiguration()
    ValidatorConfigurator validatorConfigurator
    URL resource
    String wadl = "default.wadl";

    @Before
    void setUp() {
        resource = this.getClass().getClassLoader().getResource(wadl)
        cnf = new ValidatorConfiguration()
        ValidatorItem v1 = new ValidatorItem()
        v1.setDefault(true)
        v1.setWadl(wadl)
        v1.setApiCoverage(true)
        ValidatorItem v2 = new ValidatorItem()
        v2.setWadl(wadl)
        cnf.getValidator().add(v1)
        cnf.getValidator().add(v2)
        validatorConfigurator = new ValidatorConfigurator();
    }

    @Test
    void whenMultiMatchIsTrueThenPreserveRequestBodyShouldBeTrue() {
        cnf.setMultiRoleMatch(true)
        validatorConfigurator.processConfiguration(cnf, getFilePath(resource), wadl)
        for (ValidatorInfo info : validatorConfigurator.getValidators()) {
            assert info.getValidator().config().preserveRequestBody
        }
    }

    @Test
    void testProcessConfiguration() {
        cnf.setMultiRoleMatch(false)
        validatorConfigurator.processConfiguration(cnf, getFilePath(resource), wadl)
        for (ValidatorInfo info : validatorConfigurator.getValidators()) {
            assert !info.getValidator().config().preserveRequestBody
        }
    }

    @Test
    void whenApiCoverageIsTrueThenAnInstrumentedHandlerShouldBePresent() {
        validatorConfigurator.processConfiguration(cnf, getFilePath(resource), wadl)

        int instrumentedHandlerPresent = 0
        for (ValidatorInfo info : validatorConfigurator.getValidators()) {
            for (int i = 0; i < info.getValidator().config().getResultHandler().handlers.length; ++i) {
                if (info.getValidator().config().getResultHandler().handlers[i] instanceof InstrumentedHandler) {
                    ++instrumentedHandlerPresent
                }
            }
        }
        assert instrumentedHandlerPresent == 1
    }

    static String getFilePath(URL path) {
        int d = path.getPath().lastIndexOf("/")

        return path.getPath().substring(0, d);
    }
}
