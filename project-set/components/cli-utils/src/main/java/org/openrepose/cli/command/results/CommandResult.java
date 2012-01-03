package org.openrepose.cli.command.results;

/**
 *
 * @author zinic
 */
public interface CommandResult {

   int getStatusCode();
   
   String getStringResult();
}
