package org.adoxx.microservice.api.persistence;

import java.util.List;

import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.ConnectorsController;
import org.adoxx.microservice.api.persistence.impl.FileBasedStore;
import org.adoxx.microservice.api.persistence.impl.InMemoryStore;

public class PersistenceManager implements PersistenceI {
    
    private static PersistenceManager uniquePersistenceManager = null;
    
    static {
        uniquePersistenceManager = new PersistenceManager();
        uniquePersistenceManager.setFileBasedStoreProvider();
    }
    
    public static PersistenceManager unique() {
        return uniquePersistenceManager;
    }
    
    private PersistenceI persistenceProvider = null;
    
    public void setProvider(PersistenceI provider) {
        persistenceProvider = provider;
    }
    
    public void setFileBasedStoreProvider(String baseFolder) {
        persistenceProvider = new FileBasedStore(baseFolder);
    }
    
    public void setFileBasedStoreProvider() {
        persistenceProvider = new FileBasedStore();
    }
    
    public void setInMemoryStoreProvider() {
        persistenceProvider = new InMemoryStore();
    }

    @Override
    public String saveMicroserviceConfiguration(JsonObject microserviceConfiguration) throws Exception {
        if(persistenceProvider == null) throw new Exception("A provider must first be specified");
        checkMicroserviceConfigurationFormat(microserviceConfiguration);
        return persistenceProvider.saveMicroserviceConfiguration(microserviceConfiguration);
    }

    @Override
    public void updateMicroserviceConfiguration(String microserviceId, JsonObject microserviceConfiguration) throws Exception {
        if(persistenceProvider == null) throw new Exception("A provider must first be specified");
        checkMicroserviceConfigurationFormat(microserviceConfiguration);
        persistenceProvider.updateMicroserviceConfiguration(microserviceId, microserviceConfiguration);
    }
    
    @Override
    public void deleteMicroserviceConfiguration(String microserviceId) throws Exception {
        if(persistenceProvider == null) throw new Exception("A provider must first be specified");
        persistenceProvider.deleteMicroserviceConfiguration(microserviceId);
    }

    @Override
    public List<String> retrieveAllMicroservicesId() throws Exception {
        if(persistenceProvider == null) throw new Exception("A provider must first be specified");
        return persistenceProvider.retrieveAllMicroservicesId();
    }

    @Override
    public JsonObject retrieveMicroserviceConfiguration(String microserviceId) throws Exception {
        if(persistenceProvider == null) throw new Exception("A provider must first be specified");
        JsonObject serviceConfiguration = persistenceProvider.retrieveMicroserviceConfiguration(microserviceId);
        checkMicroserviceConfigurationFormat(serviceConfiguration);
        return serviceConfiguration;
    }
    
    @Override
    public boolean existMicroserviceConfiguration(String microserviceId) throws Exception {
        if(persistenceProvider == null) throw new Exception("A provider must first be specified");
        return persistenceProvider.existMicroserviceConfiguration(microserviceId);
    }
    
    private void checkMicroserviceConfigurationFormat(JsonObject microserviceConfiguration) throws Exception {
        JsonObject modulesInfo = ConnectorsController.unique().getConnectors();
        
        if(microserviceConfiguration == null) throw new Exception("microserviceConfiguration can not be null");
        if(microserviceConfiguration.getString("name", "").isEmpty()) throw new Exception("name must be present");
        if(microserviceConfiguration.getString("description", "").isEmpty()) throw new Exception("description must be present");
        if(!microserviceConfiguration.containsKey("public")) throw new Exception("public must be present");
        if(microserviceConfiguration.getString("defaultOperationId", "").isEmpty()) throw new Exception("defaultOperationId must be present");
        JsonObject operations = microserviceConfiguration.getJsonObject("operations");
        if(operations == null) throw new Exception("operations JSON object must be present");
        for(String operationKey : operations.keySet()) {
            JsonObject operation = operations.getJsonObject(operationKey);
            if(operation == null) throw new Exception("operation "+operationKey+" must be a valid JSON object");
            if(operation.getString("name", "").isEmpty()) throw new Exception("operation "+operationKey+" name must be present");
            if(operation.getString("description", "").isEmpty()) throw new Exception("operation "+operationKey+" description must be present");
            if(!operation.containsKey("autostart")) throw new Exception("operation "+operationKey+" autostart must be present");
            JsonObject configuration = operation.getJsonObject("configuration");
            if(configuration == null) throw new Exception("operation "+operationKey+" configuration must be a valid JSON object");
            
            String connectorId = configuration.getString("connectorId", "");
            if(connectorId.isEmpty()) throw new Exception("operation "+operationKey+" configuration connectorId must be present");
            JsonObject connectorInfos = modulesInfo.getJsonObject(connectorId);
            if(connectorInfos == null) throw new Exception("operation "+operationKey+" configuration connectorId is not valid");
            if(configuration.getJsonObject("configStart") == null) throw new Exception("operation "+operationKey+" configuration configStart must be a valid JSON object");
            if(configuration.getJsonObject("configCall") == null) throw new Exception("operation "+operationKey+" configuration configCall must be a valid JSON object");
            JsonObject inputs = configuration.getJsonObject("inputs");
            if(inputs == null) throw new Exception("operation "+operationKey+" configuration inputs must be a valid JSON object");
            for(String inputKey : inputs.keySet()) {
                JsonObject input = inputs.getJsonObject(inputKey);
                if(input == null) throw new Exception("operation "+operationKey+" configuration input "+inputKey+" must be a valid JSON object");
                if(input.getString("matchingName", "").isEmpty()) throw new Exception("operation "+operationKey+" configuration input "+inputKey+" matchingName must be present");
                if(input.getString("description", null) == null) throw new Exception("operation "+operationKey+" configuration input "+inputKey+" description must be present");
                if(input.getString("workingExample", null) == null) throw new Exception("operation "+operationKey+" configuration input "+inputKey+" workingExample must be present");
            }
            JsonObject inputsAsync = configuration.getJsonObject("inputsAsync");
            if(connectorInfos.getBoolean("asyncConnectionRequired")) {
                if(inputsAsync == null) throw new Exception("operation "+operationKey+" configuration inputsAsync is required and must be a valid JSON object");
                if(inputsAsync.getString("responseServiceId", null) == null) throw new Exception("operation "+operationKey+" configuration inputsAsync responseServiceId must be present");
                if(inputsAsync.getString("responseServiceOperationId", null) == null) throw new Exception("operation "+operationKey+" configuration inputsAsync responseServiceOperationId must be present");
                if(inputsAsync.getString("responseServiceInputId", null) == null) throw new Exception("operation "+operationKey+" configuration inputsAsync responseServiceInputId must be present");
                JsonObject responseServiceOtherInputs = inputsAsync.getJsonObject("responseServiceOtherInputs");
                if(responseServiceOtherInputs == null) throw new Exception("operation "+operationKey+" configuration inputsAsync responseServiceOtherInputs must be a valid JSON object");
                for(String responseServiceOtherInputsKey : responseServiceOtherInputs.keySet()) {
                    JsonObject responseServiceOtherInput = responseServiceOtherInputs.getJsonObject(responseServiceOtherInputsKey);
                    if(responseServiceOtherInput == null) throw new Exception("operation "+operationKey+" configuration inputsAsync responseServiceOtherInputs "+responseServiceOtherInputsKey+" must be a valid JSON object");
                    if(responseServiceOtherInput.getString("value", null) == null) throw new Exception("operation "+operationKey+" configuration inputsAsync responseServiceOtherInputs "+responseServiceOtherInputsKey+" value must be present");
                    responseServiceOtherInput.getString("value");
                }
                if(inputsAsync.getString("inputAdaptationAlgorithm", null) == null) throw new Exception("operation "+operationKey+" configuration inputsAsync inputAdaptationAlgorithm must be present");
            } else {
                if(inputsAsync != null) throw new Exception("operation "+operationKey+" configuration inputsAsync is not required and must not be present");
            }
            
            if(configuration.getString("outputDescription", null) == null) throw new Exception("operation "+operationKey+" configuration outputDescription must be present");
            if(configuration.getString("outputAdaptationAlgorithm", null) == null) throw new Exception("operation "+operationKey+" configuration outputAdaptationAlgorithm must be present");
            if(configuration.getString("statusCheckAlgorithm", null) == null) throw new Exception("operation "+operationKey+" configuration statusCheckAlgorithm must be present");
        }
        
        if(microserviceConfiguration.getJsonObject("moreInfos") == null) throw new Exception("moreInfos object must be present");
    }
}
