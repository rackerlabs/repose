package org.openrepose.cli.command;

/**
 *
 * @author zinic
 */
public interface CommandResult {

   int getStatusCode();
   
   String getStringResult();
}
