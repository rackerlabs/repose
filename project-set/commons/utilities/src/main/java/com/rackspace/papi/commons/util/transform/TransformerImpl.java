package com.rackspace.papi.commons.util.transform;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.InputStream;
import java.io.OutputStream;

public class TransformerImpl implements com.rackspace.papi.commons.util.transform.Transformer {
    private final TransformerFactory factory;

    public TransformerImpl(String transformerType){
        System.setProperty("javax.xml.transform.TransformerFactory", transformerType);
        factory = TransformerFactory.newInstance();
    }

    public void transform(InputStream inputStream, String transformationFile, OutputStream outputStream) {

        try {
            Templates templates = factory.newTemplates(new StreamSource(TransformerImpl.class.getResourceAsStream(transformationFile)));

            templates.newTransformer().transform(new StreamSource(inputStream), new StreamResult(outputStream));
        } catch (TransformerException e) {
            throw new TransformationException("The translation could not be performed.", e);
        }
    }
}
