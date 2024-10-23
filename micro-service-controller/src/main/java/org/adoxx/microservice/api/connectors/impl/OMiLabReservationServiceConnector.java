package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.MicroserviceController;
import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.adoxx.microservice.utils.Utils.HttpResults;

public class OMiLabReservationServiceConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "OMiLab Reservation Service Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "OMiLab Reservation Service Connector")
            .add("de", "OMiLab Reservation Service Connector")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("labReservationUrl", Json.createObjectBuilder()
                .add("name", "Lab Reservation Url")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Lab Reservation Url")
                    .add("de", "Lab Reservation Url"))
                .add("value", ""))
            .add("labReservationDeviceId", Json.createObjectBuilder()
                    .add("name", "Lab Reservation DeviceId")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "Lab Reservation DeviceId (eg. dobot, mbot)")
                        .add("de", "Lab Reservation DeviceId (eg. dobot, mbot)"))
                    .add("value", ""))
            .add("microserviceId", Json.createObjectBuilder()
                .add("name", "Microservice ID")
                .add("description", Json.createObjectBuilder()
                    .add("en", "ID of the Microservice to execute if the reservation check is successfully")
                    .add("de", "ID of the Microservice to execute if the reservation check is successfully"))
                .add("value", ""))
            .add("microserviceOperationId", Json.createObjectBuilder()
                    .add("name", "Microservice Operation ID")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "ID of the Microservice Operation to execute if the reservation check is successfully")
                        .add("de", "ID of the Microservice Operation to execute if the reservation check is successfully"))
                    .add("value", "microserviceOperationId"))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("microserviceOperationInput", Json.createObjectBuilder()
                .add("name", "Microservice Operation Input")
                .add("description", Json.createObjectBuilder()
                    .add("en", "JSON input for the microservice specified in the start configuration")
                    .add("de", "JSON input for the microservice specified in the start configuration"))
                .add("value", ""))
            .add("reservationToken", Json.createObjectBuilder()
                    .add("name", "Reservation Token")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "Token as returned from the OMiLab reservation service")
                        .add("de", "Token as returned from the OMiLab reservation service"))
                    .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "The output will be the same returned by the microservice configured in the start configuration";
    }

    private String labReservationUrl, labReservationDeviceId, microserviceId, microserviceOperationId = "";
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        labReservationUrl = startConfiguration.getJsonObject("labReservationUrl")==null?"":startConfiguration.getJsonObject("labReservationUrl").getString("value", "");
        if(labReservationUrl.isEmpty()) throw new Exception("labReservationUrl must be provided");
        labReservationDeviceId = startConfiguration.getJsonObject("labReservationDeviceId")==null?"":startConfiguration.getJsonObject("labReservationDeviceId").getString("value", "");
        if(labReservationDeviceId.isEmpty()) throw new Exception("labReservationDeviceId must be provided");
        microserviceId = startConfiguration.getJsonObject("microserviceId")==null?"":startConfiguration.getJsonObject("microserviceId").getString("value", "");
        if(microserviceId.isEmpty()) throw new Exception("microserviceId must be provided");
        microserviceOperationId = startConfiguration.getJsonObject("microserviceOperationId")==null?"":startConfiguration.getJsonObject("microserviceOperationId").getString("value", "");
        if(microserviceOperationId.isEmpty()) throw new Exception("microserviceOperationId must be provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String microserviceOperationInputS = configuration.getJsonObject("microserviceOperationInput")==null?"{}":configuration.getJsonObject("microserviceOperationInput").getString("value", "{}");
        JsonObject microserviceOperationInput = null;
        try{
            microserviceOperationInput = Json.createReader(new StringReader(microserviceOperationInputS)).readObject();
        }catch(Exception ex) {
            throw new Exception("The provided input is an incorrect JSON:\n" + microserviceOperationInputS, ex);
        }
        String reservationToken = configuration.getJsonObject("reservationToken")==null?"":configuration.getJsonObject("reservationToken").getString("value", "");
        if(reservationToken.isEmpty()) throw new Exception("reservationToken must be provided");
        
        ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
        htmlHeaderList.add(new String[]{"Content-Type", "application/json"});
        
        HttpResults outCheck = Utils.sendHTTP(labReservationUrl + "/reservation/check", "POST", "{\"deviceId\": \"" + labReservationDeviceId + "\", \"token\":\"" + reservationToken + "\"}", htmlHeaderList, true, true);
        JsonObject outCheckJson = null;
        try{
            outCheckJson = Json.createReader(new StringReader(new String(outCheck.data, "UTF-8"))).readObject();
        }catch(Exception ex) {
            throw new Exception("Incorrect JSON returned from reservation check service:\n" + new String(outCheck.data, "UTF-8"), ex);
        }
        if(outCheckJson.getInt("status") != 0) 
            throw new Exception("Error contacting the reservation check service: " + outCheckJson.getString("error", ""));
        if(!outCheckJson.getJsonObject("data").getBoolean("allowed", false))
            throw new Exception("The provided token (\"" + reservationToken + "\") is not allowed to use the device \"" + labReservationDeviceId + "\" now (\"" + outCheckJson.getJsonObject("data").getString("currentTimeFormatted") + "\") but only from \"" + outCheckJson.getJsonObject("data").getString("reservationDateStartFormatted") + "\" to \"" + outCheckJson.getJsonObject("data").getString("reservationDateEndFormatted") + "\"");
                
        JsonObject microserviceResponse = MicroserviceController.unique().callMicroserviceForced(microserviceId, microserviceOperationId, microserviceOperationInput);
        return microserviceResponse;
    }
    
    @Override
    public void stop() throws Exception {
        labReservationUrl = "";
        labReservationDeviceId = "";
        microserviceId = "";
        microserviceOperationId = "";
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        OMiLabReservationServiceConnector connector = new OMiLabReservationServiceConnector();
        try{
            MicroserviceController.unique().updateMicroservice("TEST", Json.createObjectBuilder()
                    .add("name", "test")
                    .add("description", "test")
                    .add("public", false)
                    .add("defaultOperationId", "sync")
                    .add("operations", Json.createObjectBuilder()
                        .add("sync", Json.createObjectBuilder()
                            .add("name", "sync")
                            .add("description", "sync")
                            .add("autostart", false)
                            .add("configuration", Json.createObjectBuilder()
                                .add("connectorId", org.adoxx.microservice.api.connectors.impl.DummySyncConnector.class.getName())
                                .add("configStart", Json.createObjectBuilder()
                                    .add("BaseText", Json.createObjectBuilder().add("value", "Hi "))
                                )
                                .add("configCall", Json.createObjectBuilder()
                                    .add("AppendText", Json.createObjectBuilder().add("value", "%name%!"))
                                )
                                .add("inputs", Json.createObjectBuilder()
                                    .add("Your Name", Json.createObjectBuilder()
                                        .add("matchingName", "%name%")
                                        .add("description", "Your Name")
                                        .add("workingExample", "Damiano")
                                    )
                                )
                                .add("outputDescription", "todo")
                                .add("outputAdaptationAlgorithm", "output.modTest='test';\n console.log('LOG SYNC: ' + out(output)); out(output);")
                                .add("statusCheckAlgorithm", "output!=null;")
                            )
                        )
                    ).add("moreInfos", Json.createObjectBuilder()
                        .add("visible", false)
                    )
                    .build()
                );
            
            connector.threadStart(Json.createObjectBuilder()
                .add("labReservationUrl", Json.createObjectBuilder().add("value", "https://olive.innovation-laboratory.org/lrs/rest"))
                .add("labReservationDeviceId", Json.createObjectBuilder().add("value", "dobot"))
                .add("microserviceId", Json.createObjectBuilder().add("value", "TEST"))
                .add("microserviceOperationId", Json.createObjectBuilder().add("value", "sync"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("microserviceOperationInput", Json.createObjectBuilder().add("value", Json.createObjectBuilder().add("Your Name", Json.createObjectBuilder().add("value", "DAM")).build().toString()))
                .add("reservationToken", Json.createObjectBuilder().add("value", "1c74ba41-2d23-46ad-9d9e-a4eaba59d0e1"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
