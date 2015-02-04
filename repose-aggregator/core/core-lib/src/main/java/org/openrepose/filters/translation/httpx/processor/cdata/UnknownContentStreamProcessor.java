package org.openrepose.filters.translation.httpx.processor.cdata;

import org.openrepose.commons.utils.io.InputStreamMerger;
import org.openrepose.filters.translation.httpx.processor.common.InputStreamProcessor;
import org.openrepose.filters.translation.httpx.processor.common.PreProcessorException;

import java.io.InputStream;

public class UnknownContentStreamProcessor implements InputStreamProcessor {

   private static final String UNKNOWN_PREFIX = "<httpx:unknown-content xmlns:httpx=\"http://docs.openrepose.org/repose/httpx/v1.0\"><![CDATA[";
   private static final String UNKNOWN_SUFFIX = "]]></httpx:unknown-content>";

   @Override
   public InputStream process(InputStream sourceStream) throws PreProcessorException {
      // TODO better way to "wrap" unknown data in an xml tag?
      return InputStreamMerger.merge(
              InputStreamMerger.wrap(UNKNOWN_PREFIX),
              sourceStream,
              InputStreamMerger.wrap(UNKNOWN_SUFFIX));
   }
}
