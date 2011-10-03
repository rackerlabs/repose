package com.rackspace.papi.commons.util;

/**
 * This is a simple helper class that can be used to generalize URI related
 * string processing.
 * 
 * @author zinic
 */
public final class StringUriUtilities {

    private StringUriUtilities() {
        // Empty constructor for utility class.
    }

    public static int indexOfUriFragment(String uri, String uriFragment) {
        final int index = uri.indexOf(uriFragment);

        if (uri.length() > uriFragment.length() + index) {
            return uri.charAt(index + uriFragment.length()) == '/' ? index : -1;
        }

        return index;
    }

    public static int indexOfUriFragment(StringBuilder uri, String uriFragment) {
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

        if (externalName.charAt(externalName.length() - 1) == '/') {
            externalName.deleteCharAt(externalName.length() - 1);
        }

        return externalName.toString();
    }
}
