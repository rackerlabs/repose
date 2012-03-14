package com.rackspace.papi.commons.util.transform;

import java.io.OutputStream;

public interface StreamTransform<S, T extends OutputStream> {

   void transform(S source, T target);
}
