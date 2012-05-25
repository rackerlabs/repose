package com.rackspace.papi.commons.config.resource.impl;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.ArrayUtilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteArrayConfigurationResource implements ConfigurationResource<ByteArrayConfigurationResource> {

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
