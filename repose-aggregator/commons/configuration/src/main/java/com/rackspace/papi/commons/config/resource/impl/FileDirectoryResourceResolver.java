package com.rackspace.papi.commons.config.resource.impl;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import com.rackspace.papi.commons.config.resource.ResourceResolutionException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author malconis
 */
public class FileDirectoryResourceResolver implements ConfigurationResourceResolver {

   private final String configRoot;

   public FileDirectoryResourceResolver(String configRoot) {

      this.configRoot = configRoot;
   }

   public String getConfigRoot() {
      return this.configRoot;
   }

   @Override
   public ConfigurationResource resolve(String resourceName) {

      try {

         if (resourceName.contains(":/")) {
            return new BufferedURLConfigurationResource(new URL(resourceName));
         } else {
            final File spec = new File(configRoot, resourceName);

            return new BufferedURLConfigurationResource(spec.toURI().toURL());
         }
      } catch (MalformedURLException murle) {
         throw new ResourceResolutionException("Unable to build URL for resource. Resource: "
                 + resourceName + ". Reason: " + murle.getMessage(), murle);
      } catch (IllegalArgumentException ex) {
         throw new ResourceResolutionException("Unable to build URL for resource. Resource: " + resourceName + ". Reason: " + ex.getMessage(), ex);
      }
   }
}
