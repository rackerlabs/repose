package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.UniformResourceInfo;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.components.versioning.schema.ObjectFactory;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.components.versioning.util.ContentTransformer;
import com.rackspace.papi.components.versioning.util.VersionChoiceFactory;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.xml.bind.JAXBElement;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Apr 26, 2011
 * Time: 2:45:53 PM
 */
public class VersioningTaggerHelper {
    private static final ObjectFactory VERSIONING_OBJECT_FACTORY = new ObjectFactory();
    private final ConfigurationData configurationData;
    private final ContentTransformer transformer;

    public VersioningTaggerHelper(ConfigurationData configurationData) {
        this.configurationData = configurationData;
        this.transformer = new ContentTransformer();
    }

    public String transformVersioningInformationRequest(HttpRequestInfo requestInfo, MediaType mediaType) {
        JAXBElement targetElement = getVersioningInformation(requestInfo);

        return (targetElement != null) ? transformer.transform(targetElement, mediaType) : null;
    }

    public JAXBElement getVersioningInformation(HttpRequestInfo requestResourceInfo) {
        final Host targetVersionedHost = configurationData.getOriginToRouteTo(requestResourceInfo);
        JAXBElement targetElement = null;

        if (targetVersionedHost != null) {
            // First match wins
            for (ServiceVersionMapping mapping : configurationData.getServiceMappings()) {
                if (mapping.getPpHostId().equals(targetVersionedHost.getId())) {
                    targetElement = VERSIONING_OBJECT_FACTORY.createVersion(new VersionChoiceFactory(mapping).create());
                    break;
                }
            }
        } else if (configurationData.isRequestForVersionChoices(requestResourceInfo)) {
            targetElement = VERSIONING_OBJECT_FACTORY.createVersions(configurationData.versionChoicesAsList(requestResourceInfo));
        }

        return targetElement;
    }

    public FilterDirector returnMultipleChoices(UniformResourceInfo requestResourceInfo, MediaType acceptType) {
        final FilterDirector myDirector = new FilterDirectorImpl();

        myDirector.setFilterAction(FilterAction.RETURN);
        myDirector.setResponseStatus(HttpStatusCode.MULTIPLE_CHOICES);

        VersionChoiceList versionChoiceList = configurationData.versionChoicesAsList(requestResourceInfo);
        JAXBElement<VersionChoiceList> versionChoiceListElement
                = VERSIONING_OBJECT_FACTORY.createChoices(versionChoiceList);

        myDirector.getResponseWriter().write(transformer.transform(versionChoiceListElement, acceptType));

        return myDirector;
    }

    @Deprecated
    public Host getOriginToRouteTo(HttpRequestInfo requestInfo) {
        return configurationData.getOriginToRouteTo(requestInfo);
    }
}
