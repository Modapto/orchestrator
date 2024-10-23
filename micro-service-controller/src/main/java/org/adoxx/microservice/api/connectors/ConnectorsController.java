package org.adoxx.microservice.api.connectors;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.adoxx.microservice.api.connectors.impl.ADOxxAssetEngineConnector;
import org.adoxx.microservice.api.connectors.impl.ADOxxClassicSafeConnector;
import org.adoxx.microservice.api.connectors.impl.ADOxxClassicUnsafeConnector;
//import org.adoxx.microservice.api.connectors.impl.ActivitiDMNEngineConnector;
import org.adoxx.microservice.api.connectors.impl.CamundaDMNEngineConnector;
import org.adoxx.microservice.api.connectors.impl.CommandLineConnector;
import org.adoxx.microservice.api.connectors.impl.DummyAsyncConnector;
import org.adoxx.microservice.api.connectors.impl.DummySyncConnector;
import org.adoxx.microservice.api.connectors.impl.ExcelConnector;
import org.adoxx.microservice.api.connectors.impl.FacebookGraphConnector;
import org.adoxx.microservice.api.connectors.impl.JMS11SubscriberConnector;
import org.adoxx.microservice.api.connectors.impl.JavascriptEngineConnector;
import org.adoxx.microservice.api.connectors.impl.LDAPConnector;
import org.adoxx.microservice.api.connectors.impl.ContentProviderConnector;
import org.adoxx.microservice.api.connectors.impl.ContentReceiverConnector;
import org.adoxx.microservice.api.connectors.impl.DashboardConnector;
import org.adoxx.microservice.api.connectors.impl.MSSQLServerConnector;
import org.adoxx.microservice.api.connectors.impl.MicroserviceConnector;
import org.adoxx.microservice.api.connectors.impl.MySQLConnector;
import org.adoxx.microservice.api.connectors.impl.OMiLabReservationServiceConnector;
import org.adoxx.microservice.api.connectors.impl.PostgreSQLConnector;
import org.adoxx.microservice.api.connectors.impl.RDF4JRESTConnector;
import org.adoxx.microservice.api.connectors.impl.RESTConnector;
import org.adoxx.microservice.api.connectors.impl.REngineConnector;
import org.adoxx.microservice.api.connectors.impl.SMTPConnector;
import org.adoxx.microservice.api.connectors.impl.SOAPConnector;
import org.adoxx.microservice.api.connectors.impl.SPM2018DemoConnector;
import org.adoxx.microservice.api.connectors.impl.FusekiRESTConnector;
import org.adoxx.microservice.api.connectors.impl.HPPrinterMonitor;
import org.adoxx.microservice.api.connectors.impl.JFuzzyLogicEngineConnector;
import org.adoxx.microservice.api.connectors.impl.JMS11PublisherConnector;
import org.adoxx.microservice.api.connectors.impl.TwilioSMSSenderConnector;
import org.adoxx.microservice.api.connectors.impl.TwitterPOSTConnector;
import org.adoxx.microservice.api.connectors.impl.TwitterSearchConnector;
import org.adoxx.microservice.api.connectors.impl.TwitterUserInfosConnector;

/**
 * <h1>ConnectorsController</h1>
 * This Class contains methods to manage connectors.<br>
 * A connector is a class that manage the connection to a particular type of endpoint.<br>
 * In order to be recognized as a connector, a class must extend one of the two abstract classes {@link SyncConnectorA} or {@link AsyncConnectorA} depending if the connector work in a synchronous or asynchronous way.<br>
 * In case the connector is user defined, it must be added to the controller using {@link #addConnector(String)}.<br>
 * In order to be used, a connector instance must be started using {@link #startConnectorInstance(String, JsonObject)} that will create a new instance of the connector, call its starting procedures and return the connector new instance id.<br>
 * Once a connector is started it can be called, stopped and checked using its instance id.
 * @author Damiano Falcioni
 */
public class ConnectorsController {

    private HashMap<String, Class<?>> asyncConnectorsList = new HashMap<String, Class<?>>();
    private HashMap<String, Class<?>> syncConnectorsList = new HashMap<String, Class<?>>();
    
    private JsonObjectBuilder connectorsDescriptionBuilder = Json.createObjectBuilder();
    private JsonObject connectorsDescriptionJson = Json.createObjectBuilder().build();
    
    private HashMap<String, AsyncConnectorA> asyncConnectorInstancesList = new HashMap<String, AsyncConnectorA>();
    private HashMap<String, SyncConnectorA> syncConnectorInstancesList = new HashMap<String, SyncConnectorA>();
    
    private static ConnectorsController connectorsController = null;
    
    /**
     * Factory method that return everytime the same ConnectorsController instance.<br>
     * @return ConnectorsController The unique ConnectorsController instance
     */
    public static ConnectorsController unique() throws Exception {
        if(connectorsController == null)
            connectorsController = new ConnectorsController();
        return connectorsController;
    }
    
    /**
     * Factory method that update the unique ConnectorsController instance with a new one and return it.<br>
     * @return ConnectorsController The new ConnectorsController instance
     */
    public static ConnectorsController newUnique() throws Exception {
        connectorsController = new ConnectorsController();
        return connectorsController;
    }
    
    /**
     * Constructor method that initialize the list of internally available connectors (all the classes of the package {@link org.adoxx.microservice.api.connectors.impl}).<br>
     */
    //FIXME: can this be done dynamically?
    public ConnectorsController() throws Exception {
        addConnector(ADOxxAssetEngineConnector.class.getName());
        addConnector(DummySyncConnector.class.getName());
        addConnector(DummyAsyncConnector.class.getName());
        addConnector(ExcelConnector.class.getName());
        addConnector(JMS11SubscriberConnector.class.getName());
        addConnector(JMS11PublisherConnector.class.getName());
        addConnector(ContentProviderConnector.class.getName());
        addConnector(ContentReceiverConnector.class.getName());
        addConnector(MicroserviceConnector.class.getName());
        addConnector(MSSQLServerConnector.class.getName());
        addConnector(MySQLConnector.class.getName());
        addConnector(PostgreSQLConnector.class.getName());
        addConnector(LDAPConnector.class.getName());
        addConnector(RESTConnector.class.getName());
        addConnector(FusekiRESTConnector.class.getName());
        addConnector(RDF4JRESTConnector.class.getName());
        addConnector(TwilioSMSSenderConnector.class.getName());
        addConnector(SMTPConnector.class.getName());
        addConnector(TwitterPOSTConnector.class.getName());
        addConnector(TwitterSearchConnector.class.getName());
        addConnector(TwitterUserInfosConnector.class.getName());
        addConnector(FacebookGraphConnector.class.getName());
        addConnector(CamundaDMNEngineConnector.class.getName());
        //addConnector(ActivitiDMNEngineConnector.class.getName());
        addConnector(JavascriptEngineConnector.class.getName());
        addConnector(REngineConnector.class.getName());
        addConnector(SPM2018DemoConnector.class.getName());
        addConnector(DashboardConnector.class.getName());
        addConnector(OMiLabReservationServiceConnector.class.getName());
        addConnector(SOAPConnector.class.getName());
        addConnector(ADOxxClassicUnsafeConnector.class.getName());
        addConnector(ADOxxClassicSafeConnector.class.getName());
        addConnector(HPPrinterMonitor.class.getName());
        addConnector(JFuzzyLogicEngineConnector.class.getName());
        addConnector(CommandLineConnector.class.getName());
    }
    
    /**
     * Add a new connector class.<br>
     * In order to be added, the class must be available in the classpath and must extend one of the two abstract classes {@link SyncConnectorA} or {@link AsyncConnectorA} depending if the connector work in a synchronous or asynchronous way.<br>
     * @param className The full qualified name of the class.
     */
    public void addConnector(String className) throws Exception {
        if(asyncConnectorsList.containsKey(className)) throw new Exception("The connector " + className + " is already present");
        
        Class<?> connectorClass = Class.forName(className);
        if(connectorClass.getSuperclass() == null || (!connectorClass.getSuperclass().getName().equals(SyncConnectorA.class.getName()) && !connectorClass.getSuperclass().getName().equals(AsyncConnectorA.class.getName())))
            throw new Exception("The class " + className + " must extend the abstract class " + SyncConnectorA.class.getName() + " or " + AsyncConnectorA.class.getName());
        boolean isAsyncConnector = connectorClass.getSuperclass().getName().equals(AsyncConnectorA.class.getName());
        
        boolean haveDefaultConstructor = connectorClass.getConstructors().length == 0;
        for(Constructor<?> connectorConstructor : connectorClass.getConstructors())
            if(connectorConstructor.getParameterCount() == 0)
                haveDefaultConstructor = true;
        if(!haveDefaultConstructor) throw new Exception("The class " + className + " must provide a constructor without parameters");
        
        SyncConnectorA connector = (SyncConnectorA) connectorClass.newInstance();
        connectorsDescriptionBuilder.add(className, Json.createObjectBuilder()
            .add("name", connector.getName())
            .add("description", connector.getDescription())
            .add("callConfigurationTemplate", connector.getCallConfigurationTemplate())
            .add("startConfigurationTemplate", connector.getStartConfigurationTemplate())
            .add("outputDescription", connector.getOutputDescription())
            .add("asyncConnectionRequired", isAsyncConnector)
        );
        
        updateConnectorsDescription();
        
        if(isAsyncConnector)
            asyncConnectorsList.put(className, connectorClass);
        else
            syncConnectorsList.put(className, connectorClass);
    }
    
    /**
     * Return a JSON object containing all the available connectors<br>
     * @return JsonObject a JSON object of the following format:
     * <pre>
     * {
     *      "unique id of one connector (full qualified class name)" : {
     *          "name" : "name of the connector as returned by {@link BasicConnectorA#getName()}",
     *          "description" : {... description of the connector as returned by {@link BasicConnectorA#getDescription()} ...}
     *          "callConfigurationTemplate" : {... call configuration template as returned by {@link BasicConnectorA#getCallConfigurationTemplate()}, to be used in order to create the input configuration for {@link #callConnectorInstance(String, JsonObject)} ...},
     *          "startConfigurationTemplate" : {... start configuration template as returned by {@link SyncConnectorA#getStartConfigurationTemplate()}, to be used in order to create the input configuration for {@link #startConnectorInstance(String, JsonObject)} ...},
     *          "outputDescription" : "... the connector output description as returned by {@link BasicConnectorA#getOutputDescription()}}",
     *          "asyncConnectionRequired" : true if the connector extend the class {@link AsyncConnectorA}, false otherwise
     *      },
     *      "unique id of another connector (full qualified class name)" : {
     *          ...
     *      }
     * }
     * </pre>
     */
    public JsonObject getConnectors(){
        return connectorsDescriptionJson;
        /*
        JsonObject ret = connectorsDescriptionBuilder.build();
        JsonObjectBuilder newBuilder = Json.createObjectBuilder();
        for(Entry<String, JsonValue> entry : ret.entrySet())
            newBuilder.add(entry.getKey(), entry.getValue());
        connectorsDescriptionBuilder = newBuilder;
        return ret;
        */
    }

    /**
     * Create a new instance for the provided synchronous connector and start it.<br>
     * The process create a new Thread for the instance calling the {@link SyncConnectorA#threadStart(JsonObject)} and it is keeped active till a stop operation happend. The starting is so not blocking.<br>
     * @param connectorId The id of the connector as returned {@link #getConnectors()}
     * @param startConfiguration The starting configuration as returned by the JSON field startConfigurationTemplate of {@link #getConnectors()}, but filled with values
     * @return String The id of the new instance created.
     */
    public String startConnectorInstance(String connectorId, JsonObject startConfiguration) throws Exception {
        return startConnectorInstance(connectorId, startConfiguration, null);
    }
    
    /**
     * Create a new instance for the provided asynchronous connector and start it.<br>
     * The process create a new Thread for the instance calling the {@link SyncConnectorA#threadStart(JsonObject)} and it is keeped active till a stop operation happend. The starting is so not blocking.<br>
     * @param connectorId The id of the connector as returned {@link #getConnectors()}
     * @param startConfiguration The starting configuration as returned by the JSON field startConfigurationTemplate of {@link #getConnectors()}, but filled with values
     * @param asyncResponseHandler The handler to manage the asynchronous responses. Once a response is received it will be received as parameter of {@link AsyncResponseHandlerI#handler(JsonObject)}
     * @return String The id of the new instance created.
     */
    public String startConnectorInstance(String connectorId, JsonObject startConfiguration, AsyncResponseHandlerI asyncResponseHandler) throws Exception {
        if(!asyncConnectorsList.containsKey(connectorId) && !syncConnectorsList.containsKey(connectorId)) throw new Exception("The connector " + connectorId + " is not present");
        String id = UUID.randomUUID().toString();
        
        if(asyncConnectorsList.containsKey(connectorId)) {
            if(asyncResponseHandler==null) throw new Exception("asyncResponseHandler can not be null for the async connector " + connectorId);
            AsyncConnectorA connector = (AsyncConnectorA)asyncConnectorsList.get(connectorId).newInstance();
            asyncConnectorInstancesList.put(id, connector);
            connector.setAsyncResponsesHandlerSafe(asyncResponseHandler);
            connector.threadStart(startConfiguration);
        } else {
            if(asyncResponseHandler!=null) throw new Exception("asyncResponseHandler must be null for the sync connector " + connectorId);
            SyncConnectorA connector = (SyncConnectorA)syncConnectorsList.get(connectorId).newInstance();
            syncConnectorInstancesList.put(id, connector);
            connector.threadStart(startConfiguration);
        }
        
        return id;
    }
    
    /**
     * Call the provided connector instance in a safe way, waiting first for the finalization of the starting procedures.<br>
     * @param startedConnectorInstanceId The id of the started connector instance
     * @param callConfiguration The configuration as returned by the JSON field callConfigurationTemplate of {@link #getConnectors()}, but filled with values
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     */
    public JsonObject callConnectorInstance(String startedConnectorInstanceId, JsonObject callConfiguration) throws Exception {
        if(!asyncConnectorInstancesList.containsKey(startedConnectorInstanceId) && !syncConnectorInstancesList.containsKey(startedConnectorInstanceId)) throw new Exception("The connector instance " + startedConnectorInstanceId + " is not present");
        
        if(asyncConnectorInstancesList.containsKey(startedConnectorInstanceId)) {
            AsyncConnectorA connector = asyncConnectorInstancesList.get(startedConnectorInstanceId);
            connector.waitThreadStart();
            return connector.performCallSafe(callConfiguration);
        } else {
            SyncConnectorA connector = syncConnectorInstancesList.get(startedConnectorInstanceId);
            connector.waitThreadStart();
            return connector.performCallSafe(callConfiguration);
        }
    }
    
    /**
     * Stop a connector instance.<br>
     * The process terminate the started Thread calling the {@link SyncConnectorA#threadStop()}.<br>
     * @param startedConnectorInstanceId The id of the started connector instance to stop
     */
    public void stopConnectorInstance(String startedConnectorInstanceId) throws Exception {
        if(!asyncConnectorInstancesList.containsKey(startedConnectorInstanceId) && !syncConnectorInstancesList.containsKey(startedConnectorInstanceId)) throw new Exception("The connector instance " + startedConnectorInstanceId + " is not present");
        if(asyncConnectorInstancesList.containsKey(startedConnectorInstanceId)) {
            AsyncConnectorA connector = asyncConnectorInstancesList.get(startedConnectorInstanceId);
            connector.waitThreadStart();
            connector.threadStop();
            connector.waitThreadStop();
            asyncConnectorInstancesList.remove(startedConnectorInstanceId);
        } else {
            SyncConnectorA connector = syncConnectorInstancesList.get(startedConnectorInstanceId);
            connector.waitThreadStart();
            connector.threadStop();
            connector.waitThreadStop();
            syncConnectorInstancesList.remove(startedConnectorInstanceId);
        }
    }
    
    /**
     * Force a Call to the specified connector automatically starting an instance and stopping it in the end.<br>
     * @param connectorId The id of the connector as returned {@link #getConnectors()}
     * @param startConfiguration The starting configuration as returned by the JSON field startConfigurationTemplate of {@link #getConnectors()}, but filled with values     * 
     * @param callConfiguration The configuration as returned by the JSON field callConfigurationTemplate of {@link #getConnectors()}, but filled with values
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     */
    public JsonObject forceCallConnectorInstance(String connectorId, JsonObject startConfiguration, JsonObject callConfiguration) throws Exception {
        String instanceId = startConnectorInstance(connectorId, startConfiguration);
        try {
            JsonObject ret = callConnectorInstance(instanceId, callConfiguration);
            return ret;
        } finally {
            stopConnectorInstance(instanceId);
        }
    }
    
    /**
     * Call the specified connector directly without starting a new Thread.<br>
     * @param connectorId The id of the connector as returned {@link #getConnectors()}
     * @param startConfiguration The starting configuration as returned by the JSON field startConfigurationTemplate of {@link #getConnectors()}, but filled with values
     * @param callConfiguration The configuration as returned by the JSON field callConfigurationTemplate of {@link #getConnectors()}, but filled with values
     * @return JsonObject A JSON object in one of the two formats returned by {@link BasicConnectorA#performCall(JsonObject)}
     */
    public JsonObject forceCallConnectorDirect(String connectorId, JsonObject startConfiguration, JsonObject callConfiguration) throws Exception {
        if(!syncConnectorsList.containsKey(connectorId)) throw new Exception("The connector " + connectorId + " is not present");
        SyncConnectorA connector = (SyncConnectorA)syncConnectorsList.get(connectorId).newInstance();
        try {
            connector.start(startConfiguration);
            JsonObject ret = connector.performCall(callConfiguration);
            return ret;
        } finally {
            connector.stop();
        }
    }
    
    /**
     * Check the current status of a connector instance.<br>
     * @param startedConnectorInstanceId The id of the started connector instance to check
     * @return JsonObject a JSON object of the following format:
     * <pre>
     * {
     *      "connectorInstanceStatus" : "STARTED/STOPPED/ERROR",
     *      "error" : ""
     * }
     * </pre>
     */
    public JsonObject checkConnectorInstanceStatus(String startedConnectorInstanceId) {
        String status = "STARTED";
        String error = "";
        if(!asyncConnectorInstancesList.containsKey(startedConnectorInstanceId) && !syncConnectorInstancesList.containsKey(startedConnectorInstanceId)) {
            status = "STOPPED";
        } else {
            if(asyncConnectorInstancesList.containsKey(startedConnectorInstanceId)) {
                AsyncConnectorA connector = asyncConnectorInstancesList.get(startedConnectorInstanceId);
                if(connector.getThreadLastError() != null) {
                    status = "ERROR";
                    error = connector.getThreadLastError().getMessage();
                }
            } else {
                SyncConnectorA connector = syncConnectorInstancesList.get(startedConnectorInstanceId);
                if(connector.getThreadLastError() != null) {
                    status = "ERROR";
                    error = connector.getThreadLastError().getMessage();
                }
            }
        }
        
        return Json.createObjectBuilder().add("connectorInstanceStatus", status).add("error", error).build();
    }
    
    /**
     * Terminate all the unresponding started instances<br>
     */
    public void fixAllConnectorInstances() {
        ArrayList<String> toRemoveAsync = new ArrayList<String>();
        ArrayList<String> toRemoveSync = new ArrayList<String>();
        for(Entry<String, AsyncConnectorA> entry : asyncConnectorInstancesList.entrySet())
            if(entry.getValue().isStopped())
                toRemoveAsync.add(entry.getKey());
        for(Entry<String, SyncConnectorA> entry : syncConnectorInstancesList.entrySet())
            if(entry.getValue().isStopped())
                toRemoveSync.add(entry.getKey());
        for(String key : toRemoveAsync)
            asyncConnectorInstancesList.remove(key);
        for(String key : toRemoveSync)
            syncConnectorInstancesList.remove(key);
    }
    
    private void updateConnectorsDescription(){
        connectorsDescriptionJson = connectorsDescriptionBuilder.build();
        JsonObjectBuilder newBuilder = Json.createObjectBuilder();
        for(Entry<String, JsonValue> entry : connectorsDescriptionJson.entrySet())
            newBuilder.add(entry.getKey(), entry.getValue());
        connectorsDescriptionBuilder = newBuilder;
    }
    
    /*
    public static void main(String[] argv) {
        try {
            ConnectorsController cc = new ConnectorsController();
            System.out.println("All available Connectors: " + cc.getConnectors().toString());
            
            //Asynchronous connector example
            String asyncConnectorInstanceId = cc.startConnectorInstance(DummyAsyncConnector.class.getName(), Json.createObjectBuilder()
                .add("Text", Json.createObjectBuilder().add("value", "AsyncText"))
                .build(), new AsyncResponseHandlerI() {
                @Override
                public void handler(JsonObject asyncResponse) throws Exception {
                    System.out.println("Received a message : " + asyncResponse.toString());
                }
            });
            Thread.sleep(25000);
            cc.stopConnectorInstance(asyncConnectorInstanceId);
            
            //Synchronous connector example
            String syncConnectorInstanceId = null;
            try {
                syncConnectorInstanceId = cc.startConnectorInstance(DummySyncConnector.class.getName(), Json.createObjectBuilder()
                    .add("BaseText", Json.createObjectBuilder().add("value", "Hi"))
                    .build()
                );
            }catch(Exception ex) {ex.printStackTrace();}
            try {
                System.out.println("DummySyncConnector OUTPUT: " + cc.callConnectorInstance(syncConnectorInstanceId, Json.createObjectBuilder()
                    .add("AppendText", Json.createObjectBuilder().add("value", " everyone"))
                    .build()
                ));
            }catch(Exception ex) {ex.printStackTrace();}
            
            System.out.println("DummySyncConnector STATUS: " + cc.checkConnectorInstanceStatus(syncConnectorInstanceId));
            cc.stopConnectorInstance(syncConnectorInstanceId);
            
        } catch(Throwable ex) {
            ex.printStackTrace();
        }
    }
    */
}
