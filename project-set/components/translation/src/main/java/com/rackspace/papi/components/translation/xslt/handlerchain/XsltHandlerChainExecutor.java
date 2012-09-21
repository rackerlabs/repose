package com.rackspace.papi.components.translation.xslt.handlerchain;

import com.rackspace.papi.components.translation.xslt.AbstractChainExecutor;
import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.io.*;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XsltHandlerChainExecutor extends AbstractChainExecutor {

    private final XsltHandlerChain chain;

    public XsltHandlerChainExecutor(SAXTransformerFactory factory, XsltHandlerChain chain) {
        super(factory);
        this.chain = chain;
    }

    public void executeChain(InputStream in, OutputStream out, List<Parameter> inputs, List<Parameter<? extends OutputStream>> outputs) throws XsltException {
        try {
            XMLReader reader = getSaxReader();
            // TODO: Make validation optional
            reader.setFeature("http://xml.org/sax/features/validation", false);

            if (!chain.getFilters().isEmpty()) {

                TransformerHandler lastHandler = null;
                
                for (TransformReference<Templates> template : chain.getFilters()) {
                    TransformerHandler handler = getFactory().newTransformerHandler(template.getFilter());
                    Transformer transformer = handler.getTransformer();
                    transformer.clearParameters();
                    
                    if (lastHandler == null) {
                        reader.setContentHandler(handler);
                    } else {
                        lastHandler.setResult(new SAXResult(handler));
                    }

                    //transformer.setURIResolver(new ClassPathUriResolver(transformer.getURIResolver()));
                    setInputParameters(template.getId(), transformer, inputs);
                    setAlternateOutputs(template.getId(), transformer, outputs);
                    lastHandler = handler;
                }

                // Set the result of the last handler to be the output stream
                lastHandler.setResult(new StreamResult(out));
            } else {
                TransformerHandler handler = getFactory().newTransformerHandler();
                reader.setContentHandler(handler);
                handler.setResult(new StreamResult(out));
            }

            reader.parse(new InputSource(in));
            //in.close();
        } catch (TransformerConfigurationException ex) {
            throw new XsltException(ex);
        } catch (IOException ex) {
            throw new XsltException(ex);
        } catch (ParserConfigurationException ex) {
            throw new XsltException(ex);
        } catch (SAXException ex) {
            throw new XsltException(ex);
        }
    }

}
