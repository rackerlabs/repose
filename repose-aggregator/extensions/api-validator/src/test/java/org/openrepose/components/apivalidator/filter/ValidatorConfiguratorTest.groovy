package org.openrepose.components.apivalidator.filter

import org.junit.Before
import org.junit.Test
import org.openrepose.components.apivalidator.servlet.config.BaseValidatorConfiguration
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration2
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem2


class ValidatorConfiguratorTest {

    BaseValidatorConfiguration cnf = new ValidatorConfiguration2()
    ValidatorConfigurator validatorConfigurator
    URL resource
    String wadl = "default.wadl";

    @Before
    void setUp() {
        resource = this.getClass().getClassLoader().getResource(wadl)
        cnf = new ValidatorConfiguration2()
        ValidatorItem2 v1 = new ValidatorItem2()
        v1.setDefault(true)
        v1.setWadl(wadl)
        ValidatorItem2 v2 = new ValidatorItem2()
        v2.setWadl(wadl)
        cnf.getValidator().add(v1)
        cnf.getValidator().add(v2)
        validatorConfigurator = ValidatorConfigurator.createValidatorConfigurator(cnf);
    }

    @Test
    void whenMultiMatchIsTrueThenPreserveRequestBodyShouldBeTrue() {

        cnf.setMultiRoleMatch(true)
        validatorConfigurator.processConfiguration(cnf, getFilePath(resource), wadl)
        for (ValidatorInfo info : validatorConfigurator.getValidators()) {

            assert info.getValidator().config().preserveRequestBody == true
        }
    }

    @Test
    void testProcessConfiguration() {

        cnf.setMultiRoleMatch(false)
        validatorConfigurator.processConfiguration(cnf, getFilePath(resource), wadl)
        for (ValidatorInfo info : validatorConfigurator.getValidators()) {

            assert info.getValidator().config().preserveRequestBody == false
        }
    }


    String getFilePath(URL path) {

        int d = path.getPath().lastIndexOf("/")

        return path.getPath().substring(0, d);
    }
}
