/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.clientauth.openstack.v1_0;


public enum OsAuthCachePrefix {
   
   TOKEN("openstack.identity.token"), USER("openstack.identity.group"), GROUP("openstack.identity.user"), ENDPOINTS("openstack.endpoints.cache");
   
   private final String prefix;
   
   private OsAuthCachePrefix(String prefix) {
        this.prefix = prefix;
    }
   
}
