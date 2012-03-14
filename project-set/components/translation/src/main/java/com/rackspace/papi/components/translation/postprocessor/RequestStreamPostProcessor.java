package com.rackspace.papi.components.translation.postprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class RequestStreamPostProcessor implements InputStreamPostProcessor {
   private static final SAXTransformerFactory handlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
   private static final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
   private static final String DEFAULT_XSL = "request-post-process.xsl";
   private final Templates templates;
   
   static {
      parserFactory.setValidating(true);
      parserFactory.setNamespaceAware(true);
   }
   
   public RequestStreamPostProcessor() throws PostProcessorException {
      this(DEFAULT_XSL);
   }
   
   public RequestStreamPostProcessor(String xsltName) throws PostProcessorException {
      try {
         InputStream xsltStream = RequestStreamPostProcessor.class.getResourceAsStream(xsltName);
         templates = handlerFactory.newTemplates(new StreamSource(xsltStream));
      } catch (TransformerConfigurationException ex) {
         throw new PostProcessorException(ex);
      }
   }
   
   public RequestStreamPostProcessor(Templates templates) {
      this.templates = templates;
   }

   @Override
   public InputStream process(Source node) throws PostProcessorException {
      try {
         final PipedInputStream resultStream = new PipedInputStream();
         final PipedOutputStream out = new PipedOutputStream(resultStream);
         
         templates.newTransformer().transform(node, new StreamResult(out) );
         
         return resultStream;
         
      } catch (TransformerException ex) {
         // TODO: Should we log the exception here?
         throw new PostProcessorException(ex);
      } catch (IOException ex) {
         // TODO: Should we log the exception here?
         throw new PostProcessorException(ex);
      }
   }

}
