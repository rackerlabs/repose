package com.rackspace.papi.domain;

import org.springframework.stereotype.Component;

@Component("reposeInstanceInfo")
public class ReposeInstanceInfo {
   
   private String clusterId;
   private String nodeId;

   public void setClusterId(String clusterId) {
      this.clusterId = clusterId;
   }

   public void setNodeId(String nodeId) {
      this.nodeId = nodeId;
   }

   public ReposeInstanceInfo(String clusterId, String nodeId) {
      this.clusterId = clusterId;
      this.nodeId = nodeId;
   }

   public ReposeInstanceInfo() {
   }

   public String getClusterId() {
      return clusterId;
   }

   public String getNodeId() {
      return nodeId;
   }

   @Override
   public String toString() {
      return clusterId + ":" + nodeId;
   }
   
   
   
}
