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

   private static final int BASE_HASH = 3;
   private static final int PRIME = 71;
   @Override
   public int hashCode() {
      int hash = BASE_HASH;
      hash = PRIME * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
      hash = PRIME * hash + this.port;
      return hash;
   }
}
