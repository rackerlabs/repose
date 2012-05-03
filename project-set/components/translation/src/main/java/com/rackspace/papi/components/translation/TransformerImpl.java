package com.rackspace.papi.components.translation;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class TransformerImpl implements Transformer {
    private final TransformerFactory factory;
    private final List<String> errors;

    public TransformerImpl(String transformerType, ClassLoader classloader){
//        System.setProperty("javax.xml.transform.TransformerFactory", transformerType);
        factory = TransformerFactory.newInstance(transformerType, classloader);

        errors = new ArrayList<String>();
         factory.setErrorListener(new ErrorListener() {
           public void error(TransformerException e) {
             String msg = e.getMessageAndLocation();
             errors.add("ERROR: " + msg);
           }
           public void fatalError(TransformerException e) {
             String msg = e.getMessageAndLocation();
             errors.add("FATAL ERROR: " + msg);
           }
           public void warning(TransformerException e) {
             String msg = e.getMessageAndLocation();
             errors.add("WARN: " + msg);
           }
         });
    }

    public void transform(InputStream inputStream, InputStream transformationFile, OutputStream outputStream) {

        try {
            
            Templates templates = factory.newTemplates(new StreamSource(transformationFile));

            templates.newTransformer().transform(new StreamSource(inputStream), new StreamResult(outputStream));
        } catch (TransformerException e) {
            throw new TransformationException("The translation could not be performed. Reason: " + e.getMessage(), e);
        }
    }
}
