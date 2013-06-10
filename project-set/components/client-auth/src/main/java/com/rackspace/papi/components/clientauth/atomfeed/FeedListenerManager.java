package com.rackspace.papi.components.clientauth.atomfeed;

// Class that will manage atom feed pollers which will delete items from cache
import com.rackspace.papi.service.datastore.Datastore;
import java.util.List;
import org.slf4j.LoggerFactory;


/*
 * Controls the feed listener
 */
public class FeedListenerManager {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FeedListenerManager.class);
   private final Thread authFeedListenerThread;
   private FeedCacheInvalidator listener;
   
   public FeedListenerManager(Datastore datastore, List<AuthFeedReader> readers, long checkInterval) {
   
      listener = FeedCacheInvalidator.openStackInstance(datastore, checkInterval);
      listener.setFeeds(readers);
      authFeedListenerThread = new Thread(listener);
      
      
   }
   
   public void startReading(){
      LOG.debug("Starting Feed Listener Manager");
      authFeedListenerThread.start();
   }
   
   
   
   public void stopReading(){
      try {
         LOG.debug("Stopping Feed Listener Manager");
         listener.done();
         authFeedListenerThread.interrupt();
         authFeedListenerThread.join();
      } catch (InterruptedException ex) {
         LOG.error("Unable to shutdown Auth Feed Listener Thread", ex);
      }
   }
   
   
}
