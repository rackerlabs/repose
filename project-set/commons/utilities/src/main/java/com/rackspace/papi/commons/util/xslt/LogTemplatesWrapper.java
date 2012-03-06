package com.rackspace.papi.commons.util.xslt;

import java.util.Properties;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;


public final class LogTemplatesWrapper implements Templates {

   private Templates templates;

   public LogTemplatesWrapper(Templates templates) {
      this.templates = templates;
   }

   public Properties getOutputProperties() {
      return templates.getOutputProperties();
   }

   public Transformer newTransformer() 
      throws TransformerConfigurationException {
      Transformer tr = templates.newTransformer();
      tr.setErrorListener (new LogErrorListener());
      return tr;
   }
}
