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
package org.openrepose.commons.config.resource.impl;

import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.ArrayUtilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteArrayConfigurationResource implements ConfigurationResource {

    private final byte[] sourceArray;
    private final String name;

    public ByteArrayConfigurationResource(String name, byte[] sourceArray) {
        this.sourceArray = ArrayUtilities.nullSafeCopy(sourceArray);
        this.name = name;
    }

    @Override
    public boolean exists() throws IOException {
        return true;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return new ByteArrayInputStream(sourceArray);
    }

    @Override
    public boolean updated() throws IOException {
        return false;
    }
}
