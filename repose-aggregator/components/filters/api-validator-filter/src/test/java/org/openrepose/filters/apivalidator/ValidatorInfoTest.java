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

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.handler.DispatchResultHandler;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.handler.ServletResultHandler;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ValidatorInfoTest {

    private final List<String> roles = new ArrayList<>();
    private final String wadl = "default.wadl";
    private final String name = "testName";
    private Config config;
    private ValidatorInfo instance;
    private ValidatorInfo instance2;

    private DispatchResultHandler getHandlers() {
        List<ResultHandler> handlers = new ArrayList<>();
        handlers.add(new ServletResultHandler());
        return new DispatchResultHandler(scala.collection.JavaConversions.asScalaBuffer(handlers).toList());
    }

    @Before
    public void setup() {
        this.config = new Config();
        config.setResultHandler(getHandlers());
        config.setUseSaxonEEValidation(false);
        config.setCheckWellFormed(true);
        config.setCheckXSDGrammar(true);
        config.setCheckElements(true);
        roles.add("someRole");
        roles.add("someRole2");
        URL resource = this.getClass().getClassLoader().getResource(wadl);

        this.instance = new ValidatorInfo(roles, resource.toExternalForm(), config, null);
        this.instance2 = new ValidatorInfo(roles, resource.toExternalForm(), config, name);
    }

    @Test
    public void shouldCreateValidatorOnce() {
        Validator validator = instance.getValidator();
        assertNotNull(validator);
        Validator validator1 = instance.getValidator();
        assertNotNull(validator1);

        assertThat("Should return exact same validator on each call to getValidator", validator1, sameInstance(validator));

        instance.clearValidator();
        Validator validator2 = instance.getValidator();
        assertNotNull(validator2);
        assertThat("Validator2 should be a new instance after clearing", validator2, not(sameInstance(validator)));
    }

    @Test
    public void shouldGenerateValidatorNameWhenPassedNull() {
        assertEquals(instance.getName(), instance.getNameFromRoles(instance.getRoles()));
    }

    @Test
    public void shouldGenerateValidatorNameWhenProvided() {
        assertEquals(instance2.getName(), name);
    }

    @Test
    public void shouldNotHaveForbiddenCharsInValidatorName() {
        roles.add("role/with/slashes");
        roles.add("role,with,commas");
        roles.add("role=with=equals");
        roles.add("role:with:colons");
        roles.add("role*with*asterisks");
        roles.add("role?with?question?marks");
        roles.add("role with spaces");
        roles.add("role\u00A0with\u00A0non-breaking\u00A0spaces");
        String goodName = instance.getNameFromRoles(roles);
        assertThat(goodName, not(containsString("/")));
        assertThat(goodName, not(containsString(",")));
        assertThat(goodName, not(containsString("=")));
        assertThat(goodName, not(containsString(":")));
        assertThat(goodName, not(containsString("*")));
        assertThat(goodName, not(containsString("?")));
        assertThat(goodName, not(containsString(" ")));
        assertThat(goodName, not(containsString("\u00A0")));
    }
}
