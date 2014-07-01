package com.rackspace.auth.rackspace;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.openstack.OpenStackGroup;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jennyvo on 7/1/14.
 */
public class AuthGroupsTests {
    Group group1,group2,group3;
    AuthGroups authGroups;
    AuthGroup testGroup1,testGroup2,testGroup3;
    private List<AuthGroup> groupsList;

    @Before
    public void setUp(){
        group1 = new Group();
        group1.setId("group1");
        group1.setName("group_1");
        group1.setDescription("user test group1");
        testGroup1 = new OpenStackGroup(group1);
        group2 = new Group();
        group2.setId("group2");
        group2.setName("group_2");
        group2.setDescription("user test group2");
        testGroup2 = new OpenStackGroup(group2);
        group3 = new Group();
        group3.setId("group3");
        group3.setName("group_3");
        group3.setDescription("user test group3");
        testGroup3 = new OpenStackGroup(group3);
        groupsList = new ArrayList<>();
        groupsList.add(testGroup1);
        groupsList.add(testGroup2);
        groupsList.add(testGroup3);
    }

    /**
     * Test of getGroups from AuthGroups
     */
    @Test
    public void shouldReturnGroupList(){
        authGroups = new AuthGroups(groupsList);
        assert authGroups.getGroups().size() == 3;
        assert authGroups.getGroups() == groupsList;
    }

    @Test
    public void shouldReturnEmptyList(){
        authGroups = new AuthGroups(null);
        assert authGroups.getGroups().isEmpty();
    }

}
