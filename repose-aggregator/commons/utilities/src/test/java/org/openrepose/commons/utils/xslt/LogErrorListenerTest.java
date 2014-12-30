package org.openrepose.commons.utils.xslt;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

@RunWith(Enclosed.class)
public class LogErrorListenerTest {
   public static class WhenTransformingXSLWithErrors {
      private static final TransformerFactory XSLT_TRANSFORMER_FACTORY =
              TransformerFactory.newInstance("javax.xml.transform.TransformerFactory", WhenTransformingXSLWithErrors.class.getClassLoader());

      static {
         XSLT_TRANSFORMER_FACTORY.setErrorListener(new LogErrorListener());
      }

      private static Templates parseXslt(Source s) throws TransformerConfigurationException {
         synchronized (XSLT_TRANSFORMER_FACTORY) {
            return new LogTemplatesWrapper(XSLT_TRANSFORMER_FACTORY.newTemplates(s));
         }
      }

      public Templates getTestTemplate() throws Exception {
         return WhenTransformingXSLWithErrors.parseXslt(new StreamSource(WhenTransformingXSLWithErrors.class.getResource("/message.xsl").openStream()));
      }

      @Test
      public void shouldLogWarningOnWarnXML() throws Exception {
         //
         //  Note that the assertion in this test is checked by the
         //  LogErrorListenerAppender, it will throw as AssertionError
         //  if the Warning is logged correctly.
         //
         Templates t = getTestTemplate();
         Transformer tr = t.newTransformer();
         tr.transform (new StreamSource (WhenTransformingXSLWithErrors.class.getResourceAsStream("/warn.xml")), 
                       new StreamResult (System.err));
      }

      @Test(expected = RuntimeException.class)
      public void shouldThrowRuntimeExceptionOnFailXML() throws Exception {
         Templates t = getTestTemplate();
         Transformer tr = t.newTransformer();
         tr.transform (new StreamSource (WhenTransformingXSLWithErrors.class.getResourceAsStream("/fail.xml")),
                       new StreamResult (System.err));
      }
   }
}
