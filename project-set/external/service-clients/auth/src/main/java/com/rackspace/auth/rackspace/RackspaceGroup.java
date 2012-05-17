package com.rackspace.auth.rackspace;

import com.rackspace.auth.AuthGroup;
import com.rackspacecloud.docs.auth.api.v1.Group;

import java.io.Serializable;

/**
 * @author fran
 */
public class RackspaceGroup implements AuthGroup, Serializable {

   private final Group group;

   public RackspaceGroup(Group group) {
      this.group = group;
   }

   @Override
   public String getId() {
      return group.getId();
   }

   @Override
   public String getDescription() {
      return group.getDescription();
   }

   @Override
   public String getName() {
      throw new UnsupportedOperationException("The Rackspace Auth 1.1 Group does not provide a name.");
   }
}
