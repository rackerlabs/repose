package com.rackspace.papi.commons.util.xslt;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

@RunWith(Enclosed.class)
public class LogErrorListenerTest {
   public static class WhenTransformingXSLWithErrors {

      public Templates getTestTemplate() throws Exception {
         return TemplatesFactory.instance().parseXslt(WhenTransformingXSLWithErrors.class.getResource("/message.xsl"));
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
