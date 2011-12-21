package org.openrepose.cli.command.results;

/**
 *
 * @author zinic
 */
public class InvalidArguments extends MessageResult {

   public InvalidArguments(String message) {
      super(message);
   }

   @Override
   public int getStatusCode() {
      return StatusCodes.INVALID_ARGUMENTS.getStatusCode();
   }
}
