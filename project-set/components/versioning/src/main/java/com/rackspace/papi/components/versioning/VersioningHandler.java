package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.components.versioning.domain.VersionedHostNotFoundException;
import com.rackspace.papi.components.versioning.domain.VersionedOriginService;
import com.rackspace.papi.components.versioning.domain.VersionedRequest;
import com.rackspace.papi.components.versioning.schema.ObjectFactory;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.components.versioning.util.ContentTransformer;
import com.rackspace.papi.components.versioning.util.VersionChoiceFactory;
import com.rackspace.papi.components.versioning.util.http.HttpRequestInfo;
import com.rackspace.papi.components.versioning.util.http.HttpRequestInfoImpl;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.filters.Versioning;
import com.rackspace.papi.service.reporting.metrics.MeterByCategory;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBElement;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

public class VersioningHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(VersioningHandler.class);
   private static final ObjectFactory VERSIONING_OBJECT_FACTORY = new ObjectFactory();
   private static final double VERSIONING_DEFAULT_QUALITY = 0.5;
   private final ConfigurationData configurationData;
   private final ContentTransformer transformer;
   private final MetricsService metricsService;
   private MeterByCategory mbcVersionedRequests;

   public VersioningHandler(ConfigurationData configurationData, ContentTransformer transformer, MetricsService metricsService) {
      this.configurationData = configurationData;
      this.transformer = transformer;
      this.metricsService = metricsService;

       // TODO replace "versioning" with filter-id or name-number in sys-model
       if (metricsService != null) {
           mbcVersionedRequests = metricsService.newMeterByCategory(Versioning.class,
                   "versioning", "VersionedRequest", TimeUnit.SECONDS);
       }
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector filterDirector = new FilterDirectorImpl();
      final HttpRequestInfo httpRequestInfo = new HttpRequestInfoImpl(request);

      try {
         final VersionedOriginService targetOriginService = configurationData.getOriginServiceForRequest(httpRequestInfo, filterDirector);

         if (targetOriginService != null) {
            final VersionedRequest versionedRequest = new VersionedRequest(httpRequestInfo, targetOriginService.getMapping());
            handleVersionedRequest(versionedRequest, filterDirector, targetOriginService);
            if (mbcVersionedRequests != null) {
                mbcVersionedRequests.mark(targetOriginService.getMapping().getId());
            }
         } else {
            handleUnversionedRequest(httpRequestInfo, filterDirector);
            if (mbcVersionedRequests != null) {
                mbcVersionedRequests.mark("Unversioned");
            }
         }
         filterDirector.responseHeaderManager().appendHeader("Content-Type", httpRequestInfo.getPreferedMediaRange().getMimeType().getMimeType());
      } catch (VersionedHostNotFoundException vhnfe) {
         filterDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
         filterDirector.setFilterAction(FilterAction.RETURN);

         LOG.warn("Configured versioned service mapping refers to a bad pp-dest-id. Reason: " + vhnfe.getMessage(), vhnfe);
      } catch (MalformedURLException murlex) {
         filterDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
         filterDirector.setFilterAction(FilterAction.RETURN);

         LOG.warn("Configured versioned service mapping refers to a bad host definition. Reason: " + murlex.getMessage(), murlex);
      }

      // This is not a version we recognize - tell the client what's up
      if (filterDirector.getFilterAction() == FilterAction.NOT_SET) {
         writeMultipleChoices(filterDirector, httpRequestInfo);
      }

      return filterDirector;
   }

   private void handleUnversionedRequest(HttpRequestInfo httpRequestInfo, FilterDirector filterDirector) {
      // Is this a request to the service root to describe the available versions? (e.g. http://api.service.com/)
      if (configurationData.isRequestForVersions(httpRequestInfo)) {
         filterDirector.setResponseStatus(HttpStatusCode.OK);
         filterDirector.setFilterAction(FilterAction.RETURN);

         final JAXBElement<VersionChoiceList> versions = VERSIONING_OBJECT_FACTORY.createVersions(configurationData.versionChoicesAsList(httpRequestInfo));
         transformer.transform(versions, httpRequestInfo.getPreferedMediaRange(), filterDirector.getResponseOutputStream());
      }
   }

   private void handleVersionedRequest(VersionedRequest versionedRequest, FilterDirector filterDirector, VersionedOriginService targetOriginService) throws VersionedHostNotFoundException, MalformedURLException {
      // Is this a request to a version root we are aware of for describing it? (e.g. http://api.service.com/v1.0/)
      if (versionedRequest.isRequestForRoot() || versionedRequest.requestMatchesVersionMapping()) {
         final JAXBElement versionElement = VERSIONING_OBJECT_FACTORY.createVersion(new VersionChoiceFactory(targetOriginService.getMapping()).create());

         transformer.transform(versionElement, versionedRequest.getRequestInfo().getPreferedMediaRange(), filterDirector.getResponseOutputStream());

         filterDirector.setResponseStatus(HttpStatusCode.OK);
         filterDirector.setFilterAction(FilterAction.RETURN);
      } else {
         RouteDestination dest = filterDirector.addDestination(targetOriginService.getOriginServiceHost(), versionedRequest.asInternalURI(), (float) VERSIONING_DEFAULT_QUALITY);
         dest.setContextRemoved(versionedRequest.getMapping().getId());
         filterDirector.setFilterAction(FilterAction.PASS);
      }
   }

   private void writeMultipleChoices(FilterDirector filterDirector, HttpRequestInfo httpRequestInfo) {
      filterDirector.setResponseStatus(HttpStatusCode.MULTIPLE_CHOICES);
      filterDirector.setFilterAction(FilterAction.RETURN);

      final VersionChoiceList versionChoiceList = configurationData.versionChoicesAsList(httpRequestInfo);
      JAXBElement<VersionChoiceList> versionChoiceListElement = VERSIONING_OBJECT_FACTORY.createChoices(versionChoiceList);

      transformer.transform(versionChoiceListElement, httpRequestInfo.getPreferedMediaRange(), filterDirector.getResponseOutputStream());
   }
}