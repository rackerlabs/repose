package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.StringUtilities;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MediaRangeParser {

    public static final Pattern ACCEPTS_HEADER_REGEX = Pattern.compile("^((([\\w]+)|(\\*))/(([ \\w\\+\\.\\-;=]*[\\w])|(((\\*)[[;q=][\\d]\\.[\\d]]*))),? ?)+$");

    public static final String QUALITIY_PARAMETER_KEY = "q";

    private MediaRangeParser() {
        
    }
    
    public static MediaRange getPerferedMediaRange(List<MediaRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            throw new IllegalArgumentException("No ranges specified to get the perfered range from");
        }

        MediaRange perferedMediaRange = ranges.get(0);
        double largestQualityFactor = 0;

        for (MediaRange nextMediaRange : ranges) {
            final String unparsedQualityFactor = nextMediaRange.getParameter(QUALITIY_PARAMETER_KEY);

            if (unparsedQualityFactor != null) {
                try {
                    final double qualityFactor = Double.parseDouble(unparsedQualityFactor);

                    if (qualityFactor < 0 || qualityFactor > 1) {
                        throw new MalformedMediaRangeException("Quality factors must be valid a decimal number from 0 to 1");
                    }

                    if (qualityFactor > largestQualityFactor) {
                        largestQualityFactor = qualityFactor;
                        perferedMediaRange = nextMediaRange;
                    }
                } catch (NumberFormatException nfe) {
                    throw new MalformedMediaRangeException("Quality factors must be valid a decimal number from 0 to 1", nfe);
                }
            }
        }

        return perferedMediaRange;
    }

    public static List<MediaRange> getMediaRangesFromAcceptHeader(String acceptHeader) {
        final List<MediaRange> mediaRanges = new LinkedList<MediaRange>();
        
        if (acceptHeader != null && StringUtilities.isNotBlank(acceptHeader)) {
            final Matcher matcher = ACCEPTS_HEADER_REGEX.matcher(acceptHeader);

            if (matcher.matches()) {
                final String[] mediaRangeStrings = acceptHeader.trim().split(",");

                for (String mediaRange : mediaRangeStrings) {
                    mediaRanges.add(parseMediaRange(mediaRange));
                }
            } else {
                throw new MalformedMediaRangeException("The request accept header is malformed: " + acceptHeader);
            }
        }
        return mediaRanges;
    }

    public static MediaRange parseMediaRange(String mediaRange) {
        if (mediaRange == null || StringUtilities.isBlank(mediaRange)) {
            throw new MalformedMediaRangeException("Media range must not be null or blank");
        }

        final Map<String, String> acceptParameters = new HashMap<String, String>();
        String mediaType = null;

        if (mediaRange.contains(";")) {
            final String[] typeAndParameters = mediaRange.toLowerCase().trim().split(";");

            if (typeAndParameters.length > 1) {
                for (int index = 1; index < typeAndParameters.length; index++) {
                    final String[] keyAndValue = typeAndParameters[index].split("=");

                    if (keyAndValue.length != 2) {
                        throw new MalformedMediaRangeException("Parameter must have a value");
                    }

                    acceptParameters.put(keyAndValue[0], keyAndValue[1]);
                }
            }

            mediaType = typeAndParameters[0];
        } else {
            mediaType = mediaRange;
        }

        return createMediaRange(mediaType, acceptParameters);
    }

    private static MediaRange createMediaRange(String mediaTypeString, Map<String, String> acceptParameters) {
        MediaRange mediaRange = new MediaRange(MediaType.UNKNOWN);
        MediaType mediaType = MediaType.fromMediaTypeString(mediaTypeString.trim());

        if (MediaType.UNKNOWN.equals(mediaType)) {
            if (mediaTypeString.contains("xml") || acceptParameters.values().contains("xml")) {
                mediaRange = new MediaRange(MediaType.APPLICATION_XML, acceptParameters, mediaTypeString);
            } else if (mediaTypeString.contains("json") || acceptParameters.values().contains("json")) {
                mediaRange = new MediaRange(MediaType.APPLICATION_JSON, acceptParameters, mediaTypeString);
            }
        } else {
            mediaRange = new MediaRange(mediaType, acceptParameters, null);
        }

        return mediaRange;
    }
}
