/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openrepose.common.auth.openstack;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.common.auth.openstack.AdminToken;

import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class AdminTokenTest {

   public static class WhenCachingToken {

      @Test
      public void shouldBeValidIfTokenNotExpired() {
         Calendar expires = Calendar.getInstance();
         // token expires 1 minute in the future
         expires.add(Calendar.MINUTE, 1);
         AdminToken token = new AdminToken("SomeToken", expires);

         assertTrue("Token should be valid", token.isValid());
      }

      @Test
      public void shouldBeInvalidIfTokenExpired() {
         Calendar expires = Calendar.getInstance();
         expires.add(Calendar.MINUTE, -1);
         AdminToken token = new AdminToken("SomeToken", expires);

         assertFalse("Token should be invalid", token.isValid());
      }

      @Test
      public void shouldBeInvalidIfExpiresNull() {
         AdminToken token = new AdminToken("SomeToken", null);

         assertFalse("Token should be invalid for null expires", token.isValid());
      }
   }
}
