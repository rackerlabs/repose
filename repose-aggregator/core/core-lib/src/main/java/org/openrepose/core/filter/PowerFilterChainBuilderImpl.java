package org.openrepose.core.filter;

import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.filter.filtercontext.FilterContext;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.services.context.ServletContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import java.util.List;

/**
 *
 * @author zinic
 */
@Component("powerFilterChainBuilder")
@Scope("prototype")
public class PowerFilterChainBuilderImpl implements PowerFilterChainBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChainBuilderImpl.class);
    private final PowerFilterRouter router;
    private List<FilterContext> currentFilterChain;
    private ReposeInstanceInfo instanceInfo;
    private ServletContext servletContext;

    @Autowired
    public PowerFilterChainBuilderImpl(@Qualifier("powerFilterRouter") PowerFilterRouter router, @Qualifier("reposeInstanceInfo") ReposeInstanceInfo instanceInfo) {
        Thread.currentThread().setName(instanceInfo.toString());
        LOG.info("Creating filter chain builder");
        this.router = router;
        this.instanceInfo = instanceInfo;
    }

    @Override
    public void initialize(ReposeCluster domain, Node localhost, List<FilterContext> currentFilterChain, ServletContext servletContext, String defaultDst) throws PowerFilterChainException {
        LOG.info("Initializing filter chain builder");
        this.currentFilterChain = currentFilterChain;
        this.servletContext = servletContext;
        this.router.initialize(domain, localhost, servletContext, defaultDst);
    }


    @Override
    public PowerFilterChain newPowerFilterChain(FilterChain containerFilterChain) throws PowerFilterChainException {
        if (router == null) {
            throw new PowerFilterChainException("Power Filter Router has not been initialized yet.");
        }
        return new PowerFilterChain(currentFilterChain, containerFilterChain, router, instanceInfo,
                                    ServletContextHelper.getInstance(servletContext).getPowerApiContext().metricsService());
    }

    @Override
    public void destroy() {
        for (FilterContext context : currentFilterChain) {
            context.destroy();
        }
    }
}
