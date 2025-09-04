package org.adoxx.microservice.api.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.adoxx.microservice.api.MicroserviceController;
import org.adoxx.microservice.api.connectors.impl.CommandLineConnector;
import org.adoxx.microservice.api.log.LogManager;
import org.adoxx.microservice.api.log.impl.FileBasedLogSaver;
import org.adoxx.microservice.api.persistence.impl.FileBasedStore;
import org.adoxx.microservice.utils.Utils;

@WebListener
public class RESTContextListener implements ServletContextListener {
    
    public static String keycloakUrl = "";
    public static String keycloakRealm = "";
    public static String keycloakClient = "";
    public static String keycloakSecret = "";

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        try {
            String workingFolder = getWorkingFolder();
            String envConfig = System.getenv("MSC_CONFIG")==null?"":System.getenv("MSC_CONFIG");
            JsonObject config = Json.createReader(new InputStreamReader(!envConfig.isEmpty()?new FileInputStream(envConfig):RESTContextListener.class.getResourceAsStream("config.json"))).readObject();
            String microservicesDefinitionFolder = config.getString("microservicesDefinitionFolder", "");
            if(!microservicesDefinitionFolder.isEmpty()) {
                if(!microservicesDefinitionFolder.startsWith("/")) 
                    microservicesDefinitionFolder = workingFolder + microservicesDefinitionFolder;
                MicroserviceController.unique().setPersistenceHandler(new FileBasedStore(microservicesDefinitionFolder));
            }
            
            String uploadFolder = config.getString("uploadFolder", "");
            if(!uploadFolder.isEmpty()) {
                if(!uploadFolder.startsWith("/")) 
                    uploadFolder = workingFolder + uploadFolder;
                Utils.uploadFolder = uploadFolder;
            }
            
            String logFileName = config.getString("logFileName", "");
            if(!logFileName.isEmpty()) {
                if(!logFileName.startsWith("/")) 
                    logFileName = workingFolder + logFileName;
                LogManager.unique().setLogHandler(new FileBasedLogSaver(logFileName));
            }
            
            if(config.getBoolean("autostartEnabled", false))
                MicroserviceController.unique().initAutostart();
            
            Utils.maxJSExecTimeInMinutes = config.getInt("maxJSExecTimeInMinutes", 5);

            String commandLineExecPath = config.getString("commandLineExecPath", "");
            if(!commandLineExecPath.isEmpty()) {
                if(!commandLineExecPath.startsWith("/")) 
                    commandLineExecPath = workingFolder + commandLineExecPath;
                CommandLineConnector.commandLineExecPath = commandLineExecPath;
            }
            CommandLineConnector.commandLineMaxExecTimeInMinutes = config.getInt("commandLineMaxExecTimeInMinutes", 5);

            String keycloakUrlEnv = config.getString("keycloakUrlEnv", "OSC_KEYCLOAK_URL");
            String keycloakRealsEnv = config.getString("keycloakRealmEnv", "OSC_KEYCLOAK_REALM");
            String keycloakClientEnv = config.getString("keycloakClientEnv", "OSC_KEYCLOAK_CLIENT");
            String keycloakSecretEnv = config.getString("keycloakSecretEnv", "OSC_KEYCLOAK_SECRET");
            if(!keycloakUrlEnv.isEmpty() && !keycloakRealsEnv.isEmpty() && !keycloakClientEnv.isEmpty() && !keycloakSecretEnv.isEmpty()) {
                RESTContextListener.keycloakUrl = System.getenv(keycloakUrlEnv)==null?"":System.getenv(keycloakUrlEnv);
                RESTContextListener.keycloakRealm = System.getenv(keycloakRealsEnv)==null?"":System.getenv(keycloakRealsEnv);
                RESTContextListener.keycloakClient = System.getenv(keycloakClientEnv)==null?"":System.getenv(keycloakClientEnv);
                RESTContextListener.keycloakSecret = System.getenv(keycloakSecretEnv)==null?"":System.getenv(keycloakSecretEnv);
            }
            
            System.out.println("MICROSERVICE CONFIGURATION DEFINITION FOLDER: " + microservicesDefinitionFolder);
            System.out.println("MICROSERVICE CONFIGURATION UPLOADS FOLDER: " + uploadFolder);
            System.out.println("MICROSERVICE CONFIGURATION LOG FILE: " + logFileName);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String getWorkingFolder() {
        try {
            String baseFolder = RESTService.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            if(new File(baseFolder).isDirectory())
                baseFolder = baseFolder.substring(0, baseFolder.length()-1);
            baseFolder = baseFolder.substring(0, baseFolder.lastIndexOf("/")+1);
            //baseFolder example here : /D:/TOOLS/apache-tomcat-9.0.0.M22/wtpwebapps/micro-service-controller-rest/WEB-INF/
            if(!new File(baseFolder).canWrite())
                baseFolder = System.getProperty("java.io.tmpdir") + "/";
            return baseFolder;
        }catch(Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
