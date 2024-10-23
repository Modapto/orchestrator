package org.adoxx.microservice.api.connectors.impl;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.script.ScriptEngine;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.cache.NullFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.res.ResourceFileProvider;
import org.apache.commons.vfs2.provider.url.UrlFileProvider;
import org.renjin.appengine.AppEngineLocalFilesSystemProvider;
import org.renjin.eval.Session;
import org.renjin.eval.SessionBuilder;
import org.renjin.eval.vfs.FastJarFileProvider;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.StringVector;

public class REngineConnector extends SyncConnectorA{
    
    @Override
    public String getName() {
        return "R Engine Renjin Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Execute a R Script using the R engine Renjin and return its output")
            .add("de", "Execute a R Script using the R engine Renjin and return its output")
            .build();
    }
    
    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder().build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("algorithm", Json.createObjectBuilder()
                .add("name", "R Algorithm")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The R script code to execute")
                    .add("de", "The R script code to execute"))
                .add("value", ""))
            .add("outputVariable", Json.createObjectBuilder()
                    .add("name", "Output Variable")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "The R script variable to return")
                        .add("de", "The R script variable to return"))
                    .add("value", ""))
            .build();
    }

    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  resultList : [...]," + "\n"
                + "  print : '...'" + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                ;
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        
    }

    @Override
    public JsonObject performCall(JsonObject callConfiguration) throws Exception {
        String algorithm = callConfiguration.getJsonObject("algorithm")==null?"":callConfiguration.getJsonObject("algorithm").getString("value", "");
        if(algorithm.isEmpty()) throw new Exception("algorithm not provided");
        String outputVariable = callConfiguration.getJsonObject("outputVariable")==null?"":callConfiguration.getJsonObject("outputVariable").getString("value", "");
        if(outputVariable.isEmpty()) throw new Exception("outputVariable not provided");

        algorithm += "\ntoJSON("+outputVariable+");";
        Object[] ret = rscriptSafeEval(algorithm, null, false, 10); //FIXME: SecurityManager (true) not working
        String jsonArrayRet = ((StringVector)ret[0]).asString();
        String printRet = (String) ret[1];
        return Json.createObjectBuilder()
            .add("resultList", Json.createReader(new StringReader(jsonArrayRet)).readArray())
            .add("print", printRet)
            .add("moreInfo", Json.createObjectBuilder().add("retrievalTime", Utils.getCurrentTime()))
        .build();
    }

    @Override
    public void stop() throws Exception {
        
    }
    
    private static Object[] rscriptSafeEval(String algorithm, HashMap<String, Object> parameters, boolean enableSecurityManager, int maxAllowedExecTimeInSeconds) throws Exception {
        /*
         * material:
         * https://github.com/joshkatz/r-script
         * http://docs.renjin.org/en/latest/library/moving-data-between-java-and-r-code.html
         * https://www.renjin.org/index.html
         * https://rforge.net/rJava/
         * https://www.rforge.net/rscript/
         * https://stackoverflow.com/questions/8844451/calling-r-script-from-java
         * http://svn.rforge.net/org/trunk/rosuda/REngine/Rserve/test/StartRserve.java
         * https://github.com/s-u/REngine
         * 
         * https://mran.microsoft.com/
         * https://github.com/oracle/fastr
         * 
         * https://github.com/Rapporter/sandboxR
         * */
        
        // FIXME: SecurityManager did not work with renjin

        System.setProperty("java.net.useSystemProxies", "true");
        String[] blockedFunctionsList = new String[] { //only from package:base
            "system", "system2", "Sys.which", "setwd", "getwd", "Sys.getenv", "sessionInfo", "Sys.info", "Sys.getpid", "Sys.setenv", "find.package", "getCallingDLL", "system.file", "Sys.glob"
        };
        DefaultFileSystemManager dfsm = new DefaultFileSystemManager();
        dfsm.addProvider("jar", new FastJarFileProvider());
        // https://github.com/bedatadriven/renjin/blob/master/appengine/src/main/java/org/renjin/appengine/AppEngineContextFactory.java
        dfsm.addProvider("file", new AppEngineLocalFilesSystemProvider(new File(""))); //Restricted access to file system
        //dfsm.addProvider("file", new DefaultLocalFileProvider()); // Full access to file system
        dfsm.addProvider("res", new ResourceFileProvider());
        dfsm.addExtensionMap("jar", "jar");
        dfsm.setDefaultProvider(new UrlFileProvider());
        dfsm.setFilesCache(new NullFilesCache());
        dfsm.setCacheStrategy(CacheStrategy.ON_RESOLVE);
        dfsm.setBaseFile(new File("/"));
        dfsm.init();
        Session session = new SessionBuilder()
            .withDefaultPackages()
            .setFileSystemManager(dfsm)
            .build();
        
        Policy originalPolicy = null;
        if(enableSecurityManager) {
            ProtectionDomain currentProtectionDomain = Utils.class.getProtectionDomain();
            originalPolicy = Policy.getPolicy();
            final Policy orinalPolicyFinal = originalPolicy;
            Policy.setPolicy(new Policy() {
                @Override
                public boolean implies(ProtectionDomain domain, Permission permission) {
                    if(domain.equals(currentProtectionDomain))
                        return true;
                    return orinalPolicyFinal.implies(domain, permission);
                }
            });
        }
        
        StringWriter outputWriter = new StringWriter();
        
        try {
            SecurityManager originalSecurityManager = null;
            if(enableSecurityManager) {
                originalSecurityManager = System.getSecurityManager();
                System.setSecurityManager(new SecurityManager() {
                    //allow only the opening of a socket connection
                    @Override
                    public void checkConnect(String host, int port, Object context) {}
                    @Override
                    public void checkConnect(String host, int port) {}
                });
            }
            
            try {
                final ScriptEngine engine = new RenjinScriptEngineFactory().getScriptEngine(session);
                
                if(parameters != null)
                    for(Entry<String, Object> entry : parameters.entrySet())
                        engine.put(entry.getKey(), entry.getValue());
                
                engine.eval("library(jsonlite);");
                
                for(String blockedFunction: blockedFunctionsList) {
                    //engine.eval(blockedFunction + " <- function(...){stop('The Function " + blockedFunction + " is forbidden!');};"); //VULNERABLE: rm(system) : https://stackoverflow.com/questions/51266225/can-i-disable-the-system-command-in-r
                    engine.eval("assign('" + blockedFunction + "', function(...){stop('The function " + blockedFunction + " is forbidden!')}, envir=as.environment('package:base'));"); // https://stackoverflow.com/questions/44841808/stop-r-executing-system-or-shell-commands
                }
                
                engine.getContext().setWriter(outputWriter);
                
                class ScriptMonitor{
                    public Object scriptResult = null;
                    public Throwable  lastException = null;
                    private boolean stop = false;
                    Object lock = new Object();
                    @SuppressWarnings("deprecation")
                    public void startAndWait(Thread threadToMonitor, int secondsToWait) throws Exception {
                        threadToMonitor.start();
                        synchronized (lock) {
                            if(!stop) {
                                try {
                                    if(secondsToWait<1)
                                        lock.wait();
                                    else
                                        lock.wait(1000*secondsToWait);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        if(!stop) {
                            threadToMonitor.interrupt();
                            threadToMonitor.stop();
                            throw new Exception("R script forced to termination: Execution time bigger then " + secondsToWait + " seconds");
                        }
                        if(lastException != null)
                            throw new Exception("Error occurred in the R script execution: " + lastException.toString());
                    }
                    public void stop() {
                        synchronized (lock) {
                            stop = true;
                            lock.notifyAll();
                        }
                    }
                }
                final ScriptMonitor scriptMonitor = new ScriptMonitor();
                
                scriptMonitor.startAndWait(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            scriptMonitor.scriptResult = engine.eval(algorithm);
                        } catch (Throwable e) {
                            scriptMonitor.lastException = e;
                            //throw new RuntimeException(e);
                        } finally {
                            scriptMonitor.stop();
                        }
                    }
                }), maxAllowedExecTimeInSeconds);
                
                Object ret = scriptMonitor.scriptResult;
                return new Object[]{ret, outputWriter.toString()};
            } finally {
                if(enableSecurityManager)
                    System.setSecurityManager(originalSecurityManager);
            }
        } finally {
            if(enableSecurityManager)
                Policy.setPolicy(originalPolicy);
            dfsm.close();
        }
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        REngineConnector connector = new REngineConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("algorithm", Json.createObjectBuilder().add("value", "text <- \"Hello World!\"; print(text);"))
                //.add("algorithm", Json.createObjectBuilder().add("value", "system(\"C:\\\\Windows\\\\System32\\\\notepad.exe\");"))
                //.add("algorithm", Json.createObjectBuilder().add("value", "lapply(as.list(get('.BaseNamespaceEnv'))[293], function(x) {print(get('x')('C:\\\\Windows\\\\System32\\\\notepad.exe'))});"))                   
                .add("outputVariable", Json.createObjectBuilder().add("value", "text"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
