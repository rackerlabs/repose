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
package org.openrepose.commons.utils.regex;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author zinic
 */
public class RegexSelectorTest {

    private RegexSelector<String> selector;

    @Before
    public final void beforeAll() {
        selector = new RegexSelector<>();
        selector.addPattern("\\d\\d\\d[+-]", "notExpected");
        selector.addPattern("[+-]\\d\\d\\d", "expected");
    }

    @Test
    public void shouldSelectOnRegexMatch() {
        final SelectorResult<String> foundResult = selector.select("-124");

        assertTrue("Selector must have a key.", foundResult.hasKey());
        assertEquals("Selector should select expected key.", "expected", foundResult.getKey());
    }

    @Test
    public void shouldReturnEmptyResultWithNoMatch() {
        final SelectorResult<String> emptyResult = selector.select("expected");

        assertFalse("Selector must not have a key.", emptyResult.hasKey());
        assertNull("Selector should select expected key.", emptyResult.getKey());
    }

    @Test
    public void shouldClearStoredSelectors() {
        final SelectorResult<String> foundResult = selector.select("-124");

        assertTrue("Selector must have a key.", foundResult.hasKey());
        assertEquals("Selector should select expected key.", "expected", foundResult.getKey());

        selector.clear();

        final SelectorResult<String> emptySelection = selector.select("-124");

        assertFalse("Selector must not have a key.", emptySelection.hasKey());
        assertNull("Selector should select expected key.", emptySelection.getKey());
    }
}
