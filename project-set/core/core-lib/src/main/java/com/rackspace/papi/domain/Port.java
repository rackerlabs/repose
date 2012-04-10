package com.rackspace.papi.domain;

public class Port {
   private final String protocol;
   private final int port;
   
   public Port(String protocol, int port) {
      this.protocol = protocol;
      this.port = port;
   }

   public String getProtocol() {
      return protocol;
   }

   public int getPort() {
      return port;
   }
   
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof Port)) {
         return false;
      }
      
      Port p = (Port)other;
      
      if (protocol != null) {
         return port == p.getPort() && protocol.equalsIgnoreCase(p.getProtocol());
      }
      
      return false;
   }

   @Override
   public int hashCode() {
      int hash = 3;
      hash = 71 * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
      hash = 71 * hash + this.port;
      return hash;
   }
}
