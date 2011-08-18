package com.rackspace.papi.filter;

import com.rackspace.papi.filter.FilterClassFactory;
import javax.servlet.*;

import com.rackspace.papi.filter.FilterClassException;
import com.rackspace.papi.servlet.ServletContextInitException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;


/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class FilterClassFactoryTest {
    public static class WhenUsingFilterClassFactory {
        @Test
        public void shouldInstantiate() throws ClassNotFoundException {
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn((Class) Object.class);

            FilterClassFactory filterClassFactory = new FilterClassFactory("Fake class name", mockedClassLoader);

            assertNotNull(filterClassFactory);
        }        

        @Test
        public void shouldGetClassLoader() throws ClassNotFoundException, MalformedURLException {
            URL url = new URL("http://fake url");
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn((Class) Object.class);
            when(mockedClassLoader.getResource(any(String.class))).thenReturn(url);

            FilterClassFactory filterClassFactory = new FilterClassFactory("Fake class name", mockedClassLoader);

            assertTrue(url.getPath().equalsIgnoreCase(filterClassFactory.getClassLoader().getResource("").getPath()));
        }

        @Test
        public void shouldToString() throws ClassNotFoundException {
            String className = "Fake class name";
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn((Class) Object.class);

            FilterClassFactory filterClassFactory = new FilterClassFactory(className, mockedClassLoader);

            assertTrue(className.equalsIgnoreCase(filterClassFactory.toString()));
        }
    }

    public static class WhenValidatingFilterClass {
        @Test
        public void shouldValidate() throws ClassNotFoundException {
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn((Class) Filter.class);

            FilterClassFactory filterClassFactory = new FilterClassFactory("Fake class name", mockedClassLoader);

            filterClassFactory.validate(Filter.class);
        }

        @Test(expected=ServletContextInitException.class)
        public void shouldThrowServletContextInitExceptionForNullClassOnValidate() throws ClassNotFoundException {
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn(null);

            FilterClassFactory filterClassFactory = new FilterClassFactory("Fake class name", mockedClassLoader);

            filterClassFactory.validate(null);
        }

        @Test(expected=ServletContextInitException.class)
        public void shouldThrowServletContextInitExceptionForNonFilterClassOnValidate() throws ClassNotFoundException {
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn((Class) Object.class);

            FilterClassFactory filterClassFactory = new FilterClassFactory("Fake class name", mockedClassLoader);

            filterClassFactory.validate(null);
        }
    }

    public static class WhenCreatingNewClassInstance {
        @Test
        public void shouldCreateNewInstance() throws ClassNotFoundException {
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn((Class) FakeFilterClass.class);

            FilterClassFactory filterClassFactory = new FilterClassFactory("Fake class name", mockedClassLoader);

            Filter newFilter = filterClassFactory.newInstance();

            assertNotNull(newFilter);
        }

        @Test(expected= FilterClassException.class)
        public void shouldThrowFilterClassExceptionOnInstantiationException() throws ClassNotFoundException {
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn((Class) Filter.class);

            FilterClassFactory filterClassFactory = new FilterClassFactory("Fake class name", mockedClassLoader);

            filterClassFactory.newInstance();
        }

        @Test(expected= FilterClassException.class)
        public void shouldThrowFilterClassExceptionOnIllegalAccessException() throws ClassNotFoundException {
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedClassLoader.loadClass(any(String.class))).thenReturn((Class) FakeFilterClassWithPrivateConstructor.class);

            FilterClassFactory filterClassFactory = new FilterClassFactory("Fake class name", mockedClassLoader);

            filterClassFactory.newInstance();
        }        
    }
}
