package org.openrepose.rackspace.auth_2_0.identity.content;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.openrepose.rackspace.auth.content_identity.config.ContentIdentityAuthConfig;
import org.slf4j.Logger;

public class ContentIdentityAuthHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ContentIdentityAuthHandler> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentIdentityAuthHandlerFactory.class);
   private ContentIdentityAuthConfig config;

   public ContentIdentityAuthHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(ContentIdentityAuthConfig.class, new ContentIdentityAuthConfigurationListener());
         }
      };
   }

   private class ContentIdentityAuthConfigurationListener implements UpdateListener<ContentIdentityAuthConfig> {

      @Override
      public void configurationUpdated(ContentIdentityAuthConfig configurationObject) {
         config = configurationObject;
         LOG.debug("Configuration updated (quality = '" + config.getQuality() + "' group = '" + config.getGroup() + "')");
      }
   }

   @Override
   protected ContentIdentityAuthHandler buildHandler() {
      return new ContentIdentityAuthHandler(config);
   }
}
