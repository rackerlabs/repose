package com.rackspace.papi.components.ratelimit.util.combine;

import com.rackspace.papi.commons.util.transform.StreamTransform;
import com.rackspace.papi.components.ratelimit.RateLimitTestContext;
import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.ObjectFactory;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.ratelimit.RateLimitListBuilder;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredLimitGroup;

import com.rackspace.papi.components.ratelimit.util.LimitsEntityStreamTransformer;
import com.rackspace.papi.components.ratelimit.util.TransformHelper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class CombineLimitsTransformJsonTest extends RateLimitTestContext {

    public static final String COMBINER_XSL_LOCATION = "/META-INF/xslt/limits-combine.xsl";
    public static final ObjectFactory LIMITS_OBJECT_FACTORY = new ObjectFactory();

    public static class WhenCombiningAbsoluteLimitsWithRateLimits {

        private final Pattern validationPattern = Pattern.compile(".*\"rate\":{.*(\"absolute\":{).*", Pattern.DOTALL);
        private StreamTransform<LimitsTransformPair, OutputStream> combiner;

        @Before
        public void standUp() throws Exception {
            combiner = new CombinedLimitsTransformer(
                    TransformHelper.getTemplatesFromInputStream(
                    LimitsEntityStreamTransformer.class.getResourceAsStream(COMBINER_XSL_LOCATION)),
                    JAXBContext.newInstance(LIMITS_OBJECT_FACTORY.getClass()), LIMITS_OBJECT_FACTORY);
        }
        @Test
        @Ignore
        public void shouldCombineInputStreamWithJaxbElement() throws Exception {
            final InputStream is = CombineLimitsTransformJsonTest.class.getResourceAsStream(
                    "/META-INF/schema/examples/absolute-limits.json");

            RateLimitList rll = createRateLimitList();

            final LimitsTransformPair tPair = new LimitsTransformPair(is, rll);
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            combiner.transform(tPair, output);

            final String actual = output.toString();
            final Matcher matcher = validationPattern.matcher(actual);

            assertTrue("Combined limits must match expected output pattern", matcher.matches());

            assertNotNull("Combined limits must include rate limits", matcher.group(1));
            assertNotNull("Combined limits must include absolute limits", matcher.group(2));
        }

        private RateLimitList createRateLimitList() {
            final Map<String, CachedRateLimit> cacheMap;
            final ConfiguredLimitGroup configuredLimitGroup;

            cacheMap = new HashMap<String, CachedRateLimit>();
            configuredLimitGroup = new ConfiguredLimitGroup();

            configuredLimitGroup.setDefault(Boolean.TRUE);
            configuredLimitGroup.setId("configured-limit-group");
            configuredLimitGroup.getGroups().add("user");

            cacheMap.put(SIMPLE_URI, newCachedRateLimitFor(SIMPLE_URI, SIMPLE_URI_REGEX, HttpMethod.GET, HttpMethod.PUT));

            configuredLimitGroup.getLimit().add(newLimitFor(SIMPLE_URI, SIMPLE_URI_REGEX, HttpMethod.GET));
            configuredLimitGroup.getLimit().add(newLimitFor(SIMPLE_URI, SIMPLE_URI_REGEX, HttpMethod.PUT));
            configuredLimitGroup.getLimit().add(newLimitFor(SIMPLE_URI, SIMPLE_URI_REGEX, HttpMethod.DELETE));
            configuredLimitGroup.getLimit().add(newLimitFor(SIMPLE_URI, SIMPLE_URI_REGEX, HttpMethod.POST));

            cacheMap.put(COMPLEX_URI_REGEX, newCachedRateLimitFor(COMPLEX_URI, COMPLEX_URI_REGEX, HttpMethod.GET, HttpMethod.PUT));

            configuredLimitGroup.getLimit().add(newLimitFor(COMPLEX_URI, COMPLEX_URI_REGEX, HttpMethod.GET));
            configuredLimitGroup.getLimit().add(newLimitFor(COMPLEX_URI, COMPLEX_URI_REGEX, HttpMethod.DELETE));
            configuredLimitGroup.getLimit().add(newLimitFor(COMPLEX_URI, COMPLEX_URI_REGEX, HttpMethod.PUT));
            configuredLimitGroup.getLimit().add(newLimitFor(COMPLEX_URI, COMPLEX_URI_REGEX, HttpMethod.POST));

            return new RateLimitListBuilder(cacheMap, configuredLimitGroup).toRateLimitList();
        }
    }

    public static String readStream(String resourceLocation) throws Exception {
        final StringBuilder stringBuffer = new StringBuilder();

        final BufferedReader in = new BufferedReader(new InputStreamReader(
                CombineLimitsTransformJsonTest.class.getResourceAsStream(resourceLocation)));

        String nextLine;

        while ((nextLine = in.readLine()) != null) {
            stringBuffer.append(nextLine);
        }

        return stringBuffer.toString();
    }
}
