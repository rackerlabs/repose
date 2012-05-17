package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthGroup;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;

import java.io.Serializable;


/**
 * @author fran
 */
public class OpenStackGroup implements AuthGroup, Serializable {

   private final String id;
   private final String name;
   private final String description;

   public OpenStackGroup(Group group) {
      this.id = group.getId();
      this.name = group.getName();
      this.description = group.getDescription();
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public String getDescription() {
      return description;
   }
}
