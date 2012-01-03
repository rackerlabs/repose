package org.openrepose.cli.command.common;

/**
 *
 * @author zinic
 */
public interface CommandResult {

   int getStatusCode();
   
   String getStringResult();
}
