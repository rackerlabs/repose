package org.openrepose.services.healthcheck;

public class HealthCheckReport {

    private String message;
    private Severity level;

    public HealthCheckReport(String message, Severity level){

        this.message = message;
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public Severity getLevel() {
        return level;
    }
}
