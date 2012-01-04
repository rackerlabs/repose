package org.openrepose.rnxp.servlet.context;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.QualityFactorUtility;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;

/**
 *
 * @author zinic
 */
public class ExternalRoutableRequestDispatcher implements RequestDispatcher {

   private final OriginConnectionFuture originConnectionFuture;

   public ExternalRoutableRequestDispatcher(OriginConnectionFuture originConnectionFuture) {
      this.originConnectionFuture = originConnectionFuture;
   }

   @Override
   public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
      final HttpServletRequest httpRequest = (HttpServletRequest) request;

      // So you want to go somewhere with this request? Okay, where to, boss?
      final List<HeaderValue> headerValues = new HeaderFieldParser(httpRequest.getHeaders(PowerApiHeader.NEXT_ROUTE.getHeaderKey())).parse();

      if (!headerValues.isEmpty()) {
         final HeaderValue nextRoute = QualityFactorUtility.choosePreferedHeaderValue(headerValues);
         final URL destination = asURL(nextRoute.getValue());

         final InetSocketAddress address = new InetSocketAddress(destination.getHost(), httpPort(destination));
         originConnectionFuture.connect(address);
      }

      throw new IllegalArgumentException("Request opting to forward must forward to a routable destination");
   }

   private static int httpPort(URL url) {
      final int urlPort = url.getPort();

      return urlPort == -1 ? 80 : urlPort;
   }

   private URL asURL(String url) throws ServletException {
      try {
         return new URL(url);
      } catch (MalformedURLException exception) {
         throw new ServletException("Destination route URL is invalid. URL: " + url, exception);
      }
   }

   @Override
   public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
      throw new UnsupportedOperationException("Includes are not supported with this request dispatcher");
   }
}
