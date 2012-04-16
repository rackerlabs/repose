package org.openrepose.rackspace.auth_2_0.identity.content.auth;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.StreamToJaxbTransform;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.openrepose.rackspace.auth2.content_identity.config.ContentIdentityAuthConfig;
import org.slf4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

public class ContentIdentityAuthHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ContentIdentityAuthHandler> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentIdentityAuthHandlerFactory.class);
   private ContentIdentityAuthConfig config;
   private JacksonJaxbTransform jsonTranformer;
   private JAXBContext jaxbContext = null;
   private Transform<InputStream, JAXBElement<?>> xmlTransformer;

   public ContentIdentityAuthHandlerFactory() {
      jsonTranformer = new JacksonJaxbTransform();

      try {
         jaxbContext = JAXBContext.newInstance(org.openstack.docs.identity.api.v2.ObjectFactory.class);
         xmlTransformer = new StreamToJaxbTransform(jaxbContext);
      } catch (JAXBException e) {
         LOG.error("Error when creating JABXContext for auth credentials. Reason: " + e.getMessage(), e);
      }
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
      return new ContentIdentityAuthHandler(config, jsonTranformer, xmlTransformer);
   }
}
