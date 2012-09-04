package com.rackspace.papi.service.reporting.repose;

public class ReposeInfoLogic extends ReposeInfoStore implements ReposeInfo {

    private static final long LONG_ZERO = 0l;
    private static final long LONG_ONE = 1l;
    private static final int INT_ONE = 1;
    private static final double DOUBLE_ZERO = 0.0d;

    public ReposeInfoLogic() {
        super();
    }

    private ReposeInfoLogic(ReposeInfoLogic reposeInfoLogic) {
        super(reposeInfoLogic);
    }

    @Override
    public long getTotalStatusCode(int statusCode) {
        Long count = super.getStatusCodeCounts().get(statusCode);

        if (count != null) {
            return count;
        } else {
            return LONG_ZERO;
        }
    }

    @Override
    public void incrementStatusCodeCount(int statusCode) {
        Long value = super.getStatusCodeCounts().get(statusCode);

        if (value != null) {
            super.getStatusCodeCounts().put(statusCode, ++value);
        } else {
            super.getStatusCodeCounts().put(statusCode, LONG_ONE);
        }
    }

    @Override
    public void incrementRequestCount() {
        super.setTotalRequests(super.getTotalRequests() + INT_ONE);
    }

    @Override
    public void incrementResponseCount() {
        super.setTotalResponses(super.getTotalResponses() + INT_ONE);
    }

    @Override
    public void accumulateRequestSize(long requestSize) {
        super.setAccumulatedRequestSize(super.getAccumulatedRequestSize() + requestSize);
    }

    @Override
    public void accumulateResponseSize(long responseSize) {
        super.setAccumulatedResponseSize(super.getAccumulatedResponseSize() + responseSize);
    }

    @Override
    public void updateMinMaxRequestSize(long requestSize) {
        if (super.getMinRequestSize() == 0 || requestSize < super.getMinRequestSize()) {
            super.setMinRequestSize(requestSize);
        }

        if (requestSize > super.getMaxRequestSize()) {
            super.setMaxRequestSize(requestSize);
        }
    }

    @Override
    public void updateMinMaxResponseSize(long responseSize) {
        if (super.getMinResponseSize() == 0 || responseSize < super.getMinResponseSize()) {
            super.setMinResponseSize(responseSize);
        }

        if (responseSize > super.getMaxResponseSize()) {
            super.setMaxResponseSize(responseSize);
        }
    }

    @Override
    public long getMinimumRequestSize() {
        return super.getMinRequestSize();
    }

    @Override
    public long getMaximumRequestSize() {
        return super.getMaxRequestSize();
    }

    @Override
    public long getMinimumResponseSize() {
        return super.getMinResponseSize();
    }

    @Override
    public long getMaximumResponseSize() {
        return super.getMaxResponseSize();
    }

    @Override
    public double getAverageRequestSize() {
        double averageRequestSize = (double)super.getAccumulatedResponseSize()/(double)super.getTotalRequests();

        if (Double.isNaN(averageRequestSize)) {
            return DOUBLE_ZERO;
        } else {
            return averageRequestSize;
        }
    }

    @Override
    public double getAverageResponseSize() {
        double averageResponseSize = (double)super.getAccumulatedResponseSize()/(double)super.getTotalResponses();

        if (Double.isNaN(averageResponseSize)) {
            return DOUBLE_ZERO;
        } else {
            return averageResponseSize;
        }
    }

    @Override
    public ReposeInfo copy() {
        return new ReposeInfoLogic(this);
    }
}
