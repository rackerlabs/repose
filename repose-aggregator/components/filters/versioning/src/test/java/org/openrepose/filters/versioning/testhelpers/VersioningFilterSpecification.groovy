package org.openrepose.filters.versioning.testhelpers

import com.mockrunner.mock.web.MockFilterConfig
import com.mockrunner.mock.web.MockServletContext
import groovy.xml.StreamingMarkupBuilder
import org.openrepose.commons.config.manager.ConfigurationUpdateManager
import org.openrepose.commons.config.resource.ConfigurationResource
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.domain.Port
import org.openrepose.core.domain.ServicePorts
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.core.spring.SpringConfiguration
import org.openrepose.core.systemmodel.DestinationEndpoint
import org.openrepose.filters.versioning.VersioningFilter
import org.openrepose.filters.versioning.config.MediaTypeList
import org.openrepose.filters.versioning.config.ServiceVersionMapping
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Created by dimi5963 on 5/1/14.
 */
class VersioningFilterSpecification extends Specification {

    def buildFakeConfigXml(List<ServiceVersionMapping> mappingList) {
        def xml = new StreamingMarkupBuilder().bind() {
            mkp.xmlDeclaration()
            "versioning"(
                    "xmlns": "http://docs.openrepose.org/repose/versioning/v2.0"
            ) {
                "service-root"(
                        href: "localhost:9999"
                )
                mappingList.each { le ->
                    "version-mapping"(
                            "pp-dest-id": le.ppDestId,
                            id: le.id,
                            status: le.status
                    ){
                        "media-types"{
                            le.mediaTypes.mediaType.each {
                                "media-type"(
                                        type: it.type,
                                        base: it.base
                                )
                            }
                        }
                    }
                }
            }

        }
        return xml.toString()
    }

    def buildFakeSystemModelConfigXml(List<DestinationEndpoint> endpoints) {
        def xml = new StreamingMarkupBuilder().bind() {
            mkp.xmlDeclaration()
            "system-model"(
                    "xmlns": "http://docs.openrepose.org/repose/system-model/v2.0"
            ) {
                "repose-cluster"(
                        id: "repose-cluster"
                ){
                    "nodes"{
                        "node"(
                                id: "config-test",
                                hostname: "localhost",
                                "http-port": "12345"
                            )
                    }
                    "filters"{
                        "filter"(
                                name: "versioning"
                        )
                    }
                    "destinations"{
                        endpoints.each {
                            "endpoint"(
                                    id: it.id,
                                    protocol: it.protocol,
                                    hostname: it.hostname,
                                    port: it.port,
                                    "root-path": it.rootPath,
                                    default: it.default
                            )
                        }
                    }
                }
            }

        }
        return xml.toString()
    }

    VersioningFilter configureFilter(List<ServiceVersionMapping> mappingList, List<DestinationEndpoint> endpoints) {
        VersioningFilter filter = new VersioningFilter()

        def mockServletContext = new MockServletContext()
        def mockFilterConfig = new MockFilterConfig()
        mockFilterConfig.setupServletContext(mockServletContext)

        def servicePorts = new ServicePorts()
        servicePorts << [new Port("https", 8080)]
        def appContext = new AnnotationConfigApplicationContext(SpringConfiguration)
        when(appContext.getBean("servicePorts", ServicePorts.class)).thenReturn(servicePorts)


        def servletContextHelper = ServletContextHelper.configureInstance(
                mockServletContext,
                new AnnotationConfigApplicationContext(SpringConfiguration)
        )


        def configService = ServletContextHelper.getInstance(mockFilterConfig.getServletContext()).getPowerApiContext().configurationService()

        //Decouple the coupled configs, since I can't replace it
        def mockResourceResolver = mock(ConfigurationResourceResolver.class)
        configService.setResourceResolver(mockResourceResolver)
        def cfgUpdateManager = mock(ConfigurationUpdateManager.class)
        configService.setUpdateManager(cfgUpdateManager)

        def configString = buildFakeConfigXml(mappingList)

        //Create a config resource for the ContentNormalizationConfig
        def configResource = new ConfigurationResource() {

            @Override
            boolean updated() throws IOException {
                return true
            }

            @Override
            boolean exists() throws IOException {
                return true
            }

            @Override
            String name() {
                return "VersioningConfig"
            }

            @Override
            InputStream newInputStream() throws IOException {
                //Return the string of XML that we built earlier
                return new ByteArrayInputStream(configString.getBytes())
            }
        }

        //When someone asks for the config for this filter, give them the xml string we built
        when(mockResourceResolver.resolve("versioning.cfg.xml")).thenReturn(configResource)

        def systemModelConfigString = buildFakeSystemModelConfigXml(endpoints)

        def systemModelConfigResource = new ConfigurationResource() {
            @Override
            boolean updated() throws IOException {
                return false
            }

            @Override
            boolean exists() throws IOException {
                return true
            }

            @Override
            String name() {
                return "SystemModelConfig"
            }

            @Override
            InputStream newInputStream() throws IOException {
                return new ByteArrayInputStream(systemModelConfigString.getBytes())
            }
        }

        when(mockResourceResolver.resolve("system-model.cfg.xml")).thenReturn(systemModelConfigResource)

        //Initialize the filter now
        filter.init(mockFilterConfig)

        return filter
    }

    ServiceVersionMapping serviceVersionMapping(String ppDestId, String id, String status, MediaTypeList mediaTypeList) {
        new ServiceVersionMapping(
                id: id, ppDestId: ppDestId, status: status, mediaTypes: mediaTypeList)
    }
}
