package com.rackspace.papi.httpx.node;

import java.util.List;

/**
 * @author fran
 */
public class AcceptFidelityValidator {
    private static final String acceptFidelity = "ACCEPT";
    private static final String headerFidelity = "*";

    private final boolean hasValidFidelity;
    private final boolean hasAcceptFidelity;
    private final boolean hasStarFidelity;

    public AcceptFidelityValidator(List<String> fidelity) {
        this.hasAcceptFidelity = fidelity.contains(acceptFidelity);
        this.hasStarFidelity = fidelity.contains(headerFidelity);
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
