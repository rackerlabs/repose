package org.openrepose.core.service.deploy;

public class ArtifactDirectoryItem {
    private final ApplicationArtifactEvent event;
    private final String path;

    public ArtifactDirectoryItem(ApplicationArtifactEvent event, String path) {
        this.path = path;
        this.event = event;
    }

    public ApplicationArtifactEvent getEvent() {
        return event;
    }

    public String getPath() {
        return path;
    }
}
