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
package org.openrepose.cli.command.datastore.distributed;

import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.results.*;
import org.openrepose.commons.utils.encoding.UUIDEncodingProvider;
import org.openrepose.core.services.datastore.hash.MD5MessageDigestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

/**
 * @author zinic
 */
public class CacheKeyEncoder extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(CacheKeyEncoder.class);

    @Override
    public String getCommandDescription() {
        return "Encodes a cache key into a representation that the distributed datastore can address.";
    }

    @Override
    public String getCommandToken() {
        return "encode-key";
    }

    @Override
    public CommandResult perform(String[] arguments) {
        if (arguments.length != 1) {
            return new InvalidArguments("The cache key encoder expects one, string argument.");
        }

        try {
            final byte[] hashBytes = MD5MessageDigestFactory.getInstance().newMessageDigest().digest(arguments[0].getBytes(StandardCharsets.UTF_8));
            final String encodedCacheKey = UUIDEncodingProvider.getInstance().encode(hashBytes);

            return new MessageResult(encodedCacheKey);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            LOG.trace("JRE doesn't support MD5", noSuchAlgorithmException);
            return new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE,
                    "Your instance of the Java Runtime Environment does not support the MD5 hash algorithm.");
        }
    }
}
