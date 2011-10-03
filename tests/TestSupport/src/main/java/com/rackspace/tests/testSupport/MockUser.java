/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.tests.testSupport;

/**
 *
 * @author malconis
 */
public class MockUser {
    
    protected String name, group, token;
    
    
    public MockUser(String name, String group, String token){
        this.name = name;
        this.group = group;
        this.token = token;
    }
    
    public MockUser(String params, String token){
        this.token = token;
        
        this.name = params.split("&")[0].split("=")[1];
        this.group = params.split("&")[1].split("=")[1];
    }
    
    
    public String getName(){
        return name;
    }
    
    public String getGroup(){
        return group;
    }
    
    public String getToken(){
        return token;
    }
    
    
    public boolean equals(MockUser user){
        
        
        return user.getName().equals(name)  && user.getGroup().equals(group) && user.getToken().equals(token)? true :false;
    }
    
    @Override
    public String toString(){
        
        String responseBody = "<token xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" "
                + "id=\""+token+"\" "
                + "userId=\""+name+"\" "
                + "userURL=\"/users/"+name+"\" "
                + "created=\"2010-09-14T03:32:15-05:00\" "
                + "expires=\"2012-09-16T03:32:15-05:00\"/>";
        
        return responseBody;
    }
}
