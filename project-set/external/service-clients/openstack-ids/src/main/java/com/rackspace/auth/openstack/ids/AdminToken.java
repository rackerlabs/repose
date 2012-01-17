package com.rackspace.auth.openstack.ids;

import java.util.Calendar;

public class AdminToken {
      private final String token;
      private final Calendar expires;

      public AdminToken(String token, Calendar expires) {
         this.token = token;
         this.expires = expires;
      }

      public String getToken() {
         return token;
      }

      public boolean isValid() {
         boolean valid = expires != null && !expires.getTime().before(Calendar.getInstance().getTime());
         return valid;
      }
   
}
