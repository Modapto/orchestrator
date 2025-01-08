package org.adoxx.microservice.api.rest;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.adoxx.microservice.api.MicroserviceController;
import org.adoxx.microservice.api.log.LogManager;
import org.adoxx.microservice.api.log.LogI.LogLevel;
import org.adoxx.microservice.utils.Utils;

@Path("msc")
public class RESTService {
    /*
    TODO: Implement security here
     */
    
    @Context
    ServletContext context;
    
    @GET
    @Path("/getAvailableConnectors")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAvailableConnectors() {
        try{
            return "{\"status\":0, \"data\":" + MicroserviceController.unique().getAvailableConnectors().toString() + "}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service getAvailableConnectors", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/getStartedMicroservices")
    @Produces(MediaType.APPLICATION_JSON)
    public String getStartedMicroservices(){
        try{
            return "{\"status\":0, \"data\":" + MicroserviceController.unique().getStartedMicroservices().toString() + "}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service getStartedMicroservices", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @POST
    @Path("/createMicroservice")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createMicroservice(String microserviceConfiguration){
        try{
            JsonObject microserviceConfigurationJson = Json.createReader(new StringReader(microserviceConfiguration)).readObject();
            String microserviceId = MicroserviceController.unique().createMicroservice(microserviceConfigurationJson);
            return "{\"status\":0, \"data\": {\"microserviceId\" : \""+microserviceId+"\"}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service createMicroservice", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/createEmptyMicroserviceConfiguration")
    @Produces(MediaType.APPLICATION_JSON)
    public String createEmptyMicroserviceConfiguration(){
        try{
            return "{\"status\":0, \"data\": "+MicroserviceController.unique().createEmptyMicroserviceConfiguration()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service createEmptyMicroserviceConfiguration", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/createDemoMicroserviceConfiguration")
    @Produces(MediaType.APPLICATION_JSON)
    public String createDemoMicroserviceConfiguration(){
        try{
            return "{\"status\":0, \"data\": "+MicroserviceController.unique().createDemoMicroserviceConfiguration()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service createDemoMicroserviceConfiguration", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @POST
    @Path("/updateMicroservice")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateMicroservice(@QueryParam("microserviceId") String microserviceId, String microserviceConfiguration){
        try{
            JsonObject microserviceConfigurationJson = Json.createReader(new StringReader(microserviceConfiguration)).readObject();
            MicroserviceController.unique().updateMicroservice(microserviceId, microserviceConfigurationJson);
            return "{\"status\":0, \"data\": {}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service updateMicroservice", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/deleteMicroservice")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteMicroservice(@QueryParam("microserviceId") String microserviceId){
        try{
            MicroserviceController.unique().deleteMicroservice(microserviceId);
            return "{\"status\":0, \"data\": {}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service deleteMicroservice", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/retrieveAllMicroservices")
    @Produces(MediaType.APPLICATION_JSON)
    public String retrieveAllMicroservices(){
        try{
            JsonObject microservices = MicroserviceController.unique().retrieveAllMicroservices(false);
            return "{\"status\":0, \"data\":"+microservices.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service retrieveAllMicroservices", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/retrieveMicroserviceDetails")
    @Produces(MediaType.APPLICATION_JSON)
    public String retrieveMicroserviceDetails(@QueryParam("microserviceId") String microserviceId){
        try{
            JsonObject microserviceOperations = MicroserviceController.unique().retrieveMicroserviceDetails(microserviceId);
            return "{\"status\":0, \"data\":"+microserviceOperations.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service retrieveMicroserviceDetails", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/retrieveMicroserviceConfiguration")
    @Produces(MediaType.APPLICATION_JSON)
    public String retrieveMicroserviceConfiguration(@QueryParam("microserviceId") String microserviceId){
        try{
            JsonObject microserviceConfiguration = MicroserviceController.unique().retrieveMicroserviceConfiguration(microserviceId);
            return "{\"status\":0, \"data\":"+microserviceConfiguration.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service retrieveMicroserviceConfiguration", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/getMicroserviceIOInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMicroserviceIOInfo(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId){
        try{
            JsonObject microserviceIOInfo = MicroserviceController.unique().getMicroserviceIOInfo(microserviceId, operationId);
            return "{\"status\":0, \"data\":"+microserviceIOInfo.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service getMicroserviceIOInfo", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/startMicroservice")
    @Produces(MediaType.APPLICATION_JSON)
    public String startMicroservice(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId){
        try{
            MicroserviceController.unique().startMicroservice(microserviceId, operationId);
            return "{\"status\":0, \"data\":{}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service startMicroservice", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/startAllMicroserviceOperations")
    @Produces(MediaType.APPLICATION_JSON)
    public String startAllMicroserviceOperations(@QueryParam("microserviceId") String microserviceId){
        try{
            MicroserviceController.unique().startAllMicroserviceOperations(microserviceId);
            return "{\"status\":0, \"data\":{}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service startAllMicroserviceOperations", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @POST
    @Path("/callMicroservice")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String callMicroservice(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId, String microserviceInputs){
        try{
            if(microserviceInputs == null || microserviceInputs.isEmpty())
                microserviceInputs = "{}";
            JsonObject microserviceInputsO = Json.createReader(new StringReader(microserviceInputs)).readObject();
            JsonObject callRet = MicroserviceController.unique().callMicroservice(microserviceId, operationId, microserviceInputsO);
            return "{\"status\":0, \"data\":"+callRet.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service callMicroservice", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }
    
    @GET
    @Path("/callMicroservice")
    @Produces(MediaType.APPLICATION_JSON)
    public String callMicroserviceGET(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId, @QueryParam("microserviceInputs") String microserviceInputs){
        try{
            if(microserviceInputs == null || microserviceInputs.isEmpty())
                microserviceInputs = "{}";
            JsonObject microserviceInputsO = Json.createReader(new StringReader(microserviceInputs)).readObject();
            JsonObject callRet = MicroserviceController.unique().callMicroservice(microserviceId, operationId, microserviceInputsO);
            return "{\"status\":0, \"data\":"+callRet.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service callMicroservice", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }
    
    @GET
    @Path("/stopMicroservice")
    @Produces(MediaType.APPLICATION_JSON)
    public String stopMicroservice(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId){
        try{
            MicroserviceController.unique().stopMicroservice(microserviceId, operationId);
            return "{\"status\":0, \"data\":{}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service stopMicroservice", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/stopAllMicroserviceOperations")
    @Produces(MediaType.APPLICATION_JSON)
    public String stopAllMicroserviceOperations(@QueryParam("microserviceId") String microserviceId){
        try{
            MicroserviceController.unique().stopAllMicroserviceOperations(microserviceId);
            return "{\"status\":0, \"data\":{}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service stopAllMicroserviceOperations", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @POST
    @Path("/callMicroserviceForced")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String callMicroserviceForced(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId, String microserviceInputs){
        try{
            if(microserviceInputs == null || microserviceInputs.isEmpty())
                microserviceInputs = "{}";
            JsonObject microserviceInputsO = Json.createReader(new StringReader(microserviceInputs)).readObject();
            JsonObject callRet = MicroserviceController.unique().callMicroserviceForced(microserviceId, operationId, microserviceInputsO);
            return "{\"status\":0, \"data\":"+callRet.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service callMicroserviceForced", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }
    
    @GET
    @Path("/callMicroserviceForced")
    @Produces(MediaType.APPLICATION_JSON)
    public String callMicroserviceForcedGET(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId, @QueryParam("microserviceInputs") String microserviceInputs){
        try{
            if(microserviceInputs == null || microserviceInputs.isEmpty())
                microserviceInputs = "{}";
            JsonObject microserviceInputsO = Json.createReader(new StringReader(microserviceInputs)).readObject();
            JsonObject callRet = MicroserviceController.unique().callMicroserviceForced(microserviceId, operationId, microserviceInputsO);
            return "{\"status\":0, \"data\":"+callRet.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service callMicroserviceForced", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }
/*
    @POST
    @Path("/callMicroserviceCustomIO/{microserviceId}/{operationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response callMicroserviceCustomIO(@PathParam("microserviceId") String microserviceId, @PathParam("operationId") String operationId, String microserviceInputs){
        //the custom JSON provided as input is forwarded to the first (and only) microservice input defined
        //the output is the JSON provided by the microservice
        try{
            if(microserviceInputs == null || microserviceInputs.isEmpty())
                microserviceInputs = "{}";
            
            JsonObject microserviceIOInfo = MicroserviceController.unique().getMicroserviceIOInfo(microserviceId, operationId);
            Set<String> inputKeys = microserviceIOInfo.getJsonObject("requiredInputTemplate").keySet();
            if (inputKeys.size() > 1)
                throw new Exception("The callMicroserviceCustom can not be used on microservices with more then one input defined");
            
            JsonObject microserviceInputsO = null;
            if (inputKeys.size() == 1) {
                String inputKey = inputKeys.iterator().next();
                microserviceInputsO = Json.createObjectBuilder().add(inputKey, Json.createObjectBuilder().add("value", microserviceInputs)).build();
            } else {
                microserviceInputsO = Json.createObjectBuilder().build();
            }
            JsonObject callRet = MicroserviceController.unique().callMicroserviceForced(microserviceId, operationId, microserviceInputsO);
            return Response.status(Response.Status.OK).entity(callRet.toString()).build();
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service callMicroserviceCustom", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}").build();
        }
    }
*/
    @POST
    @Path("/callMicroserviceCustomIO/{microserviceId}/{operationId}/{postInputId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response callMicroserviceCustomIO(@PathParam("microserviceId") String microserviceId, @PathParam("operationId") String operationId, @PathParam("postInputId") String postInputId, @Context UriInfo uriInfo, String microserviceBodyInput){
        //the query parameters are forwarded to microservice input defined
        //with the exception of the value postInputId input that instead is provided in the post body
        //the output is the JSON provided by the microservice
        try{
            if(microserviceBodyInput == null)
                microserviceBodyInput = "";

            MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(); 
            JsonObject microserviceIOInfo = MicroserviceController.unique().getMicroserviceIOInfo(microserviceId, operationId);
            Iterator<String> inputKeysIterator = microserviceIOInfo.getJsonObject("requiredInputTemplate").keySet().iterator();
            JsonObjectBuilder microserviceInputsO = Json.createObjectBuilder();
            while(inputKeysIterator.hasNext()) {
                String inputKey = inputKeysIterator.next();
                String queryInput = "";
                if(postInputId.equals(inputKey)) {
                    queryInput = microserviceBodyInput;
                } else {
                    queryInput = queryParams.getFirst(inputKey) == null ? "" : queryParams.getFirst(inputKey);
                }
                microserviceInputsO.add(inputKey, Json.createObjectBuilder().add("value", queryInput));
            }
            JsonObject callRet = MicroserviceController.unique().callMicroserviceForced(microserviceId, operationId, microserviceInputsO.build());
            return Response.status(Response.Status.OK).entity(callRet.toString()).build();
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service callMicroserviceCustom", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}").build();
        }
    }

    @GET
    @Path("/callMicroserviceCustomIO/{microserviceId}/{operationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response callMicroserviceCustomIOGET(@PathParam("microserviceId") String microserviceId, @PathParam("operationId") String operationId, @Context UriInfo uriInfo){
        //the query parameters are forwarded to microservice input defined
        //the output is the JSON provided by the microservice
        try{
            MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(); 
            JsonObject microserviceIOInfo = MicroserviceController.unique().getMicroserviceIOInfo(microserviceId, operationId);
            Iterator<String> inputKeysIterator = microserviceIOInfo.getJsonObject("requiredInputTemplate").keySet().iterator();
            JsonObjectBuilder microserviceInputsO = Json.createObjectBuilder();
            while(inputKeysIterator.hasNext()) {
                String inputKey = inputKeysIterator.next();
                String queryInput = queryParams.getFirst(inputKey) == null ? "" : queryParams.getFirst(inputKey);
                microserviceInputsO.add(inputKey, Json.createObjectBuilder().add("value", queryInput));
            }
            JsonObject callRet = MicroserviceController.unique().callMicroserviceForced(microserviceId, operationId, microserviceInputsO.build());
            return Response.status(Response.Status.OK).entity(callRet.toString()).build();
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service callMicroserviceCustom", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}").build();
            //return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }
    
    @GET
    @Path("/checkMicroserviceStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public String checkMicroserviceStatus(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId){
        try{
            JsonObject checkRet = MicroserviceController.unique().checkMicroserviceStatus(microserviceId, operationId);
            return "{\"status\":0, \"data\":"+checkRet.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service checkMicroserviceStatus", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/checkMicroserviceConnectorStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public String checkMicroserviceConnectorStatus(@QueryParam("microserviceId") String microserviceId, @QueryParam("operationId") String operationId){
        try{
            JsonObject checkRet = MicroserviceController.unique().checkMicroserviceConnectorStatus(microserviceId, operationId);
            return "{\"status\":0, \"data\":"+checkRet.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service checkMicroserviceConnectorStatus", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @GET
    @Path("/fixAllStartedMicroservices")
    @Produces(MediaType.APPLICATION_JSON)
    public String fixAllStartedMicroservices(){
        try{
            MicroserviceController.unique().fixAllStartedMicroservices();
            return "{\"status\":0, \"data\":{}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service fixAllStartedMicroservices", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }        
    }
    
    @POST
    @Path("/callSyncConnectorForced")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String callSyncConnectorForced(String connectorFullConfiguration){
        try{
            if(connectorFullConfiguration == null || connectorFullConfiguration.isEmpty())
                connectorFullConfiguration = "{}";
            JsonObject connectorFullConfigurationO = Json.createReader(new StringReader(connectorFullConfiguration)).readObject();
            JsonObject callRet = MicroserviceController.unique().callSyncConnectorForced(connectorFullConfigurationO);
            return "{\"status\":0, \"data\":"+callRet.toString()+"}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service callSyncConnectorForced", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }
    
    @POST
    @Path("/uploadLocalFile")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String uploadLocalFile(@QueryParam("fileName") String fileName, byte[] fileContent){
        try{
            String fileId = Utils.uploadLocalFile(fileName, fileContent);
            return "{\"status\":0, \"data\": {\"fileId\" : \""+Utils.escapeJson(fileId)+"\"}}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service uploadLocalFile", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }

    @POST
    @Path("/updateLocalFile")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateLocalFile(@QueryParam("fileId") String fileId, byte[] fileContent){
        try{
            Utils.updateLocalFile(fileId, fileContent);
            return "{\"status\":0}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service updateLocalFile", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }

    @GET
    @Path("/downloadLocalFile")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String downloadLocalFile(@QueryParam("fileId") String fileId){
        try{
            return new String(Utils.downloadLocalFile(fileId), "UTF-8");
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service updateLocalFile", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }

    @GET
    @Path("/deleteLocalFile")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteLocalFile(@QueryParam("fileId") String fileId){
        try{
            Utils.deleteLocalFile(fileId);
            return "{\"status\":0}";
        }catch(Exception ex){
            LogManager.unique().log(LogLevel.ERROR, "Exception calling the REST service updateLocalFile", ex);
            return "{\"status\":-1, \"error\":\""+Utils.escapeJson(ex.getMessage())+"\"}";
        }
    }
}
