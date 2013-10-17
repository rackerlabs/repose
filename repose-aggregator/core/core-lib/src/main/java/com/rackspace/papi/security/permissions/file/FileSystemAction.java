package com.rackspace.papi.security.permissions.file;

/**
 *
 * @author zinic
 */
public enum FileSystemAction {

   READ, WRITE, EXECUTE, DELETE;

   public static String actionsToString(FileSystemAction... actions) {
      if (actions.length == 0) {
         throw new IllegalArgumentException("Must specifiy at least one FileSystemAction to format into a string.");
      }
      final StringBuilder stringBuilder = new StringBuilder(actions[0].name().toLowerCase());
      for (int i = 1; i < actions.length; i++) {
         stringBuilder.append(",").append(actions[i].name().toLowerCase());
      }
      return stringBuilder.toString();
   }
}
