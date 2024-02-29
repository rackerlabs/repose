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
package org.openrepose.filters.apivalidator

import com.rackspace.com.papi.components.checker.Config
import com.rackspace.com.papi.components.checker.handler.*
import org.junit.Before
import org.junit.Test
import org.openrepose.filters.apivalidator.config.ValidatorConfiguration
import org.openrepose.filters.apivalidator.config.ValidatorItem
import scala.collection.JavaConversions

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

        DispatchResultHandler handlers = vldtrConfigurator.getHandlers(vItem, false, 0.0, true, "", "")
        def internalHandlers = JavaConversions.seqAsJavaList(handlers.handlers)
        assert internalHandlers.get(0) instanceof InstrumentedHandler
        assert internalHandlers.get(1) instanceof ApiCoverageHandler
    }

    @Test
    void whenIsDelegatingIsTrueThenADelegationAndMethodLoggerHandlerShouldBePresent() {
        ValidatorConfigurator vldtrConfigurator = new ValidatorConfigurator()
        ValidatorItem vItem = new ValidatorItem()

        DispatchResultHandler handlers = vldtrConfigurator.getHandlers(vItem, true, 0.9, true, "", "")
        def internalHandlers = JavaConversions.seqAsJavaList(handlers.handlers)
        assert internalHandlers.get(0) instanceof MethodLabelHandler
        assert internalHandlers.get(1) instanceof DelegationHandler
    }

    @Test
    void whenValidateCheckerIsFalseConfigShouldStoreFalse() {
        ValidatorItem vItem = new ValidatorItem();
        vItem.setValidateChecker(false);
        Config config = validatorConfigurator.createConfiguration(vItem, false, 1.0, false, "", "")
        assert !config.getValidateChecker();
    }

    @Test
    void whenCheckXsdGrammarIsTrueConfigJsonShouldBeFalse() {
        ValidatorItem vItem = new ValidatorItem();
        vItem.checkXsdGrammar = true;
        Config config = validatorConfigurator.createConfiguration(vItem, false, 1.0, false, "", "")
        assert config.getCheckXSDGrammar();
        assert !config.getCheckJSONGrammar();
    }

    @Test
    void whenCheckGrammarsIsTrueConfigBothShouldBeTrue() {
        ValidatorItem vItem = new ValidatorItem();
        vItem.checkGrammars = true;
        Config config = validatorConfigurator.createConfiguration(vItem, false, 1.0, false, "", "")
        assert config.getCheckXSDGrammar();
        assert config.getCheckJSONGrammar();
    }

    @Test
    void whenDisableSaxonByteCodeGenIsTrueConfigShouldStoreTrue() {
        ValidatorItem vItem = new ValidatorItem();
        vItem.disableSaxonByteCodeGen = true;
        Config config = validatorConfigurator.createConfiguration(vItem, false, 1.0, false, "", "")
        assert config.getDisableSaxonByteCodeGen();
    }

    @Test
    void whenDisableSaxonByteCodeGenNotSetConfigShouldStoreFalse() {
        ValidatorItem vItem = new ValidatorItem();
        Config config = validatorConfigurator.createConfiguration(vItem, false, 1.0, false, "", "")
        assert !config.getDisableSaxonByteCodeGen();
    }

    static String getFilePath(URL path) {
        int d = path.getPath().lastIndexOf("/")

        return path.getPath().substring(0, d);
    }
}
