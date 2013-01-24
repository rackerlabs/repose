package com.rackspace.auth.rackspace;

import com.rackspace.auth.AuthGroup;
import com.rackspacecloud.docs.auth.api.v1.Group;

import java.io.Serializable;

/**
 * @author fran
 */
public class RackspaceGroup implements AuthGroup, Serializable {

    private final String id;
    private final String description;

    public RackspaceGroup(Group group) {
        this.id = group.getId();
        this.description = group.getDescription();

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("The Rackspace Auth 1.1 Group does not provide a name.");
    }
}
