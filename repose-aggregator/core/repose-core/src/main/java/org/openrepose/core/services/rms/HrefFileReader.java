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
package org.openrepose.core.services.rms;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public class HrefFileReader {
    private static final Logger LOG = LoggerFactory.getLogger(HrefFileReader.class);

    private static final Pattern URI_PATTERN = Pattern.compile(":\\/\\/");

    //TODO:Enhancement Update the service to use a uri resolver
    public String read(String href, String hrefId) {

        final File f = validateHref(href, hrefId);

        String stringMessage = "";
        if (f != null) {
            try {
                stringMessage = FileUtils.readFileToString(f, Charset.defaultCharset());
            } catch (IOException ioe) {
                LOG.error(StringUtils.join("Failed to read file: ", f.getAbsolutePath(), " - Reason: ", ioe.getMessage()), ioe);
            }
        }

        return stringMessage;
    }

    public File validateHref(String href, String hrefId) {

        final Matcher m = URI_PATTERN.matcher(href);
        File f = null;

        if (m.find() && href.startsWith("file://")) {
            try {
                f = new File(new URI(href));
            } catch (URISyntaxException urise) {
                LOG.error("Bad URI syntax in message href for status code: " + hrefId, urise);
            }
        }

        return f;
    }
}
