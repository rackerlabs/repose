package org.openrepose.cli.command.results;

import org.openrepose.cli.command.common.CommandResult;

/**
 *
 * @author zinic
 */
public class MessageResult implements CommandResult {

   public final String message;

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
