package org.openrepose.powerfilter;

import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.powerfilter.filtercontext.FilterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import java.util.List;

@Named
public class PowerFilterChainBuilderImpl implements PowerFilterChainBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChainBuilderImpl.class);
    private final PowerFilterRouter router;
    private final MetricsService metricsService;
    private List<FilterContext> currentFilterChain;

    @Inject
    public PowerFilterChainBuilderImpl(PowerFilterRouter router,
                                       MetricsService metricsService) {
        this.metricsService = metricsService;
        LOG.info("Creating filter chain builder");
        this.router = router;
    }

    @Override
    public void initialize(ReposeCluster domain, Node localhost, List<FilterContext> currentFilterChain, ServletContext servletContext, String defaultDst) throws PowerFilterChainException {
        LOG.info("Initializing filter chain builder");
        this.currentFilterChain = currentFilterChain;
        this.router.initialize(domain, localhost, servletContext, defaultDst);
    }


    @Override
    public PowerFilterChain newPowerFilterChain(FilterChain containerFilterChain) throws PowerFilterChainException {
        return new PowerFilterChain(currentFilterChain,
                containerFilterChain,
                router,
                metricsService);
    }

    @Override
    public void destroy() {
        for (FilterContext context : currentFilterChain) {
            context.destroy();
        }
    }
}
