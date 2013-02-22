package com.rackspace.papi.commons.util.http.header;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zinic
 */
public class HeaderValueParser {

  private final String rawHeaderValue;

  public HeaderValueParser(String rawValue) {
    this.rawHeaderValue = rawValue;
  }

  public HeaderValue parse() throws MalformedHeaderValueException {
    final Map<String, String> parameters = new HashMap<String, String>();
    final String[] parameterSplit = rawHeaderValue.split(";");
    final StringBuilder value = new StringBuilder(parameterSplit[0]);

    if (parameterSplit.length > 1) {
      // Start at index 1 since that's the actual header value
      for (int i = 1; i < parameterSplit.length; i++) {
        String param = parameterSplit[i];
        if (param.contains("=")) {
          parseParameter(parameters, parameterSplit[i]);

        } else {
          value.append(";").append(param);
        }
      }
    }

    return new HeaderValueImpl(value.toString().trim(), parameters);
  }

  private void parseParameter(Map<String, String> parameters, String unparsedParameter) throws MalformedHeaderValueException {
    final String[] keyValueSplit = unparsedParameter.split("=");

    // For a possible parameter to be valid it must have the '=' character
    if (keyValueSplit.length == 2) {
      parameters.put(keyValueSplit[0].trim(), keyValueSplit[1].trim());
    } else {
      throw new MalformedHeaderValueException("Valid parameter expected for header. Got: " + unparsedParameter);
    }
  }
}
