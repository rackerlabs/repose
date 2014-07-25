package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.util.StringUtilities;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public class HrefFileReader {
   private static final Logger LOG = LoggerFactory.getLogger(HrefFileReader.class);

   private static final Pattern URI_PATTERN = Pattern.compile(":\\/\\/");

   //TODO:Enhancement Update the service to use a uri resolver
   public String read(String href, String hrefId) {
                  
      final File f = validateHref(href, hrefId);

      String stringMessage = "";
      if (f != null) {
         try {
            stringMessage = FileUtils.readFileToString(f, Charset.defaultCharset());
         } catch (IOException ioe) {
            LOG.error(StringUtilities.join("Failed to read file: ", f.getAbsolutePath(), " - Reason: ", ioe.getMessage()), ioe);
         }
      }

      return stringMessage;
   }

   public File validateHref(String href, String hrefId) {
      
      final Matcher m = URI_PATTERN.matcher(href);
      File f = null;

      if (m.find() && href.startsWith("file://")) {
         try {
            f = new File(new URI(href));
         } catch (URISyntaxException urise) {
            LOG.error("Bad URI syntax in message href for status code: " + hrefId, urise);
         }
      }

      return f;
   }
}
