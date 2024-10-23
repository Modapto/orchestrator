package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.MicroserviceController;
import org.adoxx.microservice.api.connectors.AsyncConnectorA;
import org.adoxx.microservice.api.connectors.AsyncResponseHandlerI;

public class MicroserviceConnector extends AsyncConnectorA {
    private boolean terminate = true;
    private AsyncResponseHandlerI responseHandler = null;
    private JsonObject startConfiguration = null;
    
    @Override
    public String getName() {
        return "Microservice Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Module for contacting automatically in background or on request, a microservice")
            .add("de", "Module for contacting automatically in background or on request, a microservice")
            .build();
    }

    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder().build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("microserviceId", Json.createObjectBuilder()
                .add("name", "Microservice Id")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The Id of the microservice to contact")
                    .add("de", "The Id of the microservice to contact"))
                .add("value", ""))
            .add("microserviceOperationId", Json.createObjectBuilder()
                .add("name", "Microservice Operation Id")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The Id of the microservice operation to contact")
                    .add("de", "The Id of the microservice operation to contact"))
                .add("value", ""))
            .add("microserviceOperationInput", Json.createObjectBuilder()
                .add("name", "Microservice Operation Input")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The JSON encoded string of the inputs required by the microservice operation")
                    .add("de", "The JSON encoded string of the inputs required by the microservice operation"))
                .add("value", ""))
            .add("automaticCallInterval", Json.createObjectBuilder()
                .add("name", "Automatic Call Interval")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The interval in minutes between every automatic call to the microservice. If left empty the automatic call is disabled")
                    .add("de", "The interval in minutes between every automatic call to the microservice. If left empty the automatic call is disabled"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "The output will be the same returned by the microservice configured in the start configuration";
    }
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        if(responseHandler == null) throw new Exception("responseHandler must be provided before calling this method");
        if(this.startConfiguration != null) return; //means is already started so I do nothing
        this.startConfiguration = startConfiguration;
        String microserviceId = startConfiguration.getJsonObject("microserviceId")==null?"":startConfiguration.getJsonObject("microserviceId").getString("value");
        String microserviceOperationId = startConfiguration.getJsonObject("microserviceOperationId")==null?"":startConfiguration.getJsonObject("microserviceOperationId").getString("value");
        String microserviceOperationInputS = startConfiguration.getJsonObject("microserviceOperationInput")==null?"{}":startConfiguration.getJsonObject("microserviceOperationInput").getString("value", "{}");
        String minutesS = startConfiguration.getJsonObject("automaticCallInterval")==null?"":startConfiguration.getJsonObject("automaticCallInterval").getString("value", "");
        JsonObject microserviceOperationInput = Json.createReader(new StringReader(microserviceOperationInputS)).readObject();
        int minutes = minutesS.isEmpty()?0:Integer.parseInt(minutesS);
        terminate = false;
        if(minutes <= 0)
            terminate = true;
        while(!terminate) {
            setPreStarted();
            Thread.sleep(1000*60*minutes);
            JsonObject microserviceResponse = MicroserviceController.unique().callMicroserviceForced(microserviceId, microserviceOperationId, microserviceOperationInput);
            responseHandler.handler(microserviceResponse);
        }
    }

    @Override
    public JsonObject performCall(JsonObject callConfiguration) throws Exception {
        String microserviceId = this.startConfiguration.getJsonObject("microserviceId")==null?"":this.startConfiguration.getJsonObject("microserviceId").getString("value");
        String microserviceOperationId = this.startConfiguration.getJsonObject("microserviceOperationId")==null?"":this.startConfiguration.getJsonObject("microserviceOperationId").getString("value");
        String microserviceOperationInputS = this.startConfiguration.getJsonObject("microserviceOperationInput")==null?"{}":this.startConfiguration.getJsonObject("microserviceOperationInput").getString("value", "{}");
        JsonObject microserviceOperationInput = Json.createReader(new StringReader(microserviceOperationInputS)).readObject();
        JsonObject microserviceResponse = MicroserviceController.unique().callMicroserviceForced(microserviceId, microserviceOperationId, microserviceOperationInput);
        if(responseHandler != null)
            responseHandler.handler(microserviceResponse);
        return microserviceResponse;
    }

    @Override
    public void stop() throws Exception {
        this.startConfiguration = null;
        terminate = true;
    }

    @Override
    public void setAsyncResponsesHandler(AsyncResponseHandlerI asyncResponseHandler) {
        responseHandler = asyncResponseHandler;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        MicroserviceConnector connector = new MicroserviceConnector();
        try {
            connector.setAsyncResponsesHandler(new AsyncResponseHandlerI() {
                @Override
                public void handler(JsonObject asyncResponse) {
                    System.out.println("Async response : " + asyncResponse.toString());
                }
            });
            connector.threadStart(Json.createObjectBuilder()
                .add("microserviceId", Json.createObjectBuilder().add("value", "TESTSYNC"))
                .add("microserviceOperationId", Json.createObjectBuilder().add("value", "default"))
                .add("microserviceOperationInput", Json.createObjectBuilder().add("value", "{\"Your Name\":{\"value\":\"Dam\"}}"))
                .add("automaticCallInterval", Json.createObjectBuilder().add("value", "1"))
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
