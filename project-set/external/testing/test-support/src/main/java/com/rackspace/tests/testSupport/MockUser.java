/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.tests.testSupport;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author malconis
 */
public class MockUser {
    
    protected String name, group, token;
    protected final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T12:00:00-05:00'");
    protected final Calendar cal = Calendar.getInstance();
    
    
    public MockUser(String name, String group, String token){
        this.name = name;
        this.group = group;
        this.token = token;
        cal.setLenient(true);
        cal.add(Calendar.DAY_OF_YEAR, 1);
    }
    
    public MockUser(String params, String token){
        this.token = token;
        
        this.name = params.split("&")[0].split("=")[1];
        this.group = params.split("&")[1].split("=")[1];
        cal.setLenient(true);
        cal.add(Calendar.DAY_OF_YEAR, 1);
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
                + "expires=\"" + format.format(cal.getTime()) + "\"/>";
                //+ "expires=\"2012-09-16T03:32:15-05:00\"/>";
        
        return responseBody;
    }
}
