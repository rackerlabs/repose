package com.rackspace.papi.mocks.auth.rs11.wrappers;

import com.rackspacecloud.docs.auth.api.v1.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

public class JsonElementWrapper implements ResponseWrapper {

   @Override
   public Object wrapElement(GroupsList groups) {
      return new GroupsWrapper(groups);
   }

   @Override
   public Object wrapElement(FullToken token) {
      return new TokenWrapper(token);
   }

   @Override
   public Object wrapElement(UnauthorizedFault fault) {
      return new UnauthorizedWrapper(fault);
   }

   @Override
   public Object wrapElement(ItemNotFoundFault fault) {
      return new ItemNotFoundWrapper(fault);
   }
   
   @XmlRootElement(name="groups")
   private static class GroupsWrapper {
      private final ValuesWrapper values;
      public GroupsWrapper(GroupsList groups) {
         this.values = new ValuesWrapper(groups);
      }
      
      @XmlElement(name="groups")
      public ValuesWrapper getGroups() {
         return values;
      }
   }
   
   @XmlRootElement(name="values")
   private static class ValuesWrapper {
      private final List<Group> groups;
      public ValuesWrapper(GroupsList groups) {
         this.groups = groups.getGroup();
      }
      
      @XmlElement(name="values")
      public List<Group> getValues() {
         return groups;
      }
   }

   @XmlRootElement(name = "token")
   private static class TokenWrapper {

      private final FullToken token;

      public TokenWrapper(FullToken token) {
         this.token = token;
      }
      
      @XmlElement(name="token")
      public FullToken getToken() {
         return token;
      }
   }

   @XmlRootElement(name = "unauthorized")
   private static class UnauthorizedWrapper {

      private final UnauthorizedFault fault;

      public UnauthorizedWrapper(UnauthorizedFault fault) {
         this.fault = fault;
      }

      public UnauthorizedFault getUnathorized() {
         return fault;
      }
   }

   @XmlRootElement(name = "itemNotFound")
   private static class ItemNotFoundWrapper {

      private final ItemNotFoundFault fault;

      public ItemNotFoundWrapper(ItemNotFoundFault fault) {
         this.fault = fault;
      }

      public ItemNotFoundFault getItemNotFound() {
         return fault;
      }
   }
   
}
