package com.rackspace.papi.commons.util.io.charset;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.security.Permission;

/**
 *
 * @author malconis
 */
@RunWith(Enclosed.class)
public class CharacterSetSupportTest extends TestCase {

   public static class WhenCheckingForCharacterSets {

      @Test(expected = RuntimeException.class)
      public void shouldCallSystemExitWhenCharacterSetIsUnsupported() {
         final SecurityManager catchManager = new SecurityManager() {

            @Override
            public void checkPermission(Permission prmsn) {
               if (prmsn.getName().contains("exitVM")) {
                  throw new RuntimeException();
               }
            }
         };

         final SecurityManager originalManager = System.getSecurityManager();
         final String unsupportedCharSet = "UFT-33";

         try {
            System.setSecurityManager(catchManager);
            CharacterSets.checkForCharacterSetSupport(unsupportedCharSet);
         } finally {
            System.setSecurityManager(originalManager);
         }
      }
   }
}
