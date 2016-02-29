The Flush Output Filter is used to flush any buffered data to the output stream.

This can be a good idea when using 3rd party filters, when it may be useful to
force repose to output response data to the output stream.

This happens in FlushOutputHandler. Whenever handleResponse is called,
the request and response are wrapped into a MutableHttpServletResponse.

commitBufferToServletOutputStream is then invoked on the Mutable Response,
forcing all buffered response data to the output stream.

 Here is an example of the filter in the filter chain (part of system-model.cfg.xml):

    <filters>
      <filter name="http-logging" />
      <filter name="flush-output" />
      <filter name="default-router"/>
    </filters>

This filter doesn't return any response codes or create any request headers, it simple
flushes data and passes through the request to the next filter (or origin service if it's the last one)
