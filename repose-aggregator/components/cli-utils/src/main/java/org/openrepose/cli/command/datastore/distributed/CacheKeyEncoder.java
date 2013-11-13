package org.openrepose.cli.command.datastore.distributed;

import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.results.*;

import java.security.NoSuchAlgorithmException;

/**
 *
 * @author zinic
 */
public class CacheKeyEncoder extends AbstractCommand {

   @Override
   public String getCommandDescription() {
      return "Encodes a cache key into a representation that the distributed datastore can address.";
   }

   @Override
   public String getCommandToken() {
      return "encode-key";
   }

   @Override
   public CommandResult perform(String[] arguments) {
      if (arguments.length != 1) {
         return new InvalidArguments("The cache key encoder expects one, string argument.");
      }

      try {
         final byte[] hashBytes = MD5MessageDigestFactory.getInstance().newMessageDigest().digest(arguments[0].getBytes(CharacterSets.UTF_8));
         final String encodedCacheKey = UUIDEncodingProvider.getInstance().encode(hashBytes);
         
         return new MessageResult(encodedCacheKey);
      } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
         return new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                 "Your instance of the Java Runtime Environment does not support the MD5 hash algorithm.");
      }
   }
}
