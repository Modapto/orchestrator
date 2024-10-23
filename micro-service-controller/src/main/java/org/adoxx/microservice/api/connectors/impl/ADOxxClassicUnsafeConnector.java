package org.adoxx.microservice.api.connectors.impl;

import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ADOxxClassicUnsafeConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "ADOxx Classic Unsafe Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Allow to execute an ADOScript (with limited features) in ADOxx Classic.\n"
                    + "Please remind that the SOAP server must be enabled \nwith the ADOScript command: CC \"AdoScript\" SERVICE start")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("url", Json.createObjectBuilder()
                .add("name", "URL")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Endpoint of the ADOxx SOAP service to use"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        String forbiddedKeys = "";
        for(String forbidded : forbiddenList)
            forbiddedKeys += forbidded + "\n";
        return Json.createObjectBuilder()
            .add("adoScript", Json.createObjectBuilder()
                .add("name", "ADOScript")
                .add("description", Json.createObjectBuilder()
                    .add("en", "ADOScript to execute. Be aware of the following forbidded keywords:\n" + forbiddedKeys))
                .add("value", ""))
            .add("resultVariable", Json.createObjectBuilder()
                .add("name", "Result Variable")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The ADOScript defined variable value to return"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  result : ''" + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }

    private String url = "";
    private String[] forbiddenList = new String[] {
            "FOR ",
            "WHILE ",
            "CC \"AdoScript\" ",
            "CC \"CoreUI\" ",
            "CC \"UserMgt\" ",
            "CC \"ImportExport\" ",
            "CC \"Evaluation\" ",
            //"CC \"Documentation\" ",
            "CC \"Application\" ",
            "CALL ",
            "EXECUTE ",
            "EXIT ",
            "LEO ",
            "SEND ",
            "START ",
            "SYSTEM ",
            "ON_EVENT ",
            "FUNCTION ",
            "PROCEDURE "
    };
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        url = startConfiguration.getJsonObject("url")==null?"":startConfiguration.getJsonObject("url").getString("value", "").toUpperCase();
        if(url.isEmpty())
            throw new Exception("url not provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String adoScript = configuration.getJsonObject("adoScript")==null?"":configuration.getJsonObject("adoScript").getString("value", "");
        if(adoScript.isEmpty()) throw new Exception("adoScript not provided");
        String resultVariable = configuration.getJsonObject("resultVariable")==null?"":configuration.getJsonObject("resultVariable").getString("value", "");
        if(resultVariable.isEmpty()) throw new Exception("resultVariable not provided");
        
        for(String forbidden: forbiddenList)
            if(adoScript.contains(forbidden))
                throw new Exception("The use of \"" + forbidden + "\" in the provided adoScript is forbidden");
        
        String result = executeAdoScript(adoScript, resultVariable);
        
        return Json.createObjectBuilder()
            .add("result", result)
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
    }
    
    @Override
    public void stop() throws Exception {
        url = "";
    }
    
    private String executeAdoScript(String adoScript, String resultVariable) throws Exception {
        ArrayList<String[]> parameterList = new ArrayList<String[]>();
        parameterList.add(new String[] {"script", adoScript});
        parameterList.add(new String[] {"resultVar", resultVariable});
        Document resultXml = Utils.sendSOAP(url, "urn:AdoWS", "execute", "", parameterList, null);
        Node executeResponseEl = resultXml.getElementsByTagNameNS("urn:AdoWS","ExecuteResponse").item(0);
        String errorCode = executeResponseEl.getFirstChild().getTextContent();
        String result = executeResponseEl.getLastChild().getTextContent();
        if(!errorCode.equals("0"))
            throw new Exception("Error executing the script:\n" + adoScript + "\nError Code=" + errorCode + "\nError Desc:\n" + result);
        return result;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        ADOxxClassicUnsafeConnector connector = new ADOxxClassicUnsafeConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("url", Json.createObjectBuilder().add("value", "http://127.0.0.1"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("adoScript", Json.createObjectBuilder().add("value", "CC \"Core\" GET_ALL_MODEL_VERSIONS_OF_THREAD  modelthreadid:(27401)"))
                .add("resultVariable", Json.createObjectBuilder().add("value", "modelversionids"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
