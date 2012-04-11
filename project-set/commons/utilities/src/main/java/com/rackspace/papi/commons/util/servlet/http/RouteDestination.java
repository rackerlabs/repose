package com.rackspace.papi.commons.util.servlet.http;

public class RouteDestination implements Comparable {

   private final String destinationId;
   private final String uri;
   private final float quality;

   @Override
   public int compareTo(Object o) {
      if (o == null || !(o instanceof RouteDestination)) {
         throw new IllegalArgumentException("Cannot compare to null instance");
      }
      
      RouteDestination r = (RouteDestination)o;
      
      int result = Double.compare(quality, r.quality);
      
      if (result == 0) {
         result = destinationId.compareTo(r.destinationId);
      }
      
      if (result == 0) {
         result = uri.compareTo(r.uri);
      }
      
      return result;
   }
   
   @Override
   public boolean equals(Object o) {
      return compareTo(o) == 0;
   }

   @Override
   public int hashCode() {
      int hash = 3;
      hash = 79 * hash + (this.destinationId != null ? this.destinationId.hashCode() : 0);
      hash = 79 * hash + (this.uri != null ? this.uri.hashCode() : 0);
      hash = 79 * hash + Float.floatToIntBits(this.quality);
      return hash;
   }

   public RouteDestination(String destinationId, String uri, float quality) {
      if (destinationId == null) {
         throw new IllegalArgumentException("destinationId cannot be null");
      }
      
      this.destinationId = destinationId;
      this.uri = uri != null? uri: "";
      this.quality = quality;
   }

   public String getDestinationId() {
      return destinationId;
   }

   public String getUri() {
      return uri;
   }

   public float getQuality() {
      return quality;
   }
}
