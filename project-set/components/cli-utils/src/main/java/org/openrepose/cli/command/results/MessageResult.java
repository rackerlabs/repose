package org.openrepose.cli.command.results;

/**
 *
 * @author zinic
 */
public class MessageResult implements CommandResult {

   private final String message;

   public MessageResult(String message) {
      this.message = message;
   }

   @Override
   public String getStringResult() {
      return message;
   }

   @Override
   public int getStatusCode() {
      return StatusCodes.OK.getStatusCode();
   }
}
