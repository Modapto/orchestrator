package org.adoxx.microservice.api.connectors.impl;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.api.log.LogManager;
import org.adoxx.microservice.api.log.LogI.LogLevel;
import org.adoxx.microservice.utils.Utils;

public class DummySyncConnector extends SyncConnectorA{

    private String text = "";
    
    @Override
    public String getName() {
        return "Dummy Synchronous Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "A simple connector that return a preconfigured string after 3 seconds")
            .add("de", "A simple connector that return a preconfigured string after 3 seconds")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("BaseText", Json.createObjectBuilder()
                .add("name", "BaseText")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The initial text that must be returned every time this connector is called")
                    .add("de", "The initial text that must be returned every time this connector is called"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("AppendText", Json.createObjectBuilder()
                    .add("name", "AppendText")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "The text that will be appended to the initial text on every call")
                        .add("de", "The text that will be appended to the initial text on every call"))
                    .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  dataMIME : 'text/plain'," + "\n"
                + "  dataText : '_the text provided in the start configuration concatenated with the text provided in the call configuration_'," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        text = startConfiguration.getJsonObject("BaseText")==null?"":startConfiguration.getJsonObject("BaseText").getString("value", "");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String append = configuration.getJsonObject("AppendText")==null?"":configuration.getJsonObject("AppendText").getString("value", "");
        Thread.sleep(3000);
        JsonObject ret = Json.createObjectBuilder()
            .add("dataMIME", "text/plain")
            .add("dataText", text + append)
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
        
        LogManager.unique().log(LogLevel.INFO, "Connector '" + getName() + "' called. Returned: " + ret.toString(), null);
        
        return ret;
    }
    
    @Override
    public void stop() throws Exception {
        text = "";
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        DummySyncConnector connector = new DummySyncConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("BaseText", Json.createObjectBuilder().add("value", "Hi "))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("AppendText", Json.createObjectBuilder().add("value", "everyone!"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
