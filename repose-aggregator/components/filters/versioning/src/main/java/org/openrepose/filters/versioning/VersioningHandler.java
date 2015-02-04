package org.openrepose.filters.versioning;

import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.core.filters.Versioning;
import org.openrepose.core.services.reporting.metrics.MeterByCategory;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.filters.versioning.domain.ConfigurationData;
import org.openrepose.filters.versioning.domain.VersionedHostNotFoundException;
import org.openrepose.filters.versioning.domain.VersionedOriginService;
import org.openrepose.filters.versioning.domain.VersionedRequest;
import org.openrepose.filters.versioning.schema.ObjectFactory;
import org.openrepose.filters.versioning.schema.VersionChoiceList;
import org.openrepose.filters.versioning.util.ContentTransformer;
import org.openrepose.filters.versioning.util.VersionChoiceFactory;
import org.openrepose.filters.versioning.util.http.HttpRequestInfo;
import org.openrepose.filters.versioning.util.http.HttpRequestInfoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
         filterDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_GATEWAY);
         filterDirector.setFilterAction(FilterAction.RETURN);

         LOG.warn("Configured versioned service mapping refers to a bad pp-dest-id. Reason: " + vhnfe.getMessage(), vhnfe);
      } catch (MalformedURLException murlex) {
         filterDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_GATEWAY);
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
         filterDirector.setResponseStatusCode(HttpServletResponse.SC_OK);
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

         filterDirector.setResponseStatusCode(HttpServletResponse.SC_OK);
         filterDirector.setFilterAction(FilterAction.RETURN);
      } else {
         RouteDestination dest = filterDirector.addDestination(targetOriginService.getOriginServiceHost(), versionedRequest.asInternalURI(), (float) VERSIONING_DEFAULT_QUALITY);
         dest.setContextRemoved(versionedRequest.getMapping().getId());
         filterDirector.setFilterAction(FilterAction.PASS);
      }
   }

   private void writeMultipleChoices(FilterDirector filterDirector, HttpRequestInfo httpRequestInfo) {
      filterDirector.setResponseStatusCode(HttpServletResponse.SC_MULTIPLE_CHOICES);
      filterDirector.setFilterAction(FilterAction.RETURN);

      final VersionChoiceList versionChoiceList = configurationData.versionChoicesAsList(httpRequestInfo);
      JAXBElement<VersionChoiceList> versionChoiceListElement = VERSIONING_OBJECT_FACTORY.createChoices(versionChoiceList);

      transformer.transform(versionChoiceListElement, httpRequestInfo.getPreferedMediaRange(), filterDirector.getResponseOutputStream());
   }
}