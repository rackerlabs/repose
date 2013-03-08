package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueParser;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.versioning.config.MediaTypeList;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.schema.VersionChoice;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.components.versioning.util.VersionChoiceFactory;
import com.rackspace.papi.components.versioning.util.http.HttpRequestInfo;
import com.rackspace.papi.components.versioning.util.http.UniformResourceInfo;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import org.ietf.atom.schema.Link;
import org.ietf.atom.schema.Relation;

import java.util.Collection;
import java.util.Map;

public class ConfigurationData {

   private final Map<String, ServiceVersionMapping> serviceMappings;
   private final Map<String, Destination> configuredHosts;
   private final ReposeCluster localDomain;
   private final Node localHost;

   public ConfigurationData(ReposeCluster localDomain, Node localHost, Map<String, Destination> configuredHosts, Map<String, ServiceVersionMapping> serviceMappings) {
      this.configuredHosts = configuredHosts;
      this.serviceMappings = serviceMappings;
      this.localDomain = localDomain;
      this.localHost = localHost;
   }

   public Collection<ServiceVersionMapping> getServiceMappings() {
      return serviceMappings.values();
   }

   public Map<String, Destination> getConfiguredHosts() {
      return configuredHosts;
   }

   public Destination getHostForVersionMapping(ServiceVersionMapping mapping) throws VersionedHostNotFoundException {
      final Destination host = configuredHosts.get(mapping.getPpDestId());

      if (host == null) {
         throw new VersionedHostNotFoundException("Endpoin: " + mapping.getPpDestId() + " is not specified in the system model");
      }

      return host;
   }

   public VersionedOriginService getOriginServiceForRequest(HttpRequestInfo requestInfo, FilterDirector director) throws VersionedHostNotFoundException {
      // Check URI first to see if it matches configured host href
      VersionedOriginService targetOriginService = findOriginServiceByUri(requestInfo);

      // If version info not in URI look in accept header
      if (targetOriginService == null) {
         final MediaType range = requestInfo.getPreferedMediaRange();
         final VersionedMapType currentServiceVersion = getServiceVersionForMediaRange(range);


         if (currentServiceVersion != null) {
            final Destination destination = getHostForVersionMapping(currentServiceVersion.getServiceVersionMapping());
            director.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.toString(), currentServiceVersion.getMediaType().getBase());
            targetOriginService = new VersionedOriginService(currentServiceVersion.getServiceVersionMapping(), destination);
         }
      }

      return targetOriginService;
   }

   public VersionedOriginService findOriginServiceByUri(HttpRequestInfo requestResourceInfo) throws VersionedHostNotFoundException {
      for (Map.Entry<String, ServiceVersionMapping> entry : serviceMappings.entrySet()) {
         final VersionedRequest versionedRequest = new VersionedRequest(requestResourceInfo, entry.getValue());

         if (versionedRequest.requestBelongsToVersionMapping()) {
            return new VersionedOriginService(entry.getValue(), getHostForVersionMapping(entry.getValue()));
         }
      }

      return null;
   }

   public VersionChoiceList versionChoicesAsList(HttpRequestInfo requestResourceInfo) {
      final VersionChoiceList versionChoices = new VersionChoiceList();

      for (ServiceVersionMapping mapping : getServiceMappings()) {
         final VersionedRequest versionedRequest = new VersionedRequest(requestResourceInfo, mapping);
         final VersionChoice choice = new VersionChoiceFactory(mapping).create();
         final Link selfReference = new Link();

         selfReference.setRel(Relation.SELF);
         selfReference.setHref(versionedRequest.asExternalURL());

         choice.getLink().add(selfReference);
         versionChoices.getVersion().add(choice);
      }

      return versionChoices;
   }

   public VersionedMapType getServiceVersionForMediaRange(MediaType preferedMediaRange) {
      com.rackspace.papi.components.versioning.config.MediaType mediaType;
      for (Map.Entry<String, ServiceVersionMapping> serviceMapping : serviceMappings.entrySet()) {
         mediaType = getMatchingMediaType((ServiceVersionMapping) serviceMapping.getValue(), preferedMediaRange);
         if (mediaType != null) {
            return new VersionedMapType((ServiceVersionMapping) serviceMapping.getValue(), mediaType);
         }
      }
      return null;
   }

   public com.rackspace.papi.components.versioning.config.MediaType getMatchingMediaType(ServiceVersionMapping serviceVersionMapping, MediaType preferedMediaType) {
      final MediaTypeList configuredMediaTypes = serviceVersionMapping.getMediaTypes();
       if(configuredMediaTypes == null){
           return null;
       }
      for (com.rackspace.papi.components.versioning.config.MediaType configuredMediaType : configuredMediaTypes.getMediaType()) {
         HeaderValue mediaType = new HeaderValueParser(configuredMediaType.getType()).parse();
         if(preferedMediaType.equalsTo(mediaType)){
            return configuredMediaType;
         }
      }
      return null;
   }

   public boolean isRequestForVersions(UniformResourceInfo uniformResourceInfo) {
      return StringUriUtilities.formatUri(uniformResourceInfo.getUri()).equals("/");
   }

   public ReposeCluster getLocalDomain() {
      return localDomain;
   }

   public Node getLocalHost() {
      return localHost;
   }
}
