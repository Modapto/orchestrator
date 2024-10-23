package org.adoxx.microservice.api.connectors;

import javax.json.JsonObject;

/**
 * <h1>AsyncResponseHandlerI</h1>
 * This Interface is used to handle the asynchronous response of a connector.<br>
 * It is specified in the connector instanciation and is called by a connector extending the {@link AsyncConnectorA} abstract Class when an asyncronous response arrive.<br>
 * 
 * @author Damiano Falcioni
 */
public interface AsyncResponseHandlerI {
    
    /**
     * Define how an asynchronous response must be handled when received<br>
     * @param asyncResponse Is the response received by the service. It is JsonObject in one of the two formats returned by {@link SyncConnectorA#performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public void handler(JsonObject asyncResponse) throws Exception;
}
