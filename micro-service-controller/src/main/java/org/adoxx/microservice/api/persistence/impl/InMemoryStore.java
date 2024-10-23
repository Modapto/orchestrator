package org.adoxx.microservice.api.persistence.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.adoxx.microservice.api.persistence.PersistenceI;

public class InMemoryStore implements PersistenceI {

    HashMap<String, JsonObject> memory = new HashMap<String, JsonObject>();
    
    @Override
    public String saveMicroserviceConfiguration(JsonObject microserviceConfiguration) throws Exception {
        String microserviceId = UUID.randomUUID().toString();
        memory.put(microserviceId, microserviceConfiguration);
        return microserviceId;
    }

    @Override
    public void updateMicroserviceConfiguration(String microserviceId, JsonObject microserviceConfiguration) throws Exception {
        memory.put(microserviceId, microserviceConfiguration);
    }

    @Override
    public void deleteMicroserviceConfiguration(String microserviceId) throws Exception {
        memory.remove(microserviceId);
    }

    @Override
    public List<String> retrieveAllMicroservicesId() throws Exception {
        return new ArrayList<String>(memory.keySet());
    }

    @Override
    public JsonObject retrieveMicroserviceConfiguration(String microserviceId) throws Exception {
        return memory.get(microserviceId);
    }

    @Override
    public boolean existMicroserviceConfiguration(String microserviceId) throws Exception {
        return memory.containsKey(microserviceId);
    }

}
