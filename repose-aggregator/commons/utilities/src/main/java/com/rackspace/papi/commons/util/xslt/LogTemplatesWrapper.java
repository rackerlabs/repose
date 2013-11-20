package com.rackspace.papi.commons.util.xslt;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import java.util.Properties;


public final class LogTemplatesWrapper implements Templates {

   private Templates templates;

   public LogTemplatesWrapper(Templates templates) {
      this.templates = templates;
   }

   @Override
   public Properties getOutputProperties() {
      return templates.getOutputProperties();
   }

   @Override
   public Transformer newTransformer() 
      throws TransformerConfigurationException {
      Transformer tr = templates.newTransformer();
      tr.setErrorListener (new LogErrorListener());
      return tr;
   }
}
