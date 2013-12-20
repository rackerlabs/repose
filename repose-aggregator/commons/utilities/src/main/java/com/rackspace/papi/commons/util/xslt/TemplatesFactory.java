package com.rackspace.papi.commons.util.xslt;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public final class TemplatesFactory {

   public static TemplatesFactory instance() {
      return TEMPLATES_FACTORY_INSTANCE;
   }
   private static final TemplatesFactory TEMPLATES_FACTORY_INSTANCE = new TemplatesFactory();
   private static final TransformerFactory XSLT_TRANSFORMER_FACTORY = TransformerFactory.newInstance();

   static {
      XSLT_TRANSFORMER_FACTORY.setErrorListener(new LogErrorListener());
   }

   private TemplatesFactory() {
   }

   public Templates parseXslt(InputStream is) throws TransformerConfigurationException {
      return parseXslt(new StreamSource(is));
   }

   public Templates parseXslt(URL location) throws TransformerConfigurationException, IOException {
      return parseXslt(location.openStream());
   }

   //TODO: Verify that the factory is thread safe
   public synchronized Templates parseXslt(Source s) throws TransformerConfigurationException {
      return new LogTemplatesWrapper(XSLT_TRANSFORMER_FACTORY.newTemplates(s));
   }
}
