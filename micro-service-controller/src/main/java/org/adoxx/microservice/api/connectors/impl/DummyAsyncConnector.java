package org.adoxx.microservice.api.connectors.impl;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.AsyncConnectorA;
import org.adoxx.microservice.api.connectors.AsyncResponseHandlerI;
import org.adoxx.microservice.api.log.LogManager;
import org.adoxx.microservice.api.log.LogI.LogLevel;
import org.adoxx.microservice.utils.Utils;

public class DummyAsyncConnector extends AsyncConnectorA{

    private String text = "";
    private boolean terminate = false;
    private AsyncResponseHandlerI responseHandler = null;
    
    @Override
    public String getName() {
        return "Dummy Asynchronous Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "A simple connector that return a preconfigured string every minute")
            .add("de", "A simple connector that return a preconfigured string every minute")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("Text", Json.createObjectBuilder()
                .add("name", "Text")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The text that must be returned")
                    .add("de", "The text that must be returned"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  dataMIME : 'text/plain'," + "\n"
                + "  dataText : '_the text provided in the start configuration_'," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        text = startConfiguration.getJsonObject("Text")==null?"":startConfiguration.getJsonObject("Text").getString("value", "");
        terminate = false;
        while(!terminate) {
            setPreStarted();
            Thread.sleep(1000*60);
            responseHandler.handler(createResponse());
        }
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        JsonObject ret = createResponse();
        LogManager.unique().log(LogLevel.INFO, "Connector '" + getName() + "' called. Returned: " + ret.toString(), null);
        if(responseHandler != null)
            responseHandler.handler(ret);
        return ret;
    }
    
    @Override
    public void stop() throws Exception {
        text = "";
        terminate = true;
    }

    @Override
    public void setAsyncResponsesHandler(AsyncResponseHandlerI asyncResponseHandler) {
        responseHandler = asyncResponseHandler;
    }
    
    private JsonObject createResponse() throws Exception {
        return Json.createObjectBuilder()
            .add("dataMIME", "text/plain")
            .add("dataText", text)
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        DummyAsyncConnector connector = new DummyAsyncConnector();
        try{
            connector.setAsyncResponsesHandler(new AsyncResponseHandlerI() {
                @Override
                public void handler(JsonObject asyncResponse) {
                    System.out.println("Async response : " + asyncResponse.toString());
                }
            });
            connector.threadStart(Json.createObjectBuilder()
                .add("Text", Json.createObjectBuilder().add("value", "Hi"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder().build());
            System.out.println("Call Output: " + callOutputJson);
        } finally {
            //connector.threadStop();
        }
    }
    */
}
