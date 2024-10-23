package org.adoxx.microservice.api.connectors;

import java.lang.Thread.State;

import javax.json.JsonObject;

import org.adoxx.microservice.api.log.LogI.LogLevel;
import org.adoxx.microservice.api.log.LogManager;

/**
 * <h1>SyncConnectorA</h1>
 * This Abstract Class define a synchronous connector.<br>
 * A synchronous connector must be defined for all this services that provide an answer only on requests, so services that are not proactive.<br>
 * 
 * @author Damiano Falcioni
 */
public abstract class SyncConnectorA extends BasicConnectorA {
    
    /**
     * Must return the template for the configuration JSON required by {@link #start(JsonObject)}<br>
     * @return JsonObject The configuration template in the following format:
     * <pre>
     * {
     *      "unique name of one property " : {
     *          "name" : "A name identifier for this property",
     *          "description" : {
     *              "landuage identifiery ex: en/it/.." : "property description in the specific language",
     *              "another language identifiery" : "property description in the specific language",
     *              ...
     *          },
     *          "value" : "" //empty field that must be filled only when the configuration for {@link #start(JsonObject)} is created
     *      },
     *      "unique name of another property" : {...},
     *      ...
     * }
     * </pre>
     * This is the standard template for a configuration but any JSON can be created, depending on the needs of {@link #start(JsonObject)}
     */
    public abstract JsonObject getStartConfigurationTemplate();
    
    /**
     * Call {@link #getStartConfigurationTemplate()}, check if the response is in the valid format and return it.<br>
     * @return JsonObject A JSON object in the format specified in {@link #getStartConfigurationTemplate()}
     */
    public JsonObject getStartConfigurationTemplateSafe() throws Exception {
        JsonObject ret = getStartConfigurationTemplate();
        checkConfigurationTemplateFormat(ret);
        return ret;
    }
    
    /**
     * Must perform all the initializations operations required for the connector.<br>
     * The method can be blocking. In such a case only before the blocking happen it must call the {@link #setPreStarted()} indicating that the starting will be completed on the next blocking operation.<br>
     * @param startConfiguration The configuration JSON as returned by {@link #getStartConfigurationTemplate()}, but filled with values
     */
    public abstract void start(JsonObject startConfiguration) throws Exception;
    
    /**
     * Must perform all the finalization operations required for the connector.<br>
     * The method can not be blocking.<br>
     */
    public abstract void stop() throws Exception;
    
    private Thread thread = null;
    Throwable returnedError = null;
    private ConnectorStatus connectorThreadStatus = ConnectorStatus.STOPPED;
    private boolean stop = false;
    private boolean isStarted = false;
    private boolean isStopped = false;
    private boolean isPreStarted = false;
    private final Object stop_lock = new Object();
    private final Object isStarted_lock = new Object();
    private final Object isStopped_lock = new Object();
    private final Object preStarted_lock = new Object();
    
    public enum ConnectorStatus {
        STARTED,
        STOPPED
    }
    
    /**
     * Create a new Thread that perform the {@link #start(JsonObject)} and wait for the {@link #threadStop()} to be executed<br>
     * If a Thread already exist the request will be skipped and nothing is done.<br> 
     * @param startConfiguration The configuration JSON as returned by {@link #getStartConfigurationTemplate()}, but filled with values
     */
    public void threadStart(JsonObject startConfiguration) {
        
        if(thread != null) return;
        
        thread = new Thread(new Runnable() {
            public void run() {
                isStarted = false;
                isStopped = false;
                isPreStarted = false;
                stop = false;
                
                try {
                    start(startConfiguration);
                } catch(Throwable t) {
                    returnedError = t;
                    LogManager.unique().log(LogLevel.ERROR, "Exception in the Thread started for the Connector '" + getName() + "' with starting configuration: " + startConfiguration.toString(), t);
                    notifyStop();
                }
                setPreStarted(); //in case is not called inside the start, I will do it
                
                waitThreadStart(); //here wait for the monitoring thread to complete
                waitStop();
                
                notifyStopped();
            }
        });
        
        Thread threadStartMonitor = new Thread(new Runnable() {
            public void run() {
                waitPreStarted();
                while(true) {
                    if(thread != null && (thread.getState().equals(State.BLOCKED) || thread.getState().equals(State.WAITING) || thread.getState().equals(State.TIMED_WAITING))) {
                        notifyStarted();
                        break;
                    }
                    if(thread == null) //not required but just in case
                        break;
                }
            }
        });
        
        thread.start();
        threadStartMonitor.start();
    }
    
    /**
     * Check if the connector is started and call {@link #performCall(JsonObject)} that check if the response is in the valid format and return it.<br>
     * @param callConfiguration The configuration JSON as returned by {@link #getCallConfigurationTemplate()}, but filled with values
     * @return JsonObject A JSON object in the format specified in {@link #performCall(JsonObject)}
     */
    public JsonObject performCallSafe(JsonObject callConfiguration) throws Exception{
        if(!isStarted) throw new Exception("The connector is not yet started");
        if(isStopped)
            if(getThreadLastError()==null)
                throw new Exception("Impossible to perform the call: the connector is stopped.");
            else
                throw new Exception("Impossible to perform the call: the connector is stopped due to an internal error. Last error occurred: " + getThreadLastError());
        return super.performCallSafe(callConfiguration);
    }
    
    /**
     * Call the {@link #stop()} and terminate the started Thread.<br>
     */
    public void threadStop() throws Exception {
        try {
            stop();
        } finally {
            notifyStop();
        }
    }
    
    /**
     * Block the current process untill the {@link #threadStart(JsonObject)} complete the initialization process.<br>
     */
    public void waitThreadStart(){
        synchronized (isStarted_lock) {
            while(!isStarted){
                try {
                    isStarted_lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
    
    /**
     * Block the current process untill the {@link #threadStop()} complete.<br>
     */
    public void waitThreadStop(){
        synchronized (isStopped_lock) {
            while(!isStopped){
                try {
                    isStopped_lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
    
    /**
     * Return the error generated by the started Thread.<br>
     * @return Exception The error generated in the Thread or null if the Thread did not rised an Exception
     */
    public Throwable getThreadLastError() {
        return returnedError;
    }
    
    /**
     * Return the Thread status of this connector.<br>
     * @return ConnectorStatus Thread status that can be STARTED or STOPPED
     */
    public ConnectorStatus getThreadStatus() {
        return connectorThreadStatus;
    }
    
    /**
     * Return true if the Thread completed all its starting procedures, false otherwise<br>
     * @return boolean true if the Thread completed all its starting procedures, false otherwise
     */
    public boolean isStarted(){
        synchronized (isStarted_lock) {
            return isStarted;
        }
    }
    
    /**
     * Return true if the Thread completed all its stopping procedures, false otherwise<br>
     * @return boolean true if the Thread completed all its stopping procedures, false otherwise
     */
    public boolean isStopped(){
        synchronized (isStopped_lock) {
            return isStopped;
        }
    }
    
    /**
     * Notify all the internal procedures that the starting procedures are completed and on the next blocking the Thread will be considered as started.<br>
     */
    protected void setPreStarted() {
        synchronized (preStarted_lock) {
            isPreStarted = true;
            preStarted_lock.notifyAll();
        }
    }
    
    
    
    private void notifyStopped() {
        synchronized (isStopped_lock) {
            thread = null;
            isStopped = true;            
            connectorThreadStatus = ConnectorStatus.STOPPED;
            isStopped_lock.notifyAll();
        }
    }
    
    private void notifyStarted() {
        synchronized (isStarted_lock) {
            isStarted = true;
            connectorThreadStatus = ConnectorStatus.STARTED;
            isStarted_lock.notifyAll();
        }
    }
    
    private void waitPreStarted() {
        synchronized (preStarted_lock) {
            while(!isPreStarted) {
                try {
                    preStarted_lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
    
    private void waitStop() {
        synchronized (stop_lock) {
            while(!stop){
                try {
                    stop_lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
    
    private void notifyStop() {
        synchronized (stop_lock) {
            stop = true;
            stop_lock.notifyAll();
        }
    }
}
