package com.rackspace.papi.commons.util.transform;

import java.io.InputStream;
import java.io.OutputStream;

public interface Transformer {
    public void transform(InputStream inputStream, String transformationFile, OutputStream outputStream);
}
