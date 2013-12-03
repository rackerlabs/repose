package com.rackspace.papi.test.mocks.util;

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
