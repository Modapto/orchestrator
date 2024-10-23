package org.adoxx.microservice.api.persistence;

import java.util.List;

import javax.json.JsonObject;

public interface PersistenceI {
    public String saveMicroserviceConfiguration(JsonObject microserviceConfiguration) throws Exception;
    public void updateMicroserviceConfiguration(String microserviceId, JsonObject microserviceConfiguration) throws Exception;
    public void deleteMicroserviceConfiguration(String microserviceId) throws Exception;
    public List<String> retrieveAllMicroservicesId() throws Exception;
    public JsonObject retrieveMicroserviceConfiguration(String microserviceId) throws Exception;
    public boolean existMicroserviceConfiguration(String microserviceId) throws Exception;
}
