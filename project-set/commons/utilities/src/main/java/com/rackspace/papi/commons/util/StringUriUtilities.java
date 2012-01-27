package com.rackspace.papi.commons.util;

import com.rackspace.papi.commons.util.string.JCharSequence;
import static com.rackspace.papi.commons.util.string.JCharSequenceFactory.*;

/**
 * This is a simple helper class that can be used to generalize URI related
 * string processing.
 * 
 * @author zinic
 */
public final class StringUriUtilities {

    public static int indexOfUriFragment(String st, String uriFragment) {
        return indexOfUriFragment(jchars(st), uriFragment);
    }

    public static int indexOfUriFragment(JCharSequence uri, String uriFragment) {
        final int index = uri.indexOf(uriFragment);

        if (uri.length() > uriFragment.length() + index) {
            return uri.charAt(index + uriFragment.length()) == '/' ? index : -1;
        }

        return index;
    }

    /**
     * Formats a URI by adding a forward slash and removing the last forward
     * slash from the URI.
     * 
     * e.g. some/random/uri/    -> /some/random/uri
     * e.g. some/random/uri     -> /some/random/uri
     * e.g. /some/random/uri/   -> /some/random/uri
     * e.g. /                   -> /
     * e.g. //////              -> /
     * 
     * @param uri
     * @return 
     */
    public static String formatUri(String uri) {
        if (StringUtilities.isBlank(uri)) {
            return "";
        }

        final StringBuilder externalName = new StringBuilder(uri);

        if (externalName.charAt(0) != '/') {
            externalName.insert(0, "/");
        }


        if (externalName.length() > 1) { //so we don't overwrite a root context uri
            if (externalName.charAt(externalName.length() - 1) == '/') {
                externalName.deleteCharAt(externalName.length() - 1);
            }
            int doubleSlash = externalName.indexOf("//");
            while(doubleSlash > -1) { //removes leading '/'
                externalName.replace(doubleSlash, doubleSlash+2, "/");
                doubleSlash = externalName.indexOf("//");
            }
        }
        
        return externalName.toString();
    }

    private StringUriUtilities() {
        // Empty constructor for utility class.
    }
}
