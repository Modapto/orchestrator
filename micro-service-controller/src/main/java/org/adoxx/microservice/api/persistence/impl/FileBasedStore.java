package org.adoxx.microservice.api.persistence.impl;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.persistence.PersistenceI;
import org.adoxx.microservice.utils.Utils;

public class FileBasedStore implements PersistenceI {
    
    private String baseFolder = null;
    private String backupFolder = null;
    
    public FileBasedStore(String baseFolder) {
        if(baseFolder==null || baseFolder.isEmpty())
            baseFolder = System.getProperty("java.io.tmpdir")+"/ADOxx_micro_configs/";
        
        if(!baseFolder.endsWith("/") && !baseFolder.endsWith("\\"))
            baseFolder += "/";
        if(!new File(baseFolder).exists())
            new File(baseFolder).mkdirs();
        this.baseFolder = baseFolder;
        
        String backupFolder = baseFolder+"backup/";
        if(!new File(backupFolder).exists())
            new File(backupFolder).mkdirs();
        this.backupFolder = backupFolder;
    }
    
    public FileBasedStore() {
        this( System.getProperty("java.io.tmpdir")+"/ADOxx_micro_configs/");
    }
    
    @Override
    public String saveMicroserviceConfiguration(JsonObject microserviceConfiguration) throws Exception {
        String id = UUID.randomUUID().toString();
        Utils.writeFile(microserviceConfiguration.toString().getBytes("UTF-8"), baseFolder+id+".json", false);
        return id;
    }

    @Override
    public void updateMicroserviceConfiguration(String microserviceId, JsonObject microserviceConfiguration) throws Exception {
        Utils.writeFile(microserviceConfiguration.toString().getBytes("UTF-8"), baseFolder+microserviceId+".json", false);
    }
    
    @Override
    public void deleteMicroserviceConfiguration(String microserviceId) throws Exception {
        File configFile = new File(baseFolder+microserviceId+".json");
        File backupFile = new File(backupFolder+microserviceId+".json");
        if(!configFile.exists())
            throw new Exception("Impossible to find the configuration for the microservice Id " + microserviceId);
        Utils.writeFile(Utils.readFile(configFile), backupFile, false);
        if(!configFile.delete())
            throw new Exception("Impossible to eliminate the configuration for the microservice Id " + microserviceId);
    }
    
    @Override
    public List<String> retrieveAllMicroservicesId() throws Exception {
        List<String> ret = new ArrayList<String>();
        File[] fileList = new File(baseFolder).listFiles();
        for(File file : fileList) {
            String fileName = file.getName();
            if(file.isFile() && fileName.toLowerCase().endsWith(".json"))
                ret.add(fileName.substring(0, fileName.length()-5));
        }
        return ret;
    }
    
    @Override
    public JsonObject retrieveMicroserviceConfiguration(String microserviceId) throws Exception {
        if(!new File(baseFolder+microserviceId+".json").exists())
            throw new Exception("Impossible to retrive the configuration for the service " + microserviceId);
        String configS = new String(Utils.readFile(baseFolder+microserviceId+".json"), "UTF-8");
        JsonObject config = Json.createReader(new StringReader(configS)).readObject();
        return config;
    }
    
    @Override
    public boolean existMicroserviceConfiguration(String microserviceId) throws Exception {
        return new File(baseFolder+microserviceId+".json").exists();
    }
    
    /*
    public static void main(String[] argv){
        try {
            FileBasedStore fileBasedStore = new FileBasedStore("/D:/ADOXX.ORG/MY_MICROSERVICE_FRAMEWORK/microservices-collection/");
            fileBasedStore.updateMicroserviceConfiguration("test", Json.createObjectBuilder().build());
            fileBasedStore.deleteMicroserviceConfiguration("test");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    */
}
