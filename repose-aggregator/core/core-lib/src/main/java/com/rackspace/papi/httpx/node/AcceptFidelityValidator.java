package com.rackspace.papi.httpx.node;

import java.util.List;

/**
 * @author fran
 */
public class AcceptFidelityValidator {
    private static final String ACCEPT_FIDELITY = "ACCEPT";
    private static final String HEADER_FIDELITY = "*";

    private final boolean hasValidFidelity;
    private final boolean hasAcceptFidelity;
    private final boolean hasStarFidelity;

    public AcceptFidelityValidator(List<String> fidelity) {
        this.hasAcceptFidelity = fidelity.contains(ACCEPT_FIDELITY);
        this.hasStarFidelity = fidelity.contains(HEADER_FIDELITY);
        this.hasValidFidelity = hasAcceptFidelity || hasStarFidelity;
    }

    public boolean hasValidFidelity() {
        return hasValidFidelity;
    }

    public boolean hasAcceptFidelity() {
        return hasAcceptFidelity;    
    }

    public boolean hasStarFidelity() {
        return hasStarFidelity;
    }
}
