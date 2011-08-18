/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * 
 */
public class StringHandler implements FormatterLogic {

    private final String staticStringContent;

    public StringHandler(String staticStringContent) {
        this.staticStringContent = staticStringContent;
    }

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        return staticStringContent;
    }
}
