package com.rackspace.auth.rackspace;

import com.rackspace.auth.AuthToken;
import com.rackspacecloud.docs.auth.api.v1.FullToken;

import java.io.Serializable;

/**
 * @author fran
 */
public class RackspaceToken extends AuthToken implements Serializable {
   private final String accountId;
   private final String userId;
   private final String tokenId;
   private final long expires;

   public RackspaceToken(String accountId, FullToken token) {
      if (token == null || token.getExpires() == null) {
         throw new IllegalArgumentException("Invalid token");
      }
      
      this.accountId = accountId;
      this.userId = token.getUserId();
      this.tokenId = token.getId();
      this.expires = token.getExpires().toGregorianCalendar().getTimeInMillis();
   }
   

   @Override
   public String getTenantId() {
      return accountId;
   }
   
   @Override
   public String getTenantName(){
      return accountId;
   }

   @Override
   public String getUserId() {
      return userId;
   }

   @Override
   public String getTokenId() {
      return tokenId;
   }

   @Override
   public long getExpires() {
      return expires;
   }

   @Override
   public String getUsername() {
      throw new UnsupportedOperationException("The Rackspace Auth 1.1 Token does not provide a username.");
   }

   @Override
   public String getRoles() {
      throw new UnsupportedOperationException("The Rackspace Auth 1.1 Token does not provide roles.");
   }

    @Override
    public String getImpersonatorTenantId() {
        return "";
    }

    @Override
    public String getImpersonatorUsername() {
        return "";
    }
}
