package com.rackspace.auth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AuthGroups implements Serializable {

    private final List<AuthGroup> groups;

    public AuthGroups(List<AuthGroup> groups) {
        
        if (groups != null) {
            this.groups = groups;
        } else {
            this.groups = new ArrayList<AuthGroup>();
        }
    }

    public List<AuthGroup> getGroups() {
        return groups;
    }
}
