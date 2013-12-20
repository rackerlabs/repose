package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.service.rms.config.Message;

import java.util.List;

/**
 * @author fran
 */
public final class MessageFilter {

   private MessageFilter() {      
   }

   public static Message filterByMediaType(List<Message> messages, MediaType mediaType) {
      Message wildcard = null;

      if (messages != null && mediaType != null) {
         for (Message message : messages) {
            final String messageMediaType = message.getMediaType();

            if (StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, mediaType.getValue())) {
               return message;
            }

            // A configured wildcard (*/*) will be returned if an exact match is not found
            if (StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, MimeType.WILDCARD.getMimeType())) {
               wildcard = message;
            }
         }
      }

      return wildcard;
   }
   
     public static Message filterByMediaType(List<Message> messages,List<MediaType> mediaTypes) {
      Message wildcard = null;

      if (messages != null && mediaTypes != null) {
         
         for(MediaType mediaType: mediaTypes) {
         
          for (Message message : messages) {
            final String messageMediaType = message.getMediaType();
            
           
            
            if (StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, mediaType.getValue())) {
               return message;
            }

            // A configured wildcard (*/*) will be returned if an exact match is not found
            if (wildcard==null && StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, MimeType.WILDCARD.getMimeType()) ) {
               wildcard = message;
            }
                
         }
      }
      }    
   
      return wildcard;
  
   }
}
