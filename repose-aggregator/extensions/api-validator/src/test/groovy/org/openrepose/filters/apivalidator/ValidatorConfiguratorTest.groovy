package org.openrepose.filters.apivalidator

import com.rackspace.com.papi.components.checker.handler.InstrumentedHandler
import org.junit.Before
import org.junit.Test
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem
import org.openrepose.filters.apivalidator.DispatchHandler
import org.openrepose.filters.apivalidator.ValidatorConfigurator
import org.openrepose.filters.apivalidator.ValidatorInfo


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
        ValidatorItem v2 = new ValidatorItem()
        v2.setWadl(wadl)
        cnf.getValidator().add(v1)
        cnf.getValidator().add(v2)
        validatorConfigurator = new ValidatorConfigurator()
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
        ValidatorConfigurator vldtrConfigurator = new ValidatorConfigurator()
        ValidatorItem vItem = new ValidatorItem()
        vItem.setEnableApiCoverage(true)

        DispatchHandler handlers = vldtrConfigurator.getHandlers(vItem, true, "")
        assert handlers.handlers[0] instanceof InstrumentedHandler
    }

    static String getFilePath(URL path) {
        int d = path.getPath().lastIndexOf("/")

        return path.getPath().substring(0, d);
    }
}
