/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.clientauth.rackspace.v1_1;


public enum RsAuthCachePrefix {
   
   TOKEN("rackspace.v1.1.token"), USER("rackspace.v1.1.user"), GROUP("rackspace.v1.1.group");
   
   private final String prefix;
   
   private RsAuthCachePrefix(String prefix) {
        this.prefix = prefix;
    }
}
