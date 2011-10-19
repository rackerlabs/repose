package com.rackspace.papi.components.translation;

import java.io.InputStream;
import java.io.OutputStream;

public interface Transformer {
    public void transform(InputStream inputStream, InputStream transformationFile, OutputStream outputStream);
}
