/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.auth;

import java.util.List;

/**
 *
 * @author malconis
 */
public class FullAuthInfo {
   
   private AuthToken token;
   private List<CachedEndpoint> endpoints;
   
   public FullAuthInfo(AuthToken token, List<CachedEndpoint> endpoints){
      this.token = token;
      this.endpoints = endpoints;
   }

   public List<CachedEndpoint> getEndpoints() {
      return endpoints;
   }

   public AuthToken getToken() {
      return token;
   }
   
   
   
}
