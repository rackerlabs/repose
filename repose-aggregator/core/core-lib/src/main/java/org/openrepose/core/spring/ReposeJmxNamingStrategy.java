package org.openrepose.core.spring;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.domain.ReposeInstanceInfo;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.stereotype.Component;

@Named
@Lazy(true)
public class ReposeJmxNamingStrategy extends MetadataNamingStrategy implements ObjectNamingStrategy, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeJmxNamingStrategy.class);
    private static final String SEPARATOR = "-";
    private final String defaultDomainPrefix = UUID.randomUUID().toString() + SEPARATOR;
    private final String clusterId;
    private final String nodeId;

    //TODO: this is super broke, need to figure out how we're going to handle JMX strategy when core needs it
    //Metrics service needs this guy
    @Inject
    public ReposeJmxNamingStrategy(AnnotationJmxAttributeSource attributeSource) {
//                                   @Value(ReposeSpringProperties.NODE.CLUSTER_ID) String clusterId,
//                                   @Value(ReposeSpringProperties.NODE.NODE_ID) String nodeId) {
        super(attributeSource);
        this.clusterId = "";
        this.nodeId = "";

        LOG.info("Configuring JMX naming strategy for {} - {} ", clusterId, nodeId);
    }

    public String getDomainPrefix() {
        //TODO: none of this is needed
        StringBuilder sb = new StringBuilder();
        if (StringUtilities.isNotBlank(clusterId)) {
            sb.append(clusterId);
        }

        if (StringUtilities.isNotBlank(nodeId)) {
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(nodeId);
        }

        return sb.length() > 0? sb.append(SEPARATOR).toString(): defaultDomainPrefix;
    }

    @Override
    public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
        ObjectName name = super.getObjectName(managedBean, beanKey);
        return new ObjectName(getDomainPrefix() +  name.getDomain(), name.getKeyPropertyList());
    }
}
