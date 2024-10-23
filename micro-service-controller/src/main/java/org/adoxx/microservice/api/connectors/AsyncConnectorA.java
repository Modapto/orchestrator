package org.adoxx.microservice.api.connectors;

import javax.json.JsonObject;

import org.adoxx.microservice.api.log.LogManager;
import org.adoxx.microservice.api.log.LogI.LogLevel;

/**
 * <h1>AsyncConnectorA</h1>
 * This Abstract Class define an asynchronous connector.<br>
 * An asynchronous connector must be defined for all this services that are proactive and provide an answer without a specific request<br>
 * 
 * @author Damiano Falcioni
 */
public abstract class AsyncConnectorA extends SyncConnectorA {
    
    /**
     * Must be used to manage and internally set the handler for asynchronous responses.<br>
     * @param asyncResponseHandler The handler to set up
     */
    public abstract void setAsyncResponsesHandler(AsyncResponseHandlerI asyncResponseHandler);
    
    /**
     * Set as handler a wrapper that check if the handler input format is valid and call the original handler inside a Thread in order to not block the process.<br>
     * @param asyncResponseHandler The handler to wrap
     */
    public void setAsyncResponsesHandlerSafe(AsyncResponseHandlerI asyncResponseHandler) {
        setAsyncResponsesHandler(new AsyncResponseHandlerI() {
            @Override
            public void handler(JsonObject asyncResponse) throws Exception {
                //checkResponseStructure(asyncResponse);
                
                Thread asyncHandlerThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            asyncResponseHandler.handler(asyncResponse);
                        } catch(Throwable t) {
                            returnedError = t;
                            LogManager.unique().log(LogLevel.ERROR, "Exception in the Thread started for handling the asynchronous response of the Connector '" + getName() + "'", t);
                        }
                    }
                });
                
                asyncHandlerThread.start();
            }
        });
    }
}
