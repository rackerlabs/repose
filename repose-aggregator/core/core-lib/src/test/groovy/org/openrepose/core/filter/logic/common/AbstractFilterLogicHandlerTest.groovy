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
package org.openrepose.core.filter.logic.common

import org.junit.Test
import org.openrepose.core.filter.logic.impl.SimplePassFilterDirector

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

/**
 * Created by eric7500 on 6/18/14.
 */
public class AbstractFilterLogicHandlerTest {

    @Test
    public void testHandleRequest() {
        AbstractFilterLogicHandler aflh = new AbstractFilterLogicHandler();
        assertThat(aflh.handleRequest(null, null), equalTo(SimplePassFilterDirector.SINGLETON_INSTANCE));
    }

    @Test
    public void testHandleResponse() {
        AbstractFilterLogicHandler aflh = new AbstractFilterLogicHandler();
        assertThat(aflh.handleResponse(null, null), equalTo(SimplePassFilterDirector.SINGLETON_INSTANCE));
    }
}