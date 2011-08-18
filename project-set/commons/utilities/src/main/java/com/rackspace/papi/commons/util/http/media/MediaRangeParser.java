package com.rackspace.papi.commons.util.http.media;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * 
 */
public abstract class MediaRangeParser {
    static final Pattern ACCEPTS_HEADER_REGEX = Pattern.compile("^((([\\w]+)|(\\*))/(([\\w][ \\w\\+\\.\\-\\;=]*[\\w])|(\\*)),? ?)+$");

    public static List<MediaRange> getMediaRangesFromAcceptHeader(String acceptHeader) {
        final Matcher matcher = ACCEPTS_HEADER_REGEX.matcher(acceptHeader);
        List<MediaRange> mediaRanges;

        if (matcher.matches()) {        
            final String[] mediaRangeStrings = acceptHeader.trim().replaceAll("\\s+","").split(",");
            mediaRanges = new ArrayList<MediaRange>();

            for (String mediaRange : mediaRangeStrings) {
                mediaRanges.add(parseMediaRange(mediaRange));
            }
        } else {
            throw new MalformedMediaRangeException("The request accept header is malformed: " + acceptHeader);
        }

        return mediaRanges;
    }

    public static MediaRange parseMediaRange(String mediaRange) {
        String[] typeAndParameters = mediaRange.toLowerCase().trim().replaceAll("\\s+","").split(";");
        Set<String> acceptParameters = null;

        // Get parameters (e.g. v=1;q=1)
        if (typeAndParameters.length > 1) {
            acceptParameters = new HashSet<String>();
            List<String> parameters = new ArrayList<String>(Arrays.asList(typeAndParameters));
            parameters.remove(0);

            for (String parameter : parameters) {
                acceptParameters.add(parameter);
            }
        }
        
        return createMediaRange(typeAndParameters[0], acceptParameters);
    }

    private static MediaRange createMediaRange(String mediaTypeString, Set<String> acceptParameters) {
        MediaRange mediaRange = new MediaRange(MediaType.UNKNOWN, null, null);
        MediaType mediaType = MediaType.fromMediaTypeString(mediaTypeString);

        if (MediaType.UNKNOWN.equals(mediaType)) {
            if (mediaTypeString.contains("xml") || setContainsType("xml", acceptParameters)) {
                mediaRange = new MediaRange(MediaType.APPLICATION_XML, acceptParameters, mediaTypeString);
            } else if (mediaTypeString.contains("json") || setContainsType("json", acceptParameters)){
                mediaRange = new MediaRange(MediaType.APPLICATION_JSON, acceptParameters, mediaTypeString);
            }
        } else {
            mediaRange = new MediaRange(mediaType, acceptParameters, null);
        }

        return mediaRange;
    }

    public static boolean setContainsType(String type, Set<String> acceptParameters) {
        boolean containsMediaType = false;

        if (acceptParameters != null) {
            for (String parameter : acceptParameters) {
                if (parameter.contains(type)) {
                    containsMediaType = true;
                    break;
                }
            }
        }

        return containsMediaType;
    }
}
