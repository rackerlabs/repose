package com.rackspace.papi.components.datastore.hash.remote;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.junit.Ignore;

/**
 *
 * @author zinic
 */
@Ignore
@RunWith(Enclosed.class)
public class RemoteCommandExecutorTest {
/*
   public static final String HOST_KEY = "host_key";

   public static class WhenExecutingRemoteCommands {

      protected RemoteCommandExecutor executor;

      @Before
      public void standUp() throws Exception {
         final HttpResponse mockedResponse = mock(HttpResponse.class);
         final HttpClient mockedClient = mock(HttpClient.class);

         when(mockedClient.execute(any(HttpRequestBase.class))).thenReturn(mockedResponse);

         final Pool<HttpClient> httpClientPool = new GenericBlockingResourcePool<HttpClient>(new ConstructionStrategy<HttpClient>() {

            @Override
            public HttpClient construct() throws ResourceConstructionException {
               return mockedClient;
            }
         });

         executor = new RemoteCommandExecutor(httpClientPool);
         executor.setHostKey(HOST_KEY);
      }

      @Test
      public void shouldPerformRemoteCommand() {
         final Object result = executor.execute(new RemoteCommand() {

            @Override
            public HttpRequestBase buildRequest() {
               return mock(HttpRequestBase.class);
            }

            @Override
            public Object handleResponse(HttpResponse httpResponse) throws IOException, DatastoreOperationException {
               return Boolean.TRUE;
            }

            @Override
            public void setHostKey(String hostKey) {
               assertEquals("Host key must be set in the remote command", HOST_KEY, hostKey);
            }
         });

         assertEquals("Executor must return the remote command's response", Boolean.TRUE, result);
      }

      @Test(expected = DatastoreOperationException.class)
      public void shouldNotCatchDatastoreOperationExceptions() {
         final Object result = executor.execute(new RemoteCommand() {

            @Override
            public HttpRequestBase buildRequest() {
               return mock(HttpRequestBase.class);
            }

            @Override
            public Object handleResponse(HttpResponse httpResponse) throws IOException, DatastoreOperationException {
               throw new DatastoreOperationException("Failure");
            }

            @Override
            public void setHostKey(String hostKey) {
               assertEquals("Host key must be set in the remote command", HOST_KEY, hostKey);
            }
         });

         assertEquals("Executor must return the remote command's response", Boolean.TRUE, result);
      }

      @Test(expected = RemoteConnectionException.class)
      public void shouldCatchIOExceptions() {
         final Object result = executor.execute(new RemoteCommand() {

            @Override
            public HttpRequestBase buildRequest() {
               return mock(HttpRequestBase.class);
            }

            @Override
            public Object handleResponse(HttpResponse httpResponse) throws IOException, DatastoreOperationException {
               throw new IOException("Failure");
            }

            @Override
            public void setHostKey(String hostKey) {
               assertEquals("Host key must be set in the remote command", HOST_KEY, hostKey);
            }
         });

         assertEquals("Executor must return the remote command's response", Boolean.TRUE, result);
      }
   }
   * 
   */
}
