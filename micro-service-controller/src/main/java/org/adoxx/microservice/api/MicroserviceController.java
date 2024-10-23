package org.adoxx.microservice.api;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.adoxx.microservice.api.connectors.AsyncResponseHandlerI;
import org.adoxx.microservice.api.connectors.BasicConnectorA;
import org.adoxx.microservice.api.connectors.ConnectorsController;
import org.adoxx.microservice.api.log.LogI;
import org.adoxx.microservice.api.log.LogI.LogLevel;
import org.adoxx.microservice.api.log.LogManager;
import org.adoxx.microservice.api.persistence.PersistenceI;
import org.adoxx.microservice.api.persistence.PersistenceManager;
import org.adoxx.microservice.utils.Utils;
/*
 * TODO:
 * si potrebbe aggiungere un algoritmo di processing per ogni input utente cosi da permettere di cambiarlo in quello che si vuole prima di fornirlo alla call config
 * 
 *dopo:
 * - come gestisco il monitoring dei servizi?
 * -- deve esserci un processo in background che controlla e logga lo stato in caso di errore
 * --- deve fornire un interfaccia che permette di personalizzare come i risultati del monitoring vengono salvati (poi di default userò il sistema di log implement) e come vengono ritrovati
 * ---- il monitoring ritorna un json da salvare in un array dove per ogni op attiva si ha lo stato?
 * ---- come è meglio per la dashboard visualizzare lo stato dei servizi?
 * ----- deve fare affidamento sul processo in background?
 * ----- deve monitorare da sola a volo lo stato dei servizi?
 * ----- mostro lo stato passato dei servizi?
 * ------ sarebbe meglio di si, cosi dalla dashboard si puo vedere quando ha iniziato a bloccarsi
 * ------ posso fare che io gestisco un servizio di monitoring e che questo mi ritorna la history di un dato servizio
 * ------ la history di un servizio la salvo in un file in una cartella history specifica
 * ------- uso un solo file di monitoring per ogni servizio? NO, quando il file creasce rallenterebbe tutto. MEglio creare una cartella per il servizio e un file per ogni stato errato (salvare solo gli stati critici)
 *
 * - exe connector? si solo se si permette di usare solo exe in una specifica cartella senza possibilità di upload, ma solo accesso diretto al server 
 * - CSV Connector
 * - SOAP connector
 * - gooogle spreadsheet connector (https://developers.google.com/identity/protocols/OAuth2WebServer)
 * - websocket client publisher/subscribe connector
 * - MQTT publisher/subrscibe usando https://www.eclipse.org/paho/clients/java/
 * - POP3 connector (JavaMail api)
 * - Rimuovere JsonFileConnector e sostituirlo con FileProviderConnector
 * - Cassandra connector? 
 * - KairosDB connector (https://github.com/kairosdb/kairosdb    basato su cassandra)
 * - InfluxDB connector? (https://github.com/influxdata/influxdb) (https://db-engines.com/en/ranking/time+series+dbms)
 * - Graphite connector? (https://github.com/graphite-project/graphite-web)
 * - ElasticSearch connector?
 * 
 */


/**
 * <h1>MicroserviceController</h1>
 * This Class contains methods to manage microservices.<br>
 * A microservice must be first created using {@link #createMicroservice(JsonObject)}.<br>
 * A microservice is composed of operations that represent the different features the microservice can provide.<br>
 * Once the microservice is created, everyone of its operations can be started indipendently using {@link #startMicroservice(String)}.<br>
 * A started microservice operation can optionally be called syncronously using {@link #callMicroservice(String, JsonObject)}. The execution of this method maybe not required for asynchronous microservice operations (operations that use an asynchronous connector {@link #getAvailableConnectors()}).<br>
 * When the microservice operation is not required anymore can be stopped using {@link #stopMicroservice(String)}.<br>
 * Checking methods {@link #checkMicroserviceStatus(String)} can be used to evaluate the microservice operation status.<br>
 * This class in particular use the facilities of the {@link ConnectorsController} Class.<br>
 * 
 * @author Damiano Falcioni
 */
public class MicroserviceController {

    private static MicroserviceController microserviceController = null;
    
    /**
     * Factory method that return everytime the same MicroserviceController instance.<br>
     * @return MicroserviceController The unique MicroserviceController instance
     * @throws Exception in case of error
     */
    public static MicroserviceController unique() throws Exception {
        if(microserviceController == null)
            microserviceController = new MicroserviceController();
        return microserviceController;
    }
    
    /**
     * Factory method that update the unique MicroserviceController instance with a new one and return it.<br>
     * @return MicroserviceController The new MicroserviceController instance
     * @throws Exception in case of error
     */
    public static MicroserviceController newUnique() throws Exception {
        microserviceController = new MicroserviceController();
        return microserviceController;
    }
    
    private HashMap<String, HashMap<String, String>> service_operationStartedList = new HashMap<String, HashMap<String, String>>();
    
    //private int autoRestartIntervalTimeMinutes=5;
    
    public MicroserviceController() {
        
    }
    /*
    private void initServiceAutoRestart() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(autoRestartIntervalTimeMinutes > 0) {
                    try {
                        Thread.sleep(autoRestartIntervalTimeMinutes*60*1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ArrayList<String[]> toRestartArray = new ArrayList<>();
                    for(HashMap<String, String> service_operationProperties : service_operationStartedList.values())
                        toRestartArray.add(new String[] {service_operationProperties.get("microserviceId"), service_operationProperties.get("operationId")});
                    for(String[] serviceToRestart : toRestartArray) {
                        try {
                            stopMicroservice(serviceToRestart[0], serviceToRestart[1]);
                            startMicroservice(serviceToRestart[0], serviceToRestart[1]);
                        } catch (Exception ex) {
                            LogManager.unique().log(LogLevel.ERROR, "Exception in the AutoRestart for service " + serviceToRestart[0] + " " + serviceToRestart[1], ex);
                        }
                    }
                    
                }
                
            }
        }).start();
    }
    */
    
    /**
     * Set a new persistence handler that is used to store and retrieve the microservices configuration files<br>
     * @param handler A class implementing the {@link PersistenceI} interface
     * @see PersistenceI
     */
    public void setPersistenceHandler(PersistenceI handler) {
        PersistenceManager.unique().setProvider(handler);
    }
    
    /**
     * Set a new Log handler<br>
     * @param handler A class implementing the {@link LogI} interface
     * @see LogI
     */
    public void setLogHandler(LogI handler) {
        LogManager.unique().setLogHandler(handler);
    }
    
    /**
     * Start all the microservice operations that have the autostart configuration with value true
     */
    public void initAutostart() throws Exception {
        for(String microserviceId : PersistenceManager.unique().retrieveAllMicroservicesId()) {
            try {
                JsonObject microserviceConfiguration = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
                checkAutostart(microserviceId, microserviceConfiguration);
            } catch (Exception e) {
                LogManager.unique().log(LogLevel.ERROR, "Error on the Autostart of microservice '" + microserviceId + "", e);
            }
        }
    }
    
    /**
     * Return a JSON that contain the array list of all the started microservices operations with the used configuration<br>
     * @return JsonObject A JSON object of this format: 
     * <pre>
     * {
     *     "startedList" : [{
     *          "microserviceId" : "id of the started microservice",
     *          "microserviceName" : "name of the started microservice",
     *          "microserviceDescription" : "description of the started microservice",
     *          "operationId" : "id of the started microservice operation",
     *          "operationName" : "name of the started microservice operation",
     *          "operationDescription" : "description of the started microservice operation"
     *     },
     *     ...
     *     ]
     * }
     * </pre>
     */
    public JsonObject getStartedMicroservices() {
        JsonArrayBuilder listBuilder = Json.createArrayBuilder();
        for(HashMap<String, String> service_operationProperties : service_operationStartedList.values()) {
            listBuilder.add(Json.createObjectBuilder()
                .add("microserviceId", service_operationProperties.get("microserviceId"))
                .add("microserviceName", service_operationProperties.get("microserviceName"))
                .add("microserviceDescription", service_operationProperties.get("microserviceDescription"))
                .add("operationId", service_operationProperties.get("operationId"))
                .add("operationName", service_operationProperties.get("operationName"))
                .add("operationDescription", service_operationProperties.get("operationDescription"))
            );
        }
        return Json.createObjectBuilder().add("startedList", listBuilder).build();
    }
    
    /**
     * Start all the microservice operations provided<br>
     * @param initConfiguration The JSON object specifying the microservice operations to start. The format accepted is the same as returned by {@link #getStartedMicroservices()} (name and description fields can be skipped. Only Id fields are required)
     * <pre>
     * {
     *     "startedList" : [{
     *          "microserviceId" : "id of the microservice to start",
     *          "operationId" : "operation id of the microservice to start"
     *     },
     *     ...
     *     ]
     * }
     * </pre>
     * @throws Exception in case of error
     */
    public void initStartedMicroservices(JsonObject initConfiguration) throws Exception {
        if(initConfiguration == null) throw new Exception("initConfiguration can not be null");
        JsonArray startedList = initConfiguration.getJsonArray("startedList");
        if(startedList == null) throw new Exception("The json array startedList must be present");
        for(JsonValue startedV : startedList) {
            JsonObject started = (JsonObject) startedV;
            if(!started.containsKey("microserviceId")) throw new Exception("The json object inside the array startedServicesList must be have a json string microserviceId");
            if(!started.containsKey("operationId")) throw new Exception("The json object inside the array startedServicesList must be have a json string operationId");
            String serviceId = started.getString("microserviceId");
            String operationId = started.getString("operationId");
            startMicroservice(serviceId, operationId);
        }
    }
    
    /**
     * Return a JSON object that contain all the available microservices connectors<br>
     * @return JsonObject A JSON object of the same format returned by {@link ConnectorsController#getConnectors()}
     * @throws Exception in case of error
     * @see org.adoxx.microservice.api.connectors.SyncConnectorA 
     * @see org.adoxx.microservice.api.connectors.AsyncConnectorA
     */
    public JsonObject getAvailableConnectors() throws Exception {
        return ConnectorsController.unique().getConnectors();
    }
    
    /**
     * Create an empty microservice configuration using the Dummy Synchronous connector<br>
     * @return JsonObject A JSON object in the format required by {@link #createMicroservice(JsonObject)}
     * @throws Exception in case of error
     * @see #createMicroservice(JsonObject)
     */
    public JsonObject createEmptyMicroserviceConfiguration() throws Exception {
        return Json.createObjectBuilder()
            .add("name", "New Microservice")
            .add("description", "Empty New Microservice")
            .add("public", true)
            .add("defaultOperationId", "default")
            .add("operations", Json.createObjectBuilder()
                .add("default", Json.createObjectBuilder()
                    .add("name", "default")
                    .add("description", "default operation")
                    .add("autostart", false)
                    .add("configuration", Json.createObjectBuilder()
                        .add("connectorId", org.adoxx.microservice.api.connectors.impl.DummySyncConnector.class.getName())
                        .add("configStart", Json.createObjectBuilder()
                            .add("BaseText", Json.createObjectBuilder()
                                .add("value", "Hello ")))
                        .add("configCall", Json.createObjectBuilder()
                            .add("AppendText", Json.createObjectBuilder()
                                .add("value", "$textToAppend")))
                        .add("inputs", Json.createObjectBuilder()
                            .add("Append Text", Json.createObjectBuilder()
                                .add("matchingName", "$textToAppend")
                                .add("description", "Text to append")
                                .add("workingExample", "World")))
                        .add("outputDescription", "A string resulting from the concatenation of the BaseText with the AppendText")
                        .add("outputAdaptationAlgorithm", "/*\n- The Json object returned by the connector is available in the variable 'output'.\n- The last instruction of this algorithm must be a string rapresentation of a Json object. The function out(JsonObject) is available and return the string rapresentation of the Json object provided as input.\n- The function callMicroservice(microserviceId, operationId, inputJson) is available and allow to call another microservice identifyed by the string paramters microserviceId and operationId, using as input the Json object provided as the last parameter. The function return the Json Object returned by the called microservice.\n*/\n\noutput.newField='newField';\nout(output);")
                        .add("statusCheckAlgorithm", "/*\n- The Json object returned by this microservice is available in the variable 'output'.\n- The last instruction of this algorithm must be a boolean value indicating if the microservice is in a good status (true) or not (false).\n*/\n\noutput!=null;")
                    )
                )
            ).add("moreInfos", Json.createObjectBuilder()
                .add("visible", true)
                .add("presentationImageUrl", "https://www.adoxx.org/live/image/layout_set_logo?img_id=179909&t=1521267871183")
                .add("ownerHtml", "<a href=\"http://www.adoxx.org\">ADOxx Team</a>")
                .add("descriptionHtml", "<p>Hello World Microservice</p>"))
            .build();
    }
    
    /**
     * Create a Demo microservice configuration using both the Dummy Synchronous and Asynchronous connectors<br>
     * @return JsonObject A JSON object in the format required by {@link #createMicroservice(JsonObject)}
     * @throws Exception in case of error
     * @see #createMicroservice(JsonObject)
     */
    public JsonObject createDemoMicroserviceConfiguration() throws Exception {
        return Json.createObjectBuilder()
            .add("name", "Demo Microservice")
            .add("description", "Demo Microservice with two dummy operations. One synchronous and another asynchronous.")
            .add("public", true)
            .add("defaultOperationId", "syncDemo")
            .add("operations", Json.createObjectBuilder()
                .add("syncDemo", Json.createObjectBuilder()
                    .add("name", "syncDemo")
                    .add("description", "synchronous 'Hello World' operation")
                    .add("autostart", false)
                    .add("configuration", Json.createObjectBuilder()
                        .add("connectorId", org.adoxx.microservice.api.connectors.impl.DummySyncConnector.class.getName())
                        .add("configStart", Json.createObjectBuilder()
                            .add("BaseText", Json.createObjectBuilder()
                                .add("value", "Hello ")))
                        .add("configCall", Json.createObjectBuilder()
                            .add("AppendText", Json.createObjectBuilder()
                                .add("value", "$textToAppend")))
                        .add("inputs", Json.createObjectBuilder()
                            .add("Append Text", Json.createObjectBuilder()
                                .add("matchingName", "$textToAppend")
                                .add("description", "Text to append")
                                .add("workingExample", "World")))
                        .add("outputDescription", "A string resulting from the concatenation of the BaseText with the AppendText")
                        .add("outputAdaptationAlgorithm", "/*\n- The Json object returned by the connector is available in the variable 'output'.\n- The last instruction of this algorithm must be a string rapresentation of a Json object. The function out(JsonObject) is available and return the string rapresentation of the Json object provided as input.\n- The function callMicroservice(microserviceId, operationId, inputJson) is available and allow to call another microservice identifyed by the string paramters microserviceId and operationId, using as input the Json object provided as the last parameter. The function return the Json Object returned by the called microservice.\n*/\n\noutput.newField='newField';\nout(output);")
                        .add("statusCheckAlgorithm", "/*\n- The Json object returned by this microservice is available in the variable 'output'.\n- The last instruction of this algorithm must be a boolean value indicating if the microservice is in a good status (true) or not (false).\n*/\n\noutput!=null;")))
                .add("asyncDemo", Json.createObjectBuilder()
                    .add("name", "asyncDemo")
                    .add("description", "asynchronous 'Hello World' operation")
                    .add("autostart", false)
                    .add("configuration", Json.createObjectBuilder()
                        .add("connectorId", org.adoxx.microservice.api.connectors.impl.DummyAsyncConnector.class.getName())
                        .add("configStart", Json.createObjectBuilder()
                            .add("Text", Json.createObjectBuilder()
                                .add("value", "Async Text Demo")))
                        .add("configCall", Json.createObjectBuilder())
                        .add("inputs", Json.createObjectBuilder())
                        .add("inputsAsync", Json.createObjectBuilder()
                            .add("responseServiceId", "")
                            .add("responseServiceOperationId", "syncDemo")
                            .add("responseServiceInputId", "Append Text")
                            .add("responseServiceOtherInputs", Json.createObjectBuilder())
                            .add("inputAdaptationAlgorithm", "/*\n- The Json object returned by the microservice is available in the variable 'output'.\n- The last instruction of this algorithm must be a string.\n- The function callMicroservice(microserviceId, operationId, inputJson) is available and allow to call another microservice identifyed by the string paramters microserviceId and operationId, using as input the Json object provided as the last parameter. The function return the Json Object returned by the called microservice.\n*/\n\noutput.dataText;"))
                        .add("outputDescription", "A string resulting from the concatenation of the BaseText with the AppendText")
                        .add("outputAdaptationAlgorithm", "/*\n- The Json object returned by the connector is available in the variable 'output'.\n- The last instruction of this algorithm must be a string rapresentation of a Json object. The function out(JsonObject) is available and return the string rapresentation of the Json object provided as input.\n- The function callMicroservice(microserviceId, operationId, inputJson) is available and allow to call another microservice identifyed by the string paramters microserviceId and operationId, using as input the Json object provided as the last parameter. The function return the Json Object returned by the called microservice.\n*/\n\noutput.newField='newField';\nout(output);")
                        .add("statusCheckAlgorithm", "/*\n- The Json object returned by this microservice is available in the variable 'output'.\n- The last instruction of this algorithm must be a boolean value indicating if the microservice is in a good status (true) or not (false).\n*/\n\noutput!=null;")
                    )
                )
            ).add("moreInfos", Json.createObjectBuilder()
                .add("visible", true)
                .add("presentationImageUrl", "https://www.adoxx.org/live/image/layout_set_logo?img_id=179909&t=1521267871183")
                .add("ownerHtml", "<a href=\"http://www.adoxx.org\">ADOxx Team</a>")
                .add("descriptionHtml", "<p>Hello World Microservice</p>"))
            .build();
    }
    
    /**
     * Create a microservice from the provided configuration<br>
     * @param microserviceConfiguration The JSON object specifying the microservice configuration. The format accepted is the following : 
     * <pre>
     * {
     *     "name" : "name for this microservice",
     *     "description" : "description for this microservice",
     *     "public" : true/false when the service must be public available or not,
     *     "defaultOperationId" : "id of the microservice operation to use as default when a microservice is started called or stopped", 
     *     "operations" : {
     *         "unique id of the operation" : {
     *             "name" : "name for this operation",
     *             "description" : "description for this operation",
     *             "autostart" : true/false when the operation must be automatically started,
     *             "configuration" : {
     *                 "connectorId" : "id of the connector to use, obtained from {@link #getAvailableConnectors()}",
     *                 "configStart" : {... JSON containing the connector specific starting configuration as specified by the startConfigurationTemplate obtained from {@link #getAvailableConnectors()}, but filled with a value ...},
     *                 "configCall" : {... JSON containing the connector specific starting configuration as specified by the callConfigurationTemplate obtained from {@link #getAvailableConnectors()}, but filled with a value ...},
     *                 "inputs" : {
     *                     "unique id of the input" : {
     *                         "matchingName" : "string that will be searched anywhere in the configCall and replaced with the user provided input specified in {@link #callMicroservice(String, JsonObject)}",
     *                         "description" : "description of the required input",
     *                         "workingExample" : "example of working input to use for service status check. This value will be substituted to the matchingName in the configCall"
     *                     }
     *                 },
     *                 "inputsAsync" : { //This must be present only if the connector is asynchronous (identifiable by the JSON property asyncConnectionRequired obtained from {@link #getAvailableConnectors()})
     *                     "responseServiceId" : "id of the microservice to where redirect the asynchronous response. If empty the current microservice is used",
     *                     "responseServiceOperationId" : "id/name of the microservice operation to where redirect the asynchronous response. If empty the default will be used",
     *                     "responseServiceInputId" : "id of the microservice input to where redirect the asynchronous response. If empty the response will not be redirected",
     *                     "responseServiceOtherInputs" : { //the values of the other inputs required by the microservice
     *                         "other input id" : {
     *                             "value" : "value of this microservice input"
     *                         }
     *                     },
     *                     "inputAdaptationAlgorithm" : "javascript code used to adapt the data to provide to the response service. It can only return null or a string. If it return null the redirection is not performed, while if is a string this will be redirected. Inside this algorithm is it possible to read the variable 'serviceOutput' and call the function 'callMicroservice(microserviceId, operationId, microserviceInputs)'"
     *                 },
     *                 "outputDescription" : "textual description of the microservice call output",
     *                 "outputAdaptationAlgorithm" : "javascript code used to personalize the output of the microservice. The algorithm have access to the microservice call output through the javascript variable 'serviceOutput' and its last instruction must be the desired output.",
     *                 "statusCheckAlgorithm" : "javascript code that is executed during the {@link #checkMicroserviceStatus(String)}. The algorithm have access to the microservice call output through the javascript variable 'serviceOutput' and its last instruction must be a boolean expression that will indicate if the service is working correctly or not"
     *             }
     *         }
     *     },
     *     
     *     "moreInfos" : {
     *          "visible" : true,
     *          "presentationImageUrl" : "",
     *          "ownerHtml" : "",
     *          "descriptionHtml" : ""
     *     }
     * }
     * </pre>
     * @return String The unique Id of the created microservice
     * @throws Exception in case of error
     */
    public String createMicroservice(JsonObject microserviceConfiguration) throws Exception {
        String microserviceId = PersistenceManager.unique().saveMicroserviceConfiguration(microserviceConfiguration);
        checkAutostart(microserviceId, microserviceConfiguration);
        return microserviceId;
    }
    
    /**
     * Update an existing microservice with a new configuration. If the microservice is not present it will be created with the provided id.<br>
     * @param microserviceId The Id of the microservice to update
     * @param microserviceConfiguration The new microservice configuration. The format of the JSON object is the same of the one provided in {@link #createMicroservice(JsonObject)}
     * @throws Exception in case of error
     */
    public void updateMicroservice(String microserviceId, JsonObject microserviceConfiguration) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        stopAllMicroserviceOperations(microserviceId);
        PersistenceManager.unique().updateMicroserviceConfiguration(microserviceId, microserviceConfiguration);
        checkAutostart(microserviceId, microserviceConfiguration);
    }
    
    /**
     * Delete an existing microservice. If the microservice is not present an exception is rised.<br>
     * @param microserviceId The Id of the microservice to delete
     * @throws Exception in case of error
     */
    public void deleteMicroservice(String microserviceId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        stopAllMicroserviceOperations(microserviceId);
        PersistenceManager.unique().deleteMicroserviceConfiguration(microserviceId);
    }
    
    /**
     * Return a list of id of all the created microservices<br>
     * @return List The list of created microservice's id  
     * @throws Exception in case of error
     */
    public List<String> retrieveAllMicroservicesId() throws Exception {
        return PersistenceManager.unique().retrieveAllMicroservicesId();
    }
    
    
    /**
     * Return a JSON containing informations about the available microservices<br>
     * @param allIncludingPrivates If true, return all the microservices, if false return only the publics one
     * @return JsonObject a JSON objectin the following format
     * <pre>
     * {
     *      "unique id of the microservice" : {
     *          "name" : "name for this microservice",
     *          "description" : "description for this microservice"
     *      }
     * }
     * </pre>
     * @throws Exception in case of error
     */
    public JsonObject retrieveAllMicroservices(boolean allIncludingPrivates) throws Exception {
        JsonObjectBuilder retBuilder = Json.createObjectBuilder();
        for(String microserviceId : PersistenceManager.unique().retrieveAllMicroservicesId()) {
            try {
                JsonObject microserviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
            
                if(allIncludingPrivates || microserviceConfig.getBoolean("public"))
                    retBuilder.add(microserviceId, Json.createObjectBuilder()
                        .add("name", microserviceConfig.getString("name"))
                        .add("description", microserviceConfig.getString("description"))
                    );
            } catch (Exception e) {
                LogManager.unique().log(LogLevel.ERROR, "Error retrieving the information for the microservice '" + microserviceId + "", e);
            };
        }
        return retBuilder.build();
    }
    
    /**
     * Return a JSON containing informations about the available operations for the provided microservice<br>
     * @param microserviceId The id of the microservice to look for the operations
     * @return JsonObject a JSON objectin the following format
     * <pre>
     * {    
     *      "id" : "id of this microservice",
     *      "name" : "name for this microservice",
     *      "description" : "description for this microservice",
     *      "operations" : {
     *          "unique id of the operation" : {
     *              "name" : "name for this operation",
     *              "description" : "description for this operation"
     *          }
     *      },
     *      "moreInfos" : {
     *          The moreInfos tag associated to this microservice configuration
     *      }
     * }
     * </pre>
     * @throws Exception in case of error
     */
    public JsonObject retrieveMicroserviceDetails(String microserviceId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        
        JsonObject microserviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        JsonObjectBuilder operationsBuilder = Json.createObjectBuilder();
        JsonObject operations = microserviceConfig.getJsonObject("operations");
        for(String operationId : operations.keySet()) {
            JsonObject operation = operations.getJsonObject(operationId);
            operationsBuilder.add(operationId, Json.createObjectBuilder()
                .add("name", operation.getString("name"))
                .add("description", operation.getString("description"))
            );
        }
        
        return Json.createObjectBuilder()
            .add("id", microserviceId)
            .add("name", microserviceConfig.getString("name"))
            .add("description", microserviceConfig.getString("description"))
            .add("operations", operationsBuilder)
            .add("moreInfos", microserviceConfig.getJsonObject("moreInfos"))
            .build();
    }
    
    /**
     * Return the JSON configuration for the required microservice<br>
     * @param microserviceId The id of the microservice to look for the configuration
     * @return JsonObject The microservice configuration. The format of the JSON object is the same of the one provided in {@link #createMicroservice(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject retrieveMicroserviceConfiguration(String microserviceId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        return PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
    }
    
    /**
     * Return a JSON object that contains inputs and outputs information for the microservice having the provided id and default operation.<br>
     * The requiredInputTemplate in particular must be used to create the input for the {@link #callMicroservice(String, JsonObject)} while the outputStructured to process its output.<br>
     * @param microserviceId The id of the microservice to look for the inputs and outputs informations
     * @return JsonObject A JSON object of this format: 
     * <pre>
     * {
     *      "requiredInputTemplate" : {
     *          "id of one input" : {
     *              "description" : "Here you can find the description of this input field",
     *              "workingExample" : "Here you can find a valid value for this input field",
     *              "value" : "This is empty and must be filled by the user when calling the {@link #callMicroservice(String, JsonObject)}"
     *          },
     *          "id of another input" : {...},
     *          ...
     *      },
     *      "outputDescription" : "Here you can find the description of the output"
     *      "asyncConnectionRequired" : true/false respectively if the used connector extend the asynchronous abstract class {@link org.adoxx.microservice.api.connectors.AsyncConnectorA} or the synchronous abstract class {@link org.adoxx.microservice.api.connectors.SyncConnectorA},
     * }
     * </pre>
     * @throws Exception in case of error
     * @see org.adoxx.microservice.api.connectors.SyncConnectorA 
     * @see org.adoxx.microservice.api.connectors.AsyncConnectorA
     */
    public JsonObject getMicroserviceIOInfo(String microserviceId) throws Exception {
        return getMicroserviceIOInfo(microserviceId, null);
    }
    
    /**
     * Return a JSON object that contains inputs and outputs information for the microservice having the provided id and operation.<br>
     * The requiredInputTemplate in particular must be used to create the input for the {@link #callMicroservice(String, JsonObject)}.<br>
     * @param microserviceId The id of the microservice to look for the inputs and outputs informations
     * @param operationId The id of the microservice operation to look for the inputs and outputs informations
     * @return JsonObject A JSON object of the same format of {@link #getMicroserviceIOInfo(String)} 
     * @throws Exception in case of error
     * @see org.adoxx.microservice.api.connectors.SyncConnectorA 
     * @see org.adoxx.microservice.api.connectors.AsyncConnectorA
     */
    public JsonObject getMicroserviceIOInfo(String microserviceId, String operationId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        operationId = (operationId == null || operationId.isEmpty())?serviceConfig.getString("defaultOperationId"):operationId;
        JsonObject operation = serviceConfig.getJsonObject("operations").getJsonObject(operationId);
        if(operation == null) throw new Exception("Impossible to find the operation with key " + operationId);
        JsonObject configuration = operation.getJsonObject("configuration");
        JsonObject inputs = configuration.getJsonObject("inputs");
        JsonObjectBuilder requiredInputTemplateBuilder = Json.createObjectBuilder();
        for(String serviceInputKey : inputs.keySet()) {
            requiredInputTemplateBuilder.add(serviceInputKey, Json.createObjectBuilder()
                .add("description", inputs.getJsonObject(serviceInputKey).getString("description"))
                .add("workingExample", inputs.getJsonObject(serviceInputKey).getString("workingExample"))
                .add("value", "")
            );
        }
        
        String connectorId = configuration.getString("connectorId");
        JsonObject connectorInfo = getAvailableConnectors().getJsonObject(connectorId);
        
        return Json.createObjectBuilder()
            .add("requiredInputTemplate", requiredInputTemplateBuilder)
            .add("outputDescription", configuration.getString("outputDescription"))
            .add("asyncConnectionRequired", connectorInfo.getBoolean("asyncConnectionRequired"))
        .build();
    }
    
    /**
     * Start the all the operations of the provided microservice.<br>
     * If one microservice operation is already started it will be ignored.<br>
     * @param microserviceId The id of the microservice to look for the configuration
     * @throws Exception in case of error
     */
    public void startAllMicroserviceOperations(String microserviceId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        JsonObject operations = serviceConfig.getJsonObject("operations");
        for(String operationKey : operations.keySet())
            startMicroservice(microserviceId, operationKey);
    }
    
    /**
     * Start the default operation of the provided microservice.<br>
     * If the microservice operation is already started it will be ignored.<br>
     * @param microserviceId The id of the microservice to start
     * @throws Exception in case of error
     */
    public void startMicroservice(String microserviceId) throws Exception {
        startMicroservice(microserviceId, null);
    }
    
    /**
     * Start the provided operation of the provided microservice.<br>
     * If the microservice operation is already started it will be ignored.<br>
     * @param microserviceId The id of the microservice to start
     * @param operationId The id of the microservice operation to start
     * @throws Exception in case of error
     */
    public void startMicroservice(String microserviceId, String operationId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        operationId = (operationId == null || operationId.isEmpty())?serviceConfig.getString("defaultOperationId"):operationId;
        JsonObject operation = serviceConfig.getJsonObject("operations").getJsonObject(operationId);
        if(operation == null) throw new Exception("Impossible to find the operation with key " + operationId);
        String serviceKey = microserviceId + "_" + operationId;
        if(service_operationStartedList.containsKey(serviceKey))
            return;
        
        JsonObject configuration = operation.getJsonObject("configuration");
        String connectorId = configuration.getString("connectorId");
        JsonObject configStart = configuration.getJsonObject("configStart");
        JsonObject inputsAsync = configuration.getJsonObject("inputsAsync");
        String outputAdaptationAlgorithm = configuration.getString("outputAdaptationAlgorithm", "");
        
        String connectorInstanceId = null;
        if(inputsAsync == null) {
            connectorInstanceId = ConnectorsController.unique().startConnectorInstance(connectorId, configStart);
        } else {
            String responseServiceId = inputsAsync.getString("responseServiceId", "").isEmpty()?microserviceId:inputsAsync.getString("responseServiceId", "");
            String responseServiceOperationId = inputsAsync.getString("responseServiceOperationId", "");
            String responseServiceInputId = inputsAsync.getString("responseServiceInputId", "");
            String inputAdaptationAlgorithm = inputsAsync.getString("inputAdaptationAlgorithm", "");
            JsonObject responseServiceOtherInputs = inputsAsync.getJsonObject("responseServiceOtherInputs");
            
            connectorInstanceId = ConnectorsController.unique().startConnectorInstance(connectorId, configStart, new AsyncResponseHandlerI() {
                @Override
                public void handler(JsonObject asyncResponse) throws Exception {
                    String asyncResponseString = asyncResponse.toString();
                    
                    if(!outputAdaptationAlgorithm.isEmpty()) {
                        Object adaptationOutputString = Utils.outputAdaptation(asyncResponseString, "{}", outputAdaptationAlgorithm, true, microserviceId);
                        if(!(adaptationOutputString instanceof String))
                            throw new Exception("The returned object of an adaptation algorithm can be only a JSON string. Obtained: " + adaptationOutputString.toString());
                        asyncResponseString = (String)adaptationOutputString;
                    }
                    
                    if(!inputAdaptationAlgorithm.isEmpty()) {
                        Object inputAdaptedOutputString = Utils.outputAdaptation(asyncResponseString, null, inputAdaptationAlgorithm, true, microserviceId);
                        if(inputAdaptedOutputString == null)
                            return;
                        if(!(inputAdaptedOutputString instanceof String))
                            throw new Exception("The returned object of an input adaptation algorithm can be only null or a string. Obtained: " + inputAdaptedOutputString.toString());
                        asyncResponseString = (String)inputAdaptedOutputString;
                    }
                    
                    if(!responseServiceInputId.isEmpty()){
                        JsonObjectBuilder responseServiceInputsBuilder = Json.createObjectBuilder();
                        responseServiceInputsBuilder.add(responseServiceInputId, Json.createObjectBuilder().add("value", asyncResponseString));
                        for(String responseServiceOtherInputKey : responseServiceOtherInputs.keySet())
                            responseServiceInputsBuilder.add(responseServiceOtherInputKey, Json.createObjectBuilder().add("value", responseServiceOtherInputs.getJsonObject(responseServiceOtherInputKey).getString("value")));
                        
                        callMicroserviceForced(responseServiceId, responseServiceOperationId, responseServiceInputsBuilder.build());
                    }
                }
            });
        }
        
        HashMap<String, String> serviceProperties = new HashMap<String, String>();
        serviceProperties.put("connectorInstanceId", connectorInstanceId);
        serviceProperties.put("operationId", operationId);
        serviceProperties.put("operationName", operation.getString("name"));
        serviceProperties.put("operationDescription", operation.getString("description"));
        serviceProperties.put("microserviceId", microserviceId);
        serviceProperties.put("microserviceName", serviceConfig.getString("name"));
        serviceProperties.put("microserviceDescription", serviceConfig.getString("description"));
        service_operationStartedList.put(serviceKey, serviceProperties);
    }
    
    /**
     * Call the default operation of the provided microservice<br>
     * @param microserviceId The id of the microservice to call
     * @param microserviceInputs The JSON of the inputs in the format:
     * <pre>
     * {
     *      "id of one input" : {
     *          "value" : "The value for this input filed as described in the JSON template requiredInputTemplate returned by {@link #getMicroserviceIOInfo(String)}"
     *      },
     *      "id of another input" : {...},
     *      ...
     * }
     * </pre>
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject callMicroservice(String microserviceId, JsonObject microserviceInputs) throws Exception {
        return callMicroservice(microserviceId, null, microserviceInputs);
    }
    
    /**
     * Call the provided operation of the provided microservice<br>
     * @param microserviceId The id of the microservice to call
     * @param operationId The id of the microservice operation to call
     * @param microserviceInputs The JSON of the inputs is described in {@link #callMicroservice(String, JsonObject)}
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject callMicroservice(String microserviceId, String operationId, JsonObject microserviceInputs) throws Exception {
        return _callMicroservice(microserviceId, operationId, microserviceInputs, false, true);
    }
    /*
    public JsonObject callMicroservice_ToDelete(String microserviceId, String operationId, JsonObject microserviceInputs) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        if(microserviceInputs == null) throw new Exception("microserviceInputs must be a valid JSON object");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        operationId = (operationId == null || operationId.isEmpty())?serviceConfig.getString("defaultOperationId"):operationId;
        JsonObject operation = serviceConfig.getJsonObject("operations").getJsonObject(operationId);
        if(operation == null) throw new Exception("Impossible to find the operation with key " + operationId);
        
        String serviceKey = microserviceId + "_" + operationId;
        HashMap<String, String> serviceProperties = service_operationStartedList.get(serviceKey);
        if(serviceProperties == null) throw new Exception("The service " + microserviceId + " operation " + operationId + " is not started");
        String connectorInstanceId = serviceProperties.get("connectorInstanceId");
        JsonObject configuration = operation.getJsonObject("configuration");
        String configCall = configuration.getJsonObject("configCall").toString();
        JsonObject inputs = configuration.getJsonObject("inputs");
        
        for(String serviceInputKey : inputs.keySet()) {
            JsonObject configuredServiceInputForTheProvidedKey = inputs.getJsonObject(serviceInputKey);
            String matchingName = configuredServiceInputForTheProvidedKey.getString("matchingName");
            JsonObject serviceInput = microserviceInputs.getJsonObject(serviceInputKey);
            if(serviceInput == null) throw new Exception("The input with key " + serviceInputKey + " is missing");
            if(!serviceInput.containsKey("value")) throw new Exception("The input object with key " + serviceInputKey + " must contain a \"value\" json string");
            String serviceInputValue = serviceInput.getString("value");
            serviceInputValue = Utils.escapeJson(serviceInputValue);
            configCall = configCall.replace(matchingName, serviceInputValue);
        }
        
        JsonObject configCallJson = Json.createReader(new StringReader(configCall)).readObject();
        JsonObject connectorOutput = ConnectorsController.unique().callConnectorInstance(connectorInstanceId, configCallJson);
        
        String outputAdaptationAlgorithm = configuration.getString("outputAdaptationAlgorithm", "");
        if(outputAdaptationAlgorithm.isEmpty())
            return connectorOutput;
        else {
            Object adaptationOutputString = Utils.outputAdaptation(connectorOutput.toString(), microserviceInputs.toString(), outputAdaptationAlgorithm, true);
            if(!(adaptationOutputString instanceof String))
                throw new Exception("The returned object of an adaptation algorithm can be only a JSON string. Obtained: " + adaptationOutputString.toString());
            return Json.createReader(new StringReader((String)adaptationOutputString)).readObject();
        }
    }
    */
    
    /**
     * Stop the default operation of the provided microservice<br>
     * If the microservice operation is already stopped it will be ignored.<br>
     * @param microserviceId The id of the microservice to stop
     * @throws Exception in case of error
     */
    public void stopMicroservice(String microserviceId) throws Exception {
        stopMicroservice(microserviceId, null);
    }
    
    /**
     * Stop the provided operation of the provided microservice<br>
     * If the microservice operation is already stopped it will be ignored.<br>
     * @param microserviceId The id of the microservice to stop
     * @param operationId The id of the microservice operation to stop
     * @throws Exception in case of error
     */
    public void stopMicroservice(String microserviceId, String operationId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        operationId = (operationId == null || operationId.isEmpty())?serviceConfig.getString("defaultOperationId"):operationId;
        JsonObject operation = serviceConfig.getJsonObject("operations").getJsonObject(operationId);
        if(operation == null) throw new Exception("Impossible to find the operation with key " + operationId);
        
        String serviceKey = microserviceId + "_" + operationId;
        HashMap<String, String> serviceProperties = service_operationStartedList.get(serviceKey);
        if(serviceProperties == null) 
            return;
        String connectorInstanceId = serviceProperties.get("connectorInstanceId");
        ConnectorsController.unique().stopConnectorInstance(connectorInstanceId);
        service_operationStartedList.remove(serviceKey);
    }
    
    /**
     * Stop the all the started operations of the provided microservice.<br>
     * @param microserviceId The id of the microservice to look for the configuration
     * @throws Exception in case of error
     */
    public void stopAllMicroserviceOperations(String microserviceId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        HashMap<String, HashMap<String, String>> service_operationStartedListCopy = new HashMap<String, HashMap<String, String>>();
        service_operationStartedListCopy.putAll(service_operationStartedList);
        for(HashMap<String, String> startedProperties : service_operationStartedListCopy.values())
            if(microserviceId.equals(startedProperties.get("microserviceId")))
                stopMicroservice(startedProperties.get("microserviceId"), startedProperties.get("operationId"));
    }
    
    /**
     * Call the default operation of the provided microservice. If the microservice operation is stopped, will be started, called and then stopped.<br>
     * @param microserviceId The id of the microservice to call
     * @param microserviceInputs The JSON of the inputs is described in {@link #callMicroservice(String, JsonObject)}
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject callMicroserviceForced(String microserviceId, JsonObject microserviceInputs) throws Exception {
        return callMicroserviceForced(microserviceId, null, microserviceInputs);
    }
    
    /**
     * Call the provided operation of the provided microservice. If the microservice operation is stopped, will be started, called and then stopped.<br>
     * @param microserviceId The id of the microservice to call
     * @param operationId The id of the microservice operation to call
     * @param microserviceInputs The JSON of the inputs is described in {@link #callMicroservice(String, JsonObject)}
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject callMicroserviceForced(String microserviceId, String operationId, JsonObject microserviceInputs) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        if(microserviceInputs == null) throw new Exception("microserviceInputs must be a valid JSON object");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        operationId = (operationId == null || operationId.isEmpty())?serviceConfig.getString("defaultOperationId"):operationId;
        JsonObject operation = serviceConfig.getJsonObject("operations").getJsonObject(operationId);
        if(operation == null) throw new Exception("Impossible to find the operation with key " + operationId);
        
        String serviceKey = microserviceId + "_" + operationId;
        if(service_operationStartedList.get(serviceKey) != null) {
            return callMicroservice(microserviceId, operationId, microserviceInputs);
        } else {
            startMicroservice(microserviceId, operationId);
            try {
                return callMicroservice(microserviceId, operationId, microserviceInputs);
            } finally {
                stopMicroservice(microserviceId, operationId);
            }
        }
    }
    
    
    /**
     * Call the default operation of the provided microservice. If the microservice operation is stopped, will be started, called and then stopped. A new Thread is not created in this case.<br>
     * @param microserviceId The id of the microservice to call
     * @param microserviceInputs The JSON of the inputs is described in {@link #callMicroservice(String, JsonObject)}
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject callMicroserviceForcedNoThread(String microserviceId, JsonObject microserviceInputs) throws Exception {
        return callMicroserviceForcedNoThread(microserviceId, null, microserviceInputs);
    }
    
    /**
     * Call the provided operation of the provided microservice. If the microservice operation is stopped, will be started, called and then stopped. A new Thread is not created in this case.<br>
     * @param microserviceId The id of the microservice to call
     * @param operationId The id of the microservice operation to call
     * @param microserviceInputs The JSON of the inputs is described in {@link #callMicroservice(String, JsonObject)}
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject callMicroserviceForcedNoThread(String microserviceId, String operationId, JsonObject microserviceInputs) throws Exception {
        return _callMicroservice(microserviceId, operationId, microserviceInputs, true, false);
    }
    
    /**
     * Check the current connector status of the default operation of the provided microservice<br>
     * @param microserviceId The id of the microservice to check
     * @return JsonObject A JSON object of this format: 
     * <pre>
     * {
     *      "connectorInstanceStatus" : "STARTED/STOPPED/ERROR",
     *      "error" : ""
     * }
     * </pre>
     * @throws Exception in case of error
     */
    public JsonObject checkMicroserviceConnectorStatus(String microserviceId) throws Exception {
        return checkMicroserviceConnectorStatus(microserviceId, null);
    }
    
    /**
     * Check the current connector status of the provided operation for the provided microservice<br>
     * @param microserviceId The id of the microservice to check
     * @param operationId The id of the microservice operation to check
     * @return JsonObject A JSON object as specified in {@link #checkMicroserviceConnectorStatus(String)}: 
     * @throws Exception in case of error
     */
    public JsonObject checkMicroserviceConnectorStatus(String microserviceId, String operationId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        operationId = (operationId == null || operationId.isEmpty())?serviceConfig.getString("defaultOperationId"):operationId;
        JsonObject operation = serviceConfig.getJsonObject("operations").getJsonObject(operationId);
        if(operation == null) throw new Exception("Impossible to find the operation with key " + operationId);
        
        String serviceKey = microserviceId + "_" + operationId;
        HashMap<String, String> serviceProperties = service_operationStartedList.get(serviceKey);
        if(serviceProperties == null)
            return Json.createObjectBuilder().add("connectorInstanceStatus", "STOPPED").add("error", "").build();
        String connectorInstanceId = serviceProperties.get("connectorInstanceId");
        JsonObject connectorInstanceCheckResult = ConnectorsController.unique().checkConnectorInstanceStatus(connectorInstanceId);
        if(connectorInstanceCheckResult.getString("connectorInstanceStatus").equals("ERROR"))
            return Json.createObjectBuilder().add("connectorInstanceStatus", "ERROR").add("error", connectorInstanceCheckResult.getString("error")).build();
        if(connectorInstanceCheckResult.getString("connectorInstanceStatus").equals("STOPPED"))
            return Json.createObjectBuilder().add("connectorInstanceStatus", "STOPPED").add("error", "Connector Instance is stopped but the microservice is started. Consider executing the fixing service operation").build();
        else
            return Json.createObjectBuilder().add("connectorInstanceStatus", "STARTED").add("error", "").build();
    }
    
    /**
     * Check the current status of the default operation of the provided microservice<br>
     * @param microserviceId The id of the microservice to check
     * @return JsonObject A JSON object of this format: 
     * <pre>
     * {
     *      "connectorInstanceStatus" : "STARTED/STOPPED/ERROR",
     *      "serviceStatus" : "UP/DOWN/UNKNOWN/ERROR",
     *      "error" : ""
     * }
     * </pre>
     * @throws Exception in case of error
     */
    public JsonObject checkMicroserviceStatus(String microserviceId) throws Exception {
        return checkMicroserviceStatus(microserviceId, null);
    }
    
    /**
     * Check the current status of the provided operation for the provided microservice<br>
     * @param microserviceId The id of the microservice to check
     * @param operationId The id of the microservice operation to check
     * @return JsonObject A JSON object as specified in {@link #checkMicroserviceStatus(String)}: 
     * @throws Exception in case of error
     */
    public JsonObject checkMicroserviceStatus(String microserviceId, String operationId) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        operationId = (operationId == null || operationId.isEmpty())?serviceConfig.getString("defaultOperationId"):operationId;
        JsonObject operation = serviceConfig.getJsonObject("operations").getJsonObject(operationId);
        if(operation == null) throw new Exception("Impossible to find the operation with key " + operationId);
        
        String serviceKey = microserviceId + "_" + operationId;
        HashMap<String, String> serviceProperties = service_operationStartedList.get(serviceKey);
        if(serviceProperties == null)
            return Json.createObjectBuilder().add("connectorInstanceStatus", "STOPPED").add("serviceStatus", "UNKNOWN").add("error", "").build();
        String connectorInstanceId = serviceProperties.get("connectorInstanceId");
        JsonObject connectorInstanceCheckResult = ConnectorsController.unique().checkConnectorInstanceStatus(connectorInstanceId);
        if(connectorInstanceCheckResult.getString("connectorInstanceStatus").equals("ERROR"))
            return Json.createObjectBuilder().add("connectorInstanceStatus", "ERROR").add("serviceStatus", "UNKNOWN").add("error", connectorInstanceCheckResult.getString("error")).build();
        if(connectorInstanceCheckResult.getString("connectorInstanceStatus").equals("STOPPED"))
            return Json.createObjectBuilder().add("connectorInstanceStatus", "STOPPED").add("serviceStatus", "UNKNOWN").add("error", "Connector Instance is stopped but the microservice is started. Consider executing the fixing service operation").build();
        
        //from here is sure that the module is STARTED
        JsonObject configuration = operation.getJsonObject("configuration");
        String configCall = configuration.getJsonObject("configCall").toString();
        JsonObject inputs = configuration.getJsonObject("inputs");
        for(String serviceInputKey : inputs.keySet()) {
            JsonObject serviceInput = inputs.getJsonObject(serviceInputKey);
            String serviceInputTestValue = serviceInput.getString("workingExample");
            String matchingName = serviceInput.getString("matchingName");
            serviceInputTestValue = Utils.escapeJson(serviceInputTestValue);
            configCall = configCall.replace(matchingName, serviceInputTestValue);
        }
        String statusCheckAlgorithm = configuration.getString("statusCheckAlgorithm");
        JsonObject configCallJson = Json.createReader(new StringReader(configCall)).readObject();
        try {
            JsonObject serviceOutput = ConnectorsController.unique().callConnectorInstance(connectorInstanceId, configCallJson);
            if(statusCheckAlgorithm.isEmpty())
                return Json.createObjectBuilder().add("connectorInstanceStatus", "STARTED").add("serviceStatus", "UP").add("error", "").build();
            else {
                Object statusCheckOutputBoolean = Utils.outputAdaptation(serviceOutput.toString(), null, statusCheckAlgorithm, false);
                if(!(statusCheckOutputBoolean instanceof Boolean)) throw new Exception("The last expression of the Javascript algorithm must be a boolean expression. Returned: " + statusCheckOutputBoolean);
                boolean status = (boolean)statusCheckOutputBoolean;
                return Json.createObjectBuilder().add("connectorInstanceStatus", "STARTED").add("serviceStatus", status?"UP":"DOWN").add("error", "").build();
            }
        } catch (Exception ex) {
            return Json.createObjectBuilder().add("connectorInstanceStatus", "STARTED").add("serviceStatus", "ERROR").add("error", Utils.escapeJson(ex.getMessage())).build();
        }
    }
    
    /**
     * Terminate all the unresponding started services operations<br>
     * @throws Exception in case of error
     */
    public void fixAllStartedMicroservices() throws Exception {
        ConnectorsController.unique().fixAllConnectorInstances();
        ArrayList<String> toRemove = new ArrayList<String>();
        for(Entry<String, HashMap<String, String>> entry : service_operationStartedList.entrySet())
            if(ConnectorsController.unique().checkConnectorInstanceStatus(entry.getValue().get("connectorInstanceId")).getString("connectorInstanceStatus").equals("STOPPED"))
                toRemove.add(entry.getKey());
        for(String key : toRemove)
            service_operationStartedList.remove(key);
    }
    
    /**
     * Perform a direct call to a synchronous connector and return the results<br>
     * @param connectorConfiguration The JSON object specifying the connector configuration. The format accepted is the following and is an extract of the microservice configuration described in {@link #createMicroservice(JsonObject)}}: 
     * <pre>
     * {
     *      "connectorId" : "id of the connector to use, obtained from {@link #getAvailableConnectors()}",
     *      "configStart" : {... JSON containing the connector specific starting configuration as specified by the startConfigurationTemplate obtained from {@link #getAvailableConnectors()}, but filled with a value ...},
     *      "configCall" : {... JSON containing the connector specific starting configuration as specified by the callConfigurationTemplate obtained from {@link #getAvailableConnectors()}, but filled with a value ...}
     * }
     * </pre>
     * @return JsonObject The result of the call as a JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject callSyncConnectorForced(JsonObject connectorConfiguration) throws Exception {
        String connectorId = connectorConfiguration.getString("connectorId", "");
        if(connectorId.isEmpty()) throw new Exception("connectorId must be provided");
        JsonObject startConfiguration = connectorConfiguration.getJsonObject("configStart");
        if(startConfiguration == null) throw new Exception("configStart must be provided as JSON Object");
        JsonObject callConfiguration = connectorConfiguration.getJsonObject("configCall");
        if(callConfiguration == null) throw new Exception("configCall must be provided as JSON Object");
        return ConnectorsController.unique().forceCallConnectorInstance(connectorId, startConfiguration, callConfiguration);
    }
    
    
    private void checkAutostart(String microserviceId, JsonObject microserviceConfiguration) {
        JsonObject operations = microserviceConfiguration.getJsonObject("operations");
        for(String operationKey : operations.keySet())
            if(operations.getJsonObject(operationKey).getBoolean("autostart", false))
                try {
                    startMicroservice(microserviceId, operationKey);
                } catch (Exception e) {
                    LogManager.unique().log(LogLevel.ERROR, "Error on the Autostart of microservice '" + microserviceId + "' with operation '" + operationKey + "'", e);
                }
    }
    
    private JsonObject _callMicroservice(String microserviceId, String operationId, JsonObject microserviceInputs, boolean forceStart, boolean useThread) throws Exception {
        if(microserviceId == null || microserviceId.isEmpty()) throw new Exception("microserviceId can not be empty");
        if(microserviceInputs == null) throw new Exception("microserviceInputs must be a valid JSON object");
        JsonObject serviceConfig = PersistenceManager.unique().retrieveMicroserviceConfiguration(microserviceId);
        operationId = (operationId == null || operationId.isEmpty())?serviceConfig.getString("defaultOperationId"):operationId;
        JsonObject operation = serviceConfig.getJsonObject("operations").getJsonObject(operationId);
        if(operation == null) throw new Exception("Impossible to find the operation with key " + operationId);
        String connectorInstanceId = null;
        if(!forceStart) {
            String serviceKey = microserviceId + "_" + operationId;
            HashMap<String, String> serviceProperties = service_operationStartedList.get(serviceKey);
            if(serviceProperties == null) throw new Exception("The service " + microserviceId + " operation " + operationId + " is not started");
            connectorInstanceId = serviceProperties.get("connectorInstanceId");
        }
        
        JsonObject configuration = operation.getJsonObject("configuration");
        String connectorId = configuration.getString("connectorId");
        JsonObject configStart = configuration.getJsonObject("configStart");
        String configCall = configuration.getJsonObject("configCall").toString();
        JsonObject inputs = configuration.getJsonObject("inputs");
        
        for(String serviceInputKey : inputs.keySet()) {
            JsonObject configuredServiceInputForTheProvidedKey = inputs.getJsonObject(serviceInputKey);
            String matchingName = configuredServiceInputForTheProvidedKey.getString("matchingName");
            JsonObject serviceInput = microserviceInputs.getJsonObject(serviceInputKey);
            if(serviceInput == null) throw new Exception("The input with key " + serviceInputKey + " is missing");
            if(!serviceInput.containsKey("value")) throw new Exception("The input object with key " + serviceInputKey + " must contain a \"value\" json string");
            String serviceInputValue = serviceInput.getString("value");
            serviceInputValue = Utils.escapeJson(serviceInputValue);
            configCall = configCall.replace(matchingName, serviceInputValue);
        }
        
        JsonObject configCallJson = Json.createReader(new StringReader(configCall)).readObject();
        
        JsonObject connectorOutput = !forceStart ? ConnectorsController.unique().callConnectorInstance(connectorInstanceId, configCallJson) : (useThread ? ConnectorsController.unique().forceCallConnectorInstance(connectorId, configStart, configCallJson) : ConnectorsController.unique().forceCallConnectorDirect(connectorId, configStart, configCallJson));
        
        String outputAdaptationAlgorithm = configuration.getString("outputAdaptationAlgorithm", "");
        if(outputAdaptationAlgorithm.isEmpty())
            return connectorOutput;
        else {
            Object adaptationOutputString = Utils.outputAdaptation(connectorOutput.toString(), microserviceInputs.toString(), outputAdaptationAlgorithm, true, microserviceId);
            if(!(adaptationOutputString instanceof String))
                throw new Exception("The returned object of an adaptation algorithm can be only a JSON string. Obtained: " + adaptationOutputString.toString());
            return Json.createReader(new StringReader((String)adaptationOutputString)).readObject();
        }
    }
    
    /*
    public static void main(String[] argv){
        try {
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
                    .add("async", Json.createObjectBuilder()
                            .add("name", "async")
                            .add("description", "async")
                            .add("autostart", false)
                            .add("configuration", Json.createObjectBuilder()
                                .add("connectorId", org.adoxx.microservice.api.connectors.impl.DummyAsyncConnector.class.getName())
                                .add("configStart", Json.createObjectBuilder()
                                    .add("Text", Json.createObjectBuilder().add("value", "AsyncGenText"))
                                )
                                .add("configCall", Json.createObjectBuilder())
                                .add("inputs", Json.createObjectBuilder())
                                .add("inputsAsync", Json.createObjectBuilder()
                                    .add("responseServiceId", "TEST")
                                    .add("responseServiceOperationId", "sync")
                                    //.add("responseServiceInputId", "Your Name")
                                    .add("responseServiceInputId", "")
                                    .add("responseServiceOtherInputs", Json.createObjectBuilder())
                                    .add("inputAdaptationAlgorithm", "output.dataText + ' adapted';")
                                )
                                .add("outputDescription", "todo")
                                //.add("outputAdaptationAlgorithm", "out(output);")
                                .add("outputAdaptationAlgorithm", "while(true); console.log('LOG ASYNC: ' + out(output)); var name = output.dataText; out(callMicroservice('TEST', 'sync', {'Your Name':{'value':name}}));")
                                .add("statusCheckAlgorithm", "output!=null;")
                            )
                        )
                ).add("moreInfos", Json.createObjectBuilder()
                    .add("visible", true)
                )
                .build()
            );
            
            
            //MicroserviceController.unique().startMicroservice("TEST", "sync");
            //System.out.println("TEST SYNC CALL: " + MicroserviceController.unique().callMicroservice("TEST", "sync", Json.createObjectBuilder().add("Your Name", Json.createObjectBuilder().add("value", "DAM")).build()));
            //System.out.println("TEST SYNC STATUS: " + MicroserviceController.unique().checkMicroserviceStatus("TEST", "sync"));
            //MicroserviceController.unique().stopMicroservice("TEST", "sync");
            //System.out.println("TEST SYNC CALL FORCED: " + MicroserviceController.unique().callMicroserviceForced("TEST", "sync", Json.createObjectBuilder().add("Your Name", Json.createObjectBuilder().add("value", "DAM")).build()));
            //System.out.println("TEST SYNC CALL FORCED NO THREAD: " + MicroserviceController.unique().callMicroserviceForcedNoThread("TEST", "sync", Json.createObjectBuilder().add("Your Name", Json.createObjectBuilder().add("value", "DAM")).build()));
            
            
            MicroserviceController.unique().startMicroservice("TEST", "async");
            //System.out.println("TEST ASYNC CALL: " + MicroserviceController.unique().callMicroservice("TEST", "async", Json.createObjectBuilder().build()));
            //System.out.println("TEST ASYNC STATUS: " + MicroserviceController.unique().checkMicroserviceStatus("TEST", "async"));
            //MicroserviceController.unique().stopMicroservice("TEST", "async");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    */
}
