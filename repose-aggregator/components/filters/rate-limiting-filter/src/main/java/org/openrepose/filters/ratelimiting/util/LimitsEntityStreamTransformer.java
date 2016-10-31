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
package org.openrepose.filters.ratelimiting.util;

import org.openrepose.commons.utils.transform.StreamTransform;
import org.openrepose.commons.utils.transform.jaxb.JaxbToStreamTransform;
import org.openrepose.commons.utils.transform.xslt.StreamToXsltTransform;
import org.openrepose.core.services.ratelimit.config.Limits;
import org.openrepose.core.services.ratelimit.config.ObjectFactory;
import org.openrepose.filters.ratelimiting.util.combine.CombinedLimitsTransformer;
import org.openrepose.filters.ratelimiting.util.combine.LimitsTransformPair;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.InputStream;
import java.io.OutputStream;

public class LimitsEntityStreamTransformer {

    public static final String JSON_XSL_LOCATION = "/META-INF/xslt/limits-json.xsl";
    public static final String COMBINER_XSL_LOCATION = "/META-INF/xslt/limits-combine.xsl";
    private static final ObjectFactory LIMITS_OBJECT_FACTORY = new ObjectFactory();
    private final StreamTransform<LimitsTransformPair, OutputStream> combiner;
    private final StreamTransform<InputStream, OutputStream> jsonTransform;
    private final StreamTransform<JAXBElement<Limits>, OutputStream> entiyTransform;

    public LimitsEntityStreamTransformer() {
        this(buildJaxbContext());
    }

    public LimitsEntityStreamTransformer(JAXBContext context) {
        jsonTransform = new StreamToXsltTransform(
                TransformHelper.getTemplatesFromInputStream(
                        LimitsEntityStreamTransformer.class.getResourceAsStream(JSON_XSL_LOCATION)));

        combiner = new CombinedLimitsTransformer(
                TransformHelper.getTemplatesFromInputStream(
                        LimitsEntityStreamTransformer.class.getResourceAsStream(COMBINER_XSL_LOCATION)), context, LIMITS_OBJECT_FACTORY);

        entiyTransform = new JaxbToStreamTransform(context);
    }

    private static JAXBContext buildJaxbContext() {
        return TransformHelper.buildJaxbContext(LIMITS_OBJECT_FACTORY.getClass());
    }

    public void combine(LimitsTransformPair pair, OutputStream out) {
        combiner.transform(pair, out);
    }

    public void streamAsJson(InputStream in, OutputStream out) {
        jsonTransform.transform(in, out);
    }

    public void entityAsXml(Limits l, OutputStream output) {
        entiyTransform.transform(LIMITS_OBJECT_FACTORY.createLimits(l), output);
    }
}
