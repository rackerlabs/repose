package org.openrepose.filters.clientauth.openstack;


public enum OsAuthCachePrefix {
   
   TOKEN("openstack.identity.token"), USER("openstack.identity.group"), GROUP("openstack.identity.user"), ENDPOINTS("openstack.endpoints.cache");
   
   private final String prefix;
   
   private OsAuthCachePrefix(String prefix) {
        this.prefix = prefix;
    }
   
}
