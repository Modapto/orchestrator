package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.api.log.LogManager;
import org.adoxx.microservice.api.log.LogI.LogLevel;
import org.adoxx.microservice.utils.Utils;

import net.sf.saxon.TransformerFactoryImpl;

public class XSLTConnector extends SyncConnectorA{
    
    @Override
    public String getName() {
        return "XSLT Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "A connector that perform XSLT Transformation")
            .add("de", "A connector that perform XSLT Transformation")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("xslt", Json.createObjectBuilder()
                .add("name", "xslt")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The XSLT to use for the transformation")
                    .add("de", "The XSLT to use for the transformation"))
                .add("value", ""))
            .add("xml", Json.createObjectBuilder()
                    .add("name", "xml")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "The xml to transform")
                        .add("de", "The xml to transform"))
                    .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  xml : '...'," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String xslt = configuration.getJsonObject("xslt")==null?"":configuration.getJsonObject("xslt").getString("value", "");
        String xml = configuration.getJsonObject("xml")==null?"":configuration.getJsonObject("xml").getString("value", "");

        TransformerFactory factory = TransformerFactoryImpl.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);

        Transformer transformer = factory.newTransformer(new StreamSource(new StringReader(xslt)));
        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));

        JsonObject ret = Json.createObjectBuilder()
            .add("xml", writer.toString())
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
        
        LogManager.unique().log(LogLevel.INFO, "Connector '" + getName() + "' called. Returned: " + ret.toString(), null);
        
        return ret;
    }
    
    @Override
    public void stop() throws Exception {
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        XSLTConnector connector = new XSLTConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("xslt", Json.createObjectBuilder().add("value", "<?xml version=\"1.0\"?><xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:template match=\"/\"><result><xsl:value-of select=\"root/name\"/></result></xsl:template></xsl:stylesheet>"))
                .add("xml", Json.createObjectBuilder().add("value", "<root><name>Damiano</name></root>"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
