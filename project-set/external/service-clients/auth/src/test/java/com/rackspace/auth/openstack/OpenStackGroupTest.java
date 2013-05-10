/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthGroup;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import javax.xml.datatype.DatatypeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kush5342
 */
public class OpenStackGroupTest {
    
    private DatatypeFactory dataTypeFactory;
    AuthenticateResponse authResponse;
    Groups groups;
    Group group;
    AuthGroup authGroup;
    
      
    @Before
    public void setUp() {
        
        groups = new Groups();
        group = new Group();
        group.setId("groupId");
        group.setDescription("Group Description");
        group.setName("Group Name");
        groups.getGroup().add(group);
        authGroup = new OpenStackGroup(group);

    }
    
     /**
     * Test of getId method, of class OpenStackGroup.
     */
    @Test
    public void testGetId() {
        String expResult = "groupId";
        String result = authGroup.getId();
        assertEquals(expResult, result);
        
    }

    /**
     * Test of getName method, of class OpenStackGroup.
     */
    @Test
    public void testGetName() {
    
        String expResult = "Group Name";
        String result = authGroup.getName();
        assertEquals(expResult, result);
        
    }

    /**
     * Test of getDescription method, of class OpenStackGroup.
     */
    @Test
    public void testGetDescription() {
        
        String expResult = "Group Description";
        String result = authGroup.getDescription();
        assertEquals(expResult, result);
      
    }
}