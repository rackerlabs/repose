
package com.rackspace.papi.commons.collections;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;

public class OrderedMultiValuedMap extends LinkedHashMap<String, List<String>> implements MultivaluedMap<String, String> {

    @Override
    public void add(String k, String v) {
        List<String> objects = this.get(k);
        if(objects == null){
            objects = new ArrayList<String>();
        }
        objects.add(v);
        this.put(k, objects);
    }

    @Override
    public String getFirst(String k) {
        
        List<String> objects = this.get(k);
        
        if(objects == null){
            objects = new ArrayList<String>();
        }
        
        if(!objects.isEmpty()){
            return objects.get(0);
        }else{
            return null;
        }
    }

    @Override
    public void putSingle(String k, String v) {
        this.remove(k);
        this.add(k, v);
    }
    
    
}
