package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class JavascriptEngineConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "Javascript Nashorn Engine Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Execute a Javascript using the Nashorn engine and return its output")
            .add("de", "Execute a Javascript using the Nashorn engine and return its output")
            .build();
    }
    
    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder().build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("algorithm", Json.createObjectBuilder()
                .add("name", "JS Algorithm")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The Javascript code to execute in the Nahsorn Engine")
                    .add("de", "The Javascript code to execute in the Nahsorn Engine"))
                .add("value", ""))
            .build();
    }

    @Override
    public String getOutputDescription() throws Exception {
        return "A Json object as returned from the execution of the algorithm provided in the call configuration.";
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        
    }

    @Override
    public JsonObject performCall(JsonObject callConfiguration) throws Exception {
        String algorithm = callConfiguration.getJsonObject("algorithm")==null?"":callConfiguration.getJsonObject("algorithm").getString("value", "");
        if(algorithm.isEmpty()) throw new Exception("algorithm not provided");
        
        Object javascriptOutput = Utils.javascriptSafeEval(new HashMap<String, Object>(), algorithm, true);
        
        if(!(javascriptOutput instanceof String))
            throw new Exception("The returned object can be only a JSON string. Obtained: " + javascriptOutput.toString());
        try {
            return Json.createReader(new StringReader((String)javascriptOutput)).readObject();
        } catch(Exception ex) {
            throw new Exception("The returned object is an invalid JSON string: " + javascriptOutput + "\n\nError: " + ex.toString());
        }
    }

    @Override
    public void stop() throws Exception {
        
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        JavascriptEngineConnector connector = new JavascriptEngineConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("algorithm", Json.createObjectBuilder().add("value", "load('https://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.8.3/underscore-min.js'); out({min: _.min([10, 5])});"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
