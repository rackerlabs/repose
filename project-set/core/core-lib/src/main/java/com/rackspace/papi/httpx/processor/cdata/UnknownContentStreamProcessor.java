package com.rackspace.papi.httpx.processor.cdata;

import com.rackspace.papi.commons.util.io.InputStreamMerger;
import com.rackspace.papi.httpx.processor.common.InputStreamProcessor;
import com.rackspace.papi.httpx.processor.common.PreProcessorException;

import java.io.InputStream;

public class UnknownContentStreamProcessor implements InputStreamProcessor {

   private static final String UNKNOWN_PREFIX = "<xml><![CDATA[";
   private static final String UNKNOWN_SUFFIX = "]]></xml>";

   @Override
   public InputStream process(InputStream sourceStream) throws PreProcessorException {
      // TODO better way to "wrap" unknown data in an xml tag?
      return InputStreamMerger.merge(
              InputStreamMerger.wrap(UNKNOWN_PREFIX),
              sourceStream,
              InputStreamMerger.wrap(UNKNOWN_SUFFIX));
   }
}
