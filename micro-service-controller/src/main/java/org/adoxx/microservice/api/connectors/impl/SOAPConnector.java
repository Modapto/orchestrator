package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.w3c.dom.Document;

public class SOAPConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "SOAP Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Get data from a SOAP service")
            .add("de", "Get data from a SOAP service")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder().build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("url", Json.createObjectBuilder()
                .add("name", "URL")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Url of the SOAP service")
                    .add("de", "Url of the SOAP service"))
                .add("value", ""))
            .add("methodNamespace", Json.createObjectBuilder()
                .add("name", "Method Namespace")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Namespace of the method to call")
                    .add("de", "Namespace of the method to call"))
                .add("value", ""))
            .add("methodName", Json.createObjectBuilder()
                .add("name", "Method Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Name of the method to call")
                    .add("de", "Name of the method to call"))
                .add("value", ""))
            .add("soapAction", Json.createObjectBuilder()
                .add("name", "SOAP Action")
                .add("description", Json.createObjectBuilder()
                    .add("en", "SOAP Action HTTP parameter (optional)")
                    .add("de", "SOAP Action HTTP parameter (optional)"))
                .add("value", ""))
            .add("parameters", Json.createObjectBuilder()
                .add("name", "Parameters")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Parameters (expressed in JSON format) to provide to the SOAP method to call (ex. {\"param1\":\"val1\", \"param2\":\"val2\"})")
                    .add("de", "Parameters (expressed in JSON format) to provide to the SOAP method to call (ex. {\"param1\":\"val1\", \"param2\":\"val2\"})"))
                .add("value", "")
                .add("sample", "{\"param1\":\"val1\", \"param2\":\"val2\"}"))
            .add("additionalHeaders", Json.createObjectBuilder()
                .add("name", "Additional Headers")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Additional headers to append to the request, separated by \\n (\"Accept: application/json\\nAuthorization: Basic YTphQGE=\")")
                    .add("de", "Additional headers to append to the request, separated by \\n (\"Accept: application/json\\nAuthorization: Basic YTphQGE=\")"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  SOAPResponse: '_PlainText_'" + "\n"
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
        String url = configuration.getJsonObject("url")==null?"":configuration.getJsonObject("url").getString("value", "");
        if(url.isEmpty()) throw new Exception("url not provided");
        String methodNamespace = configuration.getJsonObject("methodNamespace")==null?"":configuration.getJsonObject("methodNamespace").getString("value", "");
        if(methodNamespace.isEmpty()) throw new Exception("methodNamespace not provided");
        String methodName = configuration.getJsonObject("methodName")==null?"":configuration.getJsonObject("methodName").getString("value", "");
        if(methodName.isEmpty()) throw new Exception("methodName not provided");
        String soapAction = configuration.getJsonObject("soapAction")==null?"":configuration.getJsonObject("soapAction").getString("value", "");
        String parameters = configuration.getJsonObject("parameters")==null?"{}":configuration.getJsonObject("parameters").getString("value", "{}");
        if(parameters.isEmpty()) throw new Exception("parameters not provided");
        String additionalHeaders = configuration.getJsonObject("additionalHeaders")==null?"":configuration.getJsonObject("additionalHeaders").getString("value", "");

        JsonObject parametersJson = null;
        try{
            parametersJson = Json.createReader(new StringReader(parameters)).readObject();
        }catch(Exception ex) {
            throw new Exception("parameters is not in the JSON format: " + parameters + "\n" + ex.getMessage());
        }
        ArrayList<String[]> parameterList = new ArrayList<String[]>();
        for(Entry<String, JsonValue> entry : parametersJson.entrySet()) {
            if(entry.getValue().getValueType().compareTo(ValueType.STRING) != 0) 
                throw new Exception("The value of the parameter " + entry.getKey() + " is not a String");
            parameterList.add(new String[] {entry.getKey(), ((JsonString)entry.getValue()).getString()});
        }
        
        ArrayList<String[]> additionalHtmlHeaderList = new ArrayList<String[]>();
        if(!additionalHeaders.isEmpty())
            for(String additionalHeader : additionalHeaders.split("\n"))
                additionalHtmlHeaderList.add(new String[]{additionalHeader.split(":")[0].trim(), additionalHeader.split(":")[1].trim()});
        
        Document resultXml = Utils.sendSOAP(url, methodNamespace, methodName, soapAction, parameterList, additionalHtmlHeaderList);
        
        return Json.createObjectBuilder()
            .add("SOAPResponse", Utils.getStringFromXmlDoc(resultXml))
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
    }
    
    @Override
    public void stop() throws Exception {
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        SOAPConnector connector = new SOAPConnector();
        try{
            connector.threadStart(Json.createObjectBuilder().build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("url", Json.createObjectBuilder().add("value", "http://127.0.0.1"))
                .add("methodNamespace", Json.createObjectBuilder().add("value", "urn:AdoWS"))
                .add("methodName", Json.createObjectBuilder().add("value", "execute"))
                .add("soapAction", Json.createObjectBuilder().add("value", ""))
                .add("parameters", Json.createObjectBuilder().add("value", Json.createObjectBuilder()
                        .add("script", "CC \"AdoScript\" INFOBOX \"test\"")
                        .add("resultVar", "result")
                        .build().toString()))
                .add("additionalHeaders", Json.createObjectBuilder().add("value", ""))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
