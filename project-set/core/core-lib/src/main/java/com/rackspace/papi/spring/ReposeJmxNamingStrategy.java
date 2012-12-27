package com.rackspace.papi.spring;

import java.util.UUID;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.stereotype.Component;

@Component("reposeJmxNamingStrategy")
@Lazy(true)
public class ReposeJmxNamingStrategy extends MetadataNamingStrategy implements ObjectNamingStrategy, InitializingBean {
    private final String domainPrefix;
    

    public ReposeJmxNamingStrategy() {
        domainPrefix = UUID.randomUUID().toString();
    }

    
    @Autowired
    public ReposeJmxNamingStrategy(@Qualifier("jmxAttributeSource") AnnotationJmxAttributeSource attributeSource, @Qualifier("reposeId") String reposeId) {
        super(attributeSource);
        domainPrefix = reposeId; //UUID.randomUUID().toString();
    }

    @Override
    public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
        ObjectName name = super.getObjectName(managedBean, beanKey);
        return new ObjectName(domainPrefix + "-" + name.getDomain(), name.getKeyPropertyList());
    }
}
