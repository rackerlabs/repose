package org.openrepose.commons.utils.plugin.archive;

public enum DeploymentAction {

   UNPACK_ENTRY,
   DO_NOT_UNPACK_ENTRY;

   public static DeploymentAction defaultAction() {
      return UNPACK_ENTRY;
   }
}
