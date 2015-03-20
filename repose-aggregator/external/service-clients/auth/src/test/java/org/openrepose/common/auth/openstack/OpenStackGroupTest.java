/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthGroup;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.openrepose.common.auth.openstack.OpenStackGroup;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

import javax.xml.datatype.DatatypeFactory;

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