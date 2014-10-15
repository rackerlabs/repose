package org.openrepose.cli.command.datastore.distributed;

import org.openrepose.commons.utils.io.charset.CharacterSets;
import org.openrepose.commons.utils.encoding.UUIDEncodingProvider;
import org.openrepose.services.datastore.hash.MD5MessageDigestFactory;
import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.results.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;

/**
 *
 * @author zinic
 */
public class CacheKeyEncoder extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(CacheKeyEncoder.class);

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
          LOG.trace("JRE doesn't support MD5", noSuchAlgorithmException);
         return new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                 "Your instance of the Java Runtime Environment does not support the MD5 hash algorithm.");
      }
   }
}
