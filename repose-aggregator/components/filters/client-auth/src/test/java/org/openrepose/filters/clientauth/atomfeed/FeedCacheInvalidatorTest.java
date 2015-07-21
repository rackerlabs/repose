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
package org.openrepose.filters.clientauth.atomfeed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.datastore.Datastore;

import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FeedCacheInvalidatorTest {
    private Datastore datastore;

    private ListAppender app;

    @Before
    public void setUp() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        app = ((ListAppender) (ctx.getConfiguration().getAppender("List0"))).clear();
        datastore = mock(Datastore.class);
    }

    @Test
    public void shouldIncludeTraceInLog() throws Exception {
        FeedCacheInvalidator fci = FeedCacheInvalidator.openStackInstance(datastore, 1000);
        Thread t = new Thread(fci);
        t.start();
        Thread.sleep(2000);
        fci.done();
        assertThat(app.getMessages(), hasItem(matchesRegEx("GUID:\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12} - Beginning Feed Cache Invalidator Thread request.*")));
    }

    private Matcher<? super String> matchesRegEx(String regEx) {
        final Pattern pattern = Pattern.compile(regEx, Pattern.DOTALL);

        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return pattern.matcher(item).matches();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a string that matches regular expression: " + pattern);
            }
        };
    }
}
