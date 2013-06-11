package com.rackspace.papi.spring;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import java.util.UUID;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.stereotype.Component;

@Component("reposeJmxNamingStrategy")
@Lazy(true)
public class ReposeJmxNamingStrategy extends MetadataNamingStrategy implements ObjectNamingStrategy, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeJmxNamingStrategy.class);
    private static final String SEPARATOR = "-";
    private final ReposeInstanceInfo reposeId;
    private final String defaultDomainPrefix = UUID.randomUUID().toString() + SEPARATOR;

    @Autowired
    public ReposeJmxNamingStrategy(@Qualifier("jmxAttributeSource") AnnotationJmxAttributeSource attributeSource, @Qualifier("reposeInstanceInfo") ReposeInstanceInfo reposeId) {
        super(attributeSource);
        this.reposeId = reposeId;
        
        LOG.info("Configuring JMX naming strategy for " + reposeId);
    }

    public String getDomainPrefix() {
        if (reposeId == null) {
            return defaultDomainPrefix;
        }

        StringBuilder sb = new StringBuilder();
        if (StringUtilities.isNotBlank(reposeId.getClusterId())) {
            sb.append(reposeId.getClusterId());
        }

        if (StringUtilities.isNotBlank(reposeId.getNodeId())) {
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(reposeId.getNodeId());
        }

        return sb.length() > 0? sb.append(SEPARATOR).toString(): defaultDomainPrefix;
    }

    @Override
    public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
        ObjectName name = super.getObjectName(managedBean, beanKey);
        return new ObjectName(getDomainPrefix() +  name.getDomain(), name.getKeyPropertyList());
    }
}
