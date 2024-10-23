package org.adoxx.microservice.api.connectors.impl;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class ContentReceiverConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "Content Receiver Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Store a user defined content")
            .add("de", "Store a user defined content")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("rootFolderName", Json.createObjectBuilder()
                .add("name", "Root Folder Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The folder name to use to store the provided data")
                    .add("de", "The folder name to use to store the provided data"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("fileContent", Json.createObjectBuilder()
                .add("name", "File Content")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The user defined content to store in plain text format (optional)")
                    .add("de", "The user defined content to store in plain text format (optional)"))
                .add("value", ""))
            .add("fileContentB64", Json.createObjectBuilder()
                .add("name", "File Content Base64")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The user defined content to store in Base64 format (optional)")
                    .add("de", "The user defined content to store in Base64 format (optional)"))
                .add("value", ""))
            .add("fileContentRemote", Json.createObjectBuilder()
                .add("name", "File Content from URL")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The user defined content to retrive from the provided url (optional)")
                    .add("de", "The user defined content to retrive from the provided url (optional)"))
                .add("value", ""))
            .add("fileName", Json.createObjectBuilder()
                .add("name", "File Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The name of the file to create")
                    .add("de", "The name of the file to create"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
            + "{" + "\n"
            + "  fileId : ''" + "\n"
            + "}" + "\n";
    }

    String rootFolderName;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        rootFolderName = startConfiguration.getJsonObject("rootFolderName")==null?"":startConfiguration.getJsonObject("rootFolderName").getString("value", "");
        if(rootFolderName.endsWith("\\") || rootFolderName.endsWith("/") || rootFolderName.startsWith("\\") || rootFolderName.startsWith("/"))
            throw new Exception("rootFolderName can not start or end with \\ or /");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String fileContent = configuration.getJsonObject("fileContent")==null?"":configuration.getJsonObject("fileContent").getString("value", "");
        String fileContentB64 = configuration.getJsonObject("fileContentB64")==null?"":configuration.getJsonObject("fileContentB64").getString("value", "");
        String fileContentRemote = configuration.getJsonObject("fileContentRemote")==null?"":configuration.getJsonObject("fileContentRemote").getString("value", "");
        String fileName = configuration.getJsonObject("fileName")==null?"":configuration.getJsonObject("fileName").getString("value", "");
        if(fileContent.isEmpty() && fileContentB64.isEmpty() && !fileContentRemote.startsWith("http"))
            throw new Exception("fileContent, fileContentB64 and fileContentRemote cannot be all empties");
        
        byte[] data = null;
        if(!fileContent.isEmpty())
            data = fileContent.getBytes("UTF-8");
        else if(!fileContentB64.isEmpty()) {
            try {
                data = Utils.base64Decode(fileContentB64);
            }catch(Exception ex) { throw new Exception("The provided content is not a valid Base64 string", ex);}
        } else if(fileContentRemote.startsWith("http")) {
            try {
                data = Utils.sendHTTP(fileContentRemote, "GET", null, null, true, true).data;
            }catch(Exception ex) { throw new Exception("Error retriving the content from the provided URL: " + fileContentRemote, ex);}
        } else {
            throw new Exception("fileContent, fileContentB64 and fileContentRemote cannot be all empties");
        }

        String uploadedPath = Utils.uploadLocalFile(rootFolderName, fileName, data);
        
        return Json.createObjectBuilder()
            .add("fileId", uploadedPath)
            .build();
    }
    
    @Override
    public void stop() throws Exception {
        rootFolderName = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        ContentReceiverConnector connector = new ContentReceiverConnector();
        try{
            Utils.uploadFolder = "D:\\ADOXX.ORG\\MY_MICROSERVICE_FRAMEWORK\\microservices-upload\\";
            connector.threadStart(Json.createObjectBuilder()
                .add("rootFolderName", Json.createObjectBuilder().add("value", "testFolder"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("fileContent", Json.createObjectBuilder().add("value", "ciao"))
                .add("fileContentB64", Json.createObjectBuilder().add("value", ""))
                .add("fileContentRemote", Json.createObjectBuilder().add("value", ""))
                .add("fileName", Json.createObjectBuilder().add("value", "/text.txt"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
