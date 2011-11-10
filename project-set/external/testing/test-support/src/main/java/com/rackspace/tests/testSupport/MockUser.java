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
    protected SimpleDateFormat format;
    protected final Calendar cal = Calendar.getInstance();
    protected boolean keystone;

    public MockUser(String name, String group, String token) {
        this.name = name;
        this.group = group;
        this.token = token;
        cal.setLenient(true);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        keystone = false;
    }

    public MockUser(String params, String token) {
        this.token = token;

        this.name = params.split("&")[0].split("=")[1];
        this.group = params.split("&")[1].split("=")[1];
        cal.setLenient(true);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        keystone = false;
    }

    public MockUser(MockUser user) {
        this.token = user.getToken();
        this.name = user.getName();
        keystone = true;

    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getToken() {
        return token;
    }

    public boolean equals(MockUser user) {


        return user.getName().equals(name) && user.getGroup().equals(group) && user.getToken().equals(token) ? true : false;
    }

    @Override
    public String toString() {

        String responseBody;
        if (keystone) {
            format  = new SimpleDateFormat("yyyy-MM-dd'T12:00:00.882246'");
            responseBody = "{\"access\": {\"token\": {\"expires\": \"" + format.format(cal.getTime()) + "\","
                    + " \"id\": \"" + token + "\", \"tenant\": "
                    + "{\"id\": \"1\", \"name\": \"admin\"}}, "
                    + "\"user\": {\"username\": \"" + name + "\", \"id\": \"4\","
                    + " \"roles\": [{\"id\": \"1\", \"name\": \"Admin\"},"
                    + " {\"id\": \"1\", \"name\": \"Admin\"}],"
                    + " \"tenantId\": \"1\"}}}";


        } else {
            format = new SimpleDateFormat("yyyy-MM-dd'T12:00:00-05:00'");
            responseBody = "<token xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" "
                    + "id=\"" + token + "\" "
                    + "userId=\"" + name + "\" "
                    + "userURL=\"/users/" + name + "\" "
                    + "created=\"2010-09-14T03:32:15-05:00\" "
                    + "expires=\"" + format.format(cal.getTime()) + "\"/>";
        }
        //+ "expires=\"2012-09-16T03:32:15-05:00\"/>";

        return responseBody;
    }
}
