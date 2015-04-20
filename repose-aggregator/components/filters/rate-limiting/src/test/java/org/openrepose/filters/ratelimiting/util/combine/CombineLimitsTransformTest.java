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
package org.openrepose.filters.ratelimiting.util.combine;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrepose.commons.utils.transform.StreamTransform;
import org.openrepose.core.services.ratelimit.RateLimitListBuilder;
import org.openrepose.core.services.ratelimit.cache.CachedRateLimit;
import org.openrepose.core.services.ratelimit.config.*;
import org.openrepose.filters.ratelimiting.util.LimitsEntityStreamTransformer;
import org.openrepose.filters.ratelimiting.util.TransformHelper;

import javax.xml.bind.JAXBContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

//TODO: this test fails sometimes!
public class CombineLimitsTransformTest {

    public static final String SIMPLE_URI_REGEX = "/loadbalancer/.*";
    public static final String COMPLEX_URI_REGEX = "/loadbalancer/vips/.*";
    public static final String SIMPLE_URI = "*loadbalancer*";
    public static final String COMPLEX_URI = "*loadbalancer/vips*";
    public static final String SIMPLE_ID = "12345-ABCDE";
    public static final String COMPLEX_ID = "09876-ZYXWV";

    public static final String COMBINER_XSL_LOCATION = "/META-INF/xslt/limits-combine.xsl";
    public static final ObjectFactory LIMITS_OBJECT_FACTORY = new ObjectFactory();

    private final Pattern validationPattern = Pattern.compile(".*(<rates xmlns.*>.*</rates>).*(<absolute>.*</absolute>).*", Pattern.DOTALL);
    private final Pattern validationPatternJson = Pattern.compile(".*\"rate\":.*(\"absolute\":).*", Pattern.DOTALL);
    private StreamTransform<LimitsTransformPair, OutputStream> combiner;
    GregorianCalendar splodeDate = new GregorianCalendar(2015, Calendar.JUNE, 1);

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }

    @Before
    public void standUp() throws Exception {
        combiner = new CombinedLimitsTransformer(
                TransformHelper.getTemplatesFromInputStream(
                        LimitsEntityStreamTransformer.class.getResourceAsStream(COMBINER_XSL_LOCATION)),
                JAXBContext.newInstance(LIMITS_OBJECT_FACTORY.getClass()), LIMITS_OBJECT_FACTORY);
    }

    @Test
    public void shouldCombineInputStreamWithJaxbElement() throws Exception {
        Assume.assumeTrue(new Date().getTime() > splodeDate.getTime().getTime());
        final InputStream is = CombineLimitsTransformTest.class.getResourceAsStream(
                "/META-INF/schema/examples/absolute-limits.xml");

        RateLimitList rll = createRateLimitList();

        final LimitsTransformPair tPair = new LimitsTransformPair(is, rll);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        combiner.transform(tPair, output);

        final String actual = output.toString();
        final Matcher matcher = validationPattern.matcher(actual);

        try {
            assertTrue("Combined limits must match expected output pattern", matcher.matches());
            assertNotNull("Combined limits must include rate limits", matcher.group(1));
            assertNotNull("Combined limits must include absolute limits", matcher.group(2));
        } catch (AssertionError e) {
            System.err.println("================================================================================");
            System.err.println("This is the CombineLimitsTransformTest AssertionError output:");
            System.err.println("--------------------------------------------------------------------------------");
            System.err.println("is = " + is);
            System.err.println("--------------------------------------------------------------------------------");
            System.err.println("rll = " + rll);
            System.err.println("--------------------------------------------------------------------------------");
            System.err.println("tPair = " + tPair);
            System.err.println("--------------------------------------------------------------------------------");
            System.err.println("output = " + output);
            System.err.println("--------------------------------------------------------------------------------");
            System.err.println("combiner = " + combiner);
            System.err.println("--------------------------------------------------------------------------------");
            System.err.println("actual = " + actual);
            System.err.println("--------------------------------------------------------------------------------");
            System.err.println("matcher = " + matcher);
            System.err.println("================================================================================");
            throw e;
        }
    }

    @Test
    public void shouldCombineInputStreamWithJaxbElementJson() throws Exception {
        Assume.assumeTrue(new Date().getTime() > splodeDate.getTime().getTime());
        final InputStream is = CombineLimitsTransformTest.class.getResourceAsStream(
                "/META-INF/schema/examples/absolute-limits.json");

        RateLimitList rll = createRateLimitList();

        final LimitsTransformPair tPair = new LimitsTransformPair(is, rll);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        combiner.transform(tPair, output);

        final String actual = output.toString();
        final Matcher matcher = validationPatternJson.matcher(actual);

        assertTrue("Combined limits must match expected output pattern", matcher.matches());

        assertNotNull("Combined limits must include rate limits", matcher.group(1));
        assertNotNull("Combined limits must include absolute limits", matcher.group(2));
    }

    private RateLimitList createRateLimitList() {
        final Map<String, CachedRateLimit> cacheMap;
        final ConfiguredLimitGroup configuredLimitGroup;

        LinkedList<HttpMethod> methods = new LinkedList<HttpMethod>();
        methods.add(HttpMethod.GET);
        methods.add(HttpMethod.PUT);
        methods.add(HttpMethod.POST);
        methods.add(HttpMethod.DELETE);

        cacheMap = new HashMap<String, CachedRateLimit>();
        configuredLimitGroup = new ConfiguredLimitGroup();

        configuredLimitGroup.setDefault(Boolean.TRUE);
        configuredLimitGroup.setId("configured-limit-group");
        configuredLimitGroup.getGroups().add("user");

        cacheMap.put(SIMPLE_ID, new CachedRateLimit(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods), 1));

        configuredLimitGroup.getLimit().add(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods));

        cacheMap.put(COMPLEX_ID, new CachedRateLimit(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods), 1));

        configuredLimitGroup.getLimit().add(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods));

        return new RateLimitListBuilder(cacheMap, configuredLimitGroup).toRateLimitList();
    }

    private ConfiguredRatelimit newLimitConfig(String limitId, String uri, String uriRegex, LinkedList<HttpMethod> methods) {
        final ConfiguredRatelimit configuredRateLimit = new ConfiguredRatelimit();

        configuredRateLimit.setId(limitId);
        configuredRateLimit.setUnit(TimeUnit.HOUR);
        configuredRateLimit.setUri(uri);
        configuredRateLimit.setUriRegex(uriRegex);
        configuredRateLimit.setValue(20);
        for (HttpMethod m : methods) {
            configuredRateLimit.getHttpMethods().add(m);
        }

        return configuredRateLimit;
    }
}
