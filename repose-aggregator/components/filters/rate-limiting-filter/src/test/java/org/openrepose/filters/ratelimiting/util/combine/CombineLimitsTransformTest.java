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

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.transform.StreamTransform;
import org.openrepose.core.services.ratelimit.RateLimitListBuilder;
import org.openrepose.core.services.ratelimit.cache.CachedRateLimit;
import org.openrepose.core.services.ratelimit.config.*;
import org.openrepose.filters.ratelimiting.util.LimitsEntityStreamTransformer;
import org.openrepose.filters.ratelimiting.util.TransformHelper;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class CombineLimitsTransformTest {

    public static final String SIMPLE_URI_REGEX = "/loadbalancer/.*";
    public static final String COMPLEX_URI_REGEX = "/loadbalancer/vips/.*";
    public static final String SIMPLE_URI = "*loadbalancer*";
    public static final String COMPLEX_URI = "*loadbalancer/vips*";
    public static final String SIMPLE_ID = "12345-ABCDE";
    public static final String COMPLEX_ID = "09876-ZYXWV";

    public static final String COMBINER_XSL_LOCATION = "/META-INF/xslt/limits-combine.xsl";
    public static final ObjectFactory LIMITS_OBJECT_FACTORY = new ObjectFactory();

    private StreamTransform<LimitsTransformPair, OutputStream> combiner;
    private DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    @Before
    public void standUp() throws Exception {
        combiner = new CombinedLimitsTransformer(
                TransformHelper.getTemplatesFromInputStream(
                        LimitsEntityStreamTransformer.class.getResourceAsStream(COMBINER_XSL_LOCATION)),
                JAXBContext.newInstance(LIMITS_OBJECT_FACTORY.getClass()), LIMITS_OBJECT_FACTORY);
    }

    @Test
    public void shouldCombineInputStreamWithJaxbElement() throws Exception {
        final InputStream is = CombineLimitsTransformTest.class.getResourceAsStream(
                "/META-INF/schema/examples/absolute-limits.xml");

        RateLimitList rll = createRateLimitList();

        final LimitsTransformPair tPair = new LimitsTransformPair(is, rll);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        combiner.transform(tPair, output);

        final String actual = output.toString();

        Document doc = getDocument(actual);
        XPath xpath = getxPath();

        assertNotNull("Combined limits must include rate limits", xpath.compile("/limits/rates").evaluate(doc, XPathConstants.NODE));
        assertNotNull("Combined limits must include absolute limits", xpath.compile("/limits/absolute").evaluate(doc, XPathConstants.NODE));
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

    private XPath getxPath() {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        return xPathfactory.newXPath();
    }

    private Document getDocument(String actual) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(actual.getBytes()));
    }

}
