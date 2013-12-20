package com.rackspace.papi.commons.util.io.charset;

import org.slf4j.Logger;

import java.nio.charset.Charset;

/**
 *
 * @author malconis
 */
public final class CharacterSets {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CharacterSets.class);

   // Private final names of character sets
   private static final String UTF_8_CHARSET_NAME = "UTF-8";
   
   // Public instances of supported character sets
   public static final Charset UTF_8 = getCharset(UTF_8_CHARSET_NAME);

   // Hide the utility constructor
   private CharacterSets() {
   }
   
   /**
    * This method, by virtue of being used to assign static final variables, provides
    * the necessary runtime checks for character set support. This method will hard
    * fail the JVM (read: system exit) if a character set is requested but not
    * supported by the underlying operating system.
    * 
    * @param charsetName
    * @return 
    */
   public static Charset getCharset(String charsetName) {
      checkForCharacterSetSupport(charsetName);
      
      return Charset.forName(charsetName);
   }
   
   /**
    * This method will hard fail the JVM (read: system exit) if the specified
    * character set is requested but not supported by the underlying operating system.
    * 
    * @param charsetName 
    */
   public static void checkForCharacterSetSupport(String charsetName) {
      if (!Charset.isSupported(charsetName)) {
         LOG.error("The character set encoding " + charsetName + " is not avaiable "
                 + "on this system but is required to run Repose. This is a fatal "
                 + "system exception. Exiting.");
         
         System.exit(1);
      }
   }
}
