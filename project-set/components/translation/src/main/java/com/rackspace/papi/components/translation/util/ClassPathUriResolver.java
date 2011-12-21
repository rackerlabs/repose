package com.rackspace.papi.components.translation.util;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class ClassPathUriResolver implements URIResolver {
   private static final String CLASSPATH_PREFIX = "classpath:";
   
   public Source resolve(String href, String base) throws TransformerException {
      
      if (href != null && href.toLowerCase().startsWith(CLASSPATH_PREFIX)) {
         String path = href.substring(CLASSPATH_PREFIX.length());
         return new StreamSource(getClass().getResourceAsStream(path));
      }
      
      return null;
   }
   
}
