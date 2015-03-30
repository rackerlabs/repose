/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.test.mocks.util;

import java.util.*;

/**
 * Case Insensitive map to store headers
 */
public class HeaderMap extends HashMap<String, List<String>>{

    @Override
    public List<String> put(String key, List<String> value){
        return super.put(key.toLowerCase(), value);
    }

    @Override
    public List<String> get(Object key) {
        return super.get(key.toString().toLowerCase());
    }

    public HeaderMap(){
        super();
    }


    public static HeaderMap wrap(Map<String, List<String>> headers){

        HeaderMap map= new HeaderMap();
        final Set<Map.Entry<String,List<String>>> entrySet = headers.entrySet();

        for(Map.Entry<String,List<String>> entry : entrySet){

             if(!map.containsKey(entry.getKey().toLowerCase())){
                 map.put(entry.getKey().toLowerCase(), new ArrayList<String>());
             }
            map.get(entry.getKey().toLowerCase()).addAll(entry.getValue());

        }

        return map;
    }
}
