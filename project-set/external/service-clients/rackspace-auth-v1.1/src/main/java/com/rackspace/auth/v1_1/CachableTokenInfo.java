package com.rackspace.auth.v1_1;

import java.io.Serializable;

public class CachableTokenInfo implements Serializable {
   private final String userName;
   private final String tokenId;
   
   public CachableTokenInfo(String userName, String tokenId) {
      this.userName = userName;
      this.tokenId = tokenId;
   }
   
   public String getUserName() {
      return userName;
   }
   
   public String getTokenId() {
      return tokenId;
   }
}
