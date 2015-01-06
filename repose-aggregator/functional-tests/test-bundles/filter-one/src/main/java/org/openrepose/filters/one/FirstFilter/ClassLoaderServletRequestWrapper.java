package org.openrepose.filters.one.FirstFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.StringUtils;


/**
 * Created by dimi5963 on 1/5/15.
 */
public class ClassLoaderServletRequestWrapper extends HttpServletRequestWrapper {
    public ClassLoaderServletRequestWrapper(HttpServletRequest request){
        super(request);
    }

    @Override
    public String getHeader(String s) {
        if(StringUtils.startsWith(s, "FOO")){
            s = new SimplicityDivine().createBar();
        }
        return super.getHeader(s);
    }


}
