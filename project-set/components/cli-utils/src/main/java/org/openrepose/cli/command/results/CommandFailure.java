package org.openrepose.cli.command.results;

/**
 *
 * @author zinic
 */
public class CommandFailure implements CommandResult {

   private final String message;
   private final int statusCode;

   public CommandFailure(int statusCode, String message) {
      this.statusCode = statusCode;
      this.message = message;
   }

   @Override
   public String getStringResult() {
      return message;
   }

   @Override
   public int getStatusCode() {
      return statusCode;
   }
}
