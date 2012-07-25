package com.rackspace.auth.openstack.ids;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CachableGroupInfo implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(CachableGroupInfo.class);
    private final List<String> groupIds = new ArrayList<String>();
    private final String userId;
    private final long ttl;

    public CachableGroupInfo(String userId, Groups groups, long ttl) {
        this.userId = userId;

        for (Group group : groups.getGroup()) {
            groupIds.add(group.getId());    
        }

        this.ttl = ttl;
    }

    public List<String> getGroupIds() {
        return groupIds;
    }

    public String getUserId() {
        return userId;
    }

    public long getTtl() {
        return ttl;
    }

    public int safeGroupTtl() {
      Long tokenTtl = ttl;

      if (tokenTtl >= Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }

      return tokenTtl.intValue();
   }
}
