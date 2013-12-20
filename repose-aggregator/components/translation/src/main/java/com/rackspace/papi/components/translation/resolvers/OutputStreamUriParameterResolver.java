package com.rackspace.papi.components.translation.resolvers;

import net.sf.saxon.lib.OutputURIResolver;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.util.UriUtils;

public class OutputStreamUriParameterResolver implements OutputURIResolver {

  public static final String PREFIX = "repose:output:";
  private final Map<String, OutputStream> streams = new HashMap<String, OutputStream>();
  private final OutputURIResolver parent;

  public OutputStreamUriParameterResolver() {
    this.parent = null;
  }

  public OutputStreamUriParameterResolver(OutputURIResolver parent) {
    this.parent = parent;
  }

  public void clearStreams() {
    streams.clear();
  }

  public String addStream(OutputStream outputStreamReference, String name) {
    String key = getHref(name);
    streams.put(key, outputStreamReference);
    return key;
  }

  public String getHref(String name) {
    try {
      return PREFIX + UriUtils.encodePathSegment(name, "utf-8");
    } catch (UnsupportedEncodingException ex) {
      return PREFIX + name;
    }
  }

  private static class ResourceNotFoundException extends RuntimeException {

    ResourceNotFoundException(String message) {
      super(message);
    }
  }

  @Override
  public Result resolve(String href, String base) throws TransformerException {
    OutputStream stream = streams.get(href);
    if (stream != null) {
      StreamResult result = new StreamResult(stream);
      try {
        result.setSystemId(new URI(href).toString());
      } catch (URISyntaxException ex) {
      }

      return result;
    }

    if (parent != null && href != null && !href.startsWith("reference")) {
      return parent.resolve(href, base);
    }

    throw new ResourceNotFoundException("Failed to resolve href: " + href);
  }

  @Override
  public void close(Result result) throws TransformerException {
    try {
      ((StreamResult) result).getOutputStream().close();
    } catch (IOException ex) {
      throw new TransformerException(ex);
    }
  }
}
