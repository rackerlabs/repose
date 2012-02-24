package com.rackspace.config.manip.jmx;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;

public class ClientListener implements NotificationListener {

   @Override
   public void handleNotification(Notification notification, Object o) {

      echo("\nReceived notification:");
      echo("\tClassName: " + notification.getClass().getName());
      echo("\tSource: " + notification.getSource());
      echo("\tType: " + notification.getType());
      echo("\tMessage: " + notification.getMessage());

      if (notification instanceof AttributeChangeNotification) {
         AttributeChangeNotification acn = (AttributeChangeNotification) notification;
         echo("\tAttributeName: " + acn.getAttributeName());
         echo("\tAttributeType: " + acn.getAttributeType());
         echo("\tNewValue: " + acn.getNewValue());
         echo("\tOldValue: " + acn.getOldValue());
      }
   }

   private void echo(String message) {
      System.out.println(message);
   }
}