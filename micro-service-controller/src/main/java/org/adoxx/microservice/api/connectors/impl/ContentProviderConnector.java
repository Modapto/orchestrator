package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class ContentProviderConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "Content Provider Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Return a user defined content")
            .add("de", "Return a user defined content")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder().build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("content", Json.createObjectBuilder()
                .add("name", "Content")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The user defined content to return (optional)")
                    .add("de", "The user defined content to return (optional)"))
                .add("value", ""))
            .add("fileId", Json.createObjectBuilder()
                .add("name", "File ID")
                .add("description", Json.createObjectBuilder()
                    .add("en", "ID of the uploaded file where to extract the content to return (optional)")
                    .add("de", "ID of the uploaded file where to extract the content to return (optional)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("requireUpload", true)))
            .add("contentType", Json.createObjectBuilder()
                .add("name", "Content Type")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The type of the content to return (Text|JSON|List|Other)")
                    .add("de", "The type of the content to return (Text|JSON|List|Other)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder()
                        .add("Text")
                        .add("JSON")
                        .add("List")
                        .add("Other")  
                    )
                )
            )
            .add("contentMIME", Json.createObjectBuilder()
                .add("name", "Content MIME Type")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The MIME type of the content to return (look here for a full list: http://www.iana.org/assignments/media-types/media-types.xhtml)")
                    .add("de", "The MIME type of the content to return (look here for a full list: http://www.iana.org/assignments/media-types/media-types.xhtml)"))
                .add("value", "")
            )
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  dataMIME : 'text/*' / 'application/json' / 'all the other cases'," + "\n"
                + "  dataList / dataText / dataJson / dataBase64 : [] / '_PlainText_' / {_JsonObject_} / '_ContentBase64_'," + "\n"
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
    public JsonObject performCall(JsonObject configuration) throws Exception {
        
        String content = configuration.getJsonObject("content")==null?"":configuration.getJsonObject("content").getString("value", "");
        String fileId = configuration.getJsonObject("fileId")==null?"":configuration.getJsonObject("fileId").getString("value", "");
        if(content.isEmpty() && fileId.isEmpty())
            throw new Exception("content and fileId cannot be both empties");
        String contentType = configuration.getJsonObject("contentType")==null?"":configuration.getJsonObject("contentType").getString("value", "");
        String contentMIME = configuration.getJsonObject("contentMIME")==null?"":configuration.getJsonObject("contentMIME").getString("value", "");
        
        byte[] data = null;
        List<String> fileList = null;
        if(contentType.equals("List")) {
            fileList = Utils.listLocalFiles(fileId);
        } else {
            data = fileId.isEmpty()?content.getBytes("UTF-8"):fileId.startsWith("http")?Utils.sendHTTP(fileId, "GET", null, null, true, true).data:Utils.downloadLocalFile(fileId);
        }
        
        if(contentType.equals("List")) {
            return Json.createObjectBuilder()
                .add("dataMIME", "application/json")
                .add("dataList", Json.createArrayBuilder(fileList))
                .add("moreInfo", Json.createObjectBuilder()
                    .add("retrievalTime", Utils.getCurrentTime())
                ).build();
        } else if(contentType.equals("Text")) {
            return Json.createObjectBuilder()
                .add("dataMIME", contentMIME.isEmpty()?"text/*":contentMIME)
                .add("dataText", new String(data, "UTF-8"))
                .add("moreInfo", Json.createObjectBuilder()
                    .add("retrievalTime", Utils.getCurrentTime())
                ).build();
        } else if(contentType.equals("JSON")) {
            JsonValue dataJson = null;
            try{
                dataJson = Json.createReader(new StringReader(new String(data, "UTF-8"))).readValue();
            }catch(Exception e) {
                throw new Exception("Impossible to parse the data as JSON: "+e.getMessage());
            }
            return Json.createObjectBuilder()
                .add("dataMIME", "application/json")
                .add("dataJson", dataJson)
                .add("moreInfo", Json.createObjectBuilder()
                    .add("retrievalTime", Utils.getCurrentTime())
                ).build();
        } else {
            return Json.createObjectBuilder()
                .add("dataMIME", contentMIME.isEmpty()?"application/*":contentMIME)
                .add("dataBase64", Utils.base64Encode(data))
                .add("moreInfo", Json.createObjectBuilder()
                    .add("retrievalTime", Utils.getCurrentTime())
                ).build();
        }
    }
    
    @Override
    public void stop() throws Exception {
    }
    
    /*
    public static void main(String[] argv) throws Exception {
        ContentProviderConnector connector = new ContentProviderConnector();
        try{
            Utils.uploadFolder = "D:\\ADOXX.ORG\\MY_MICROSERVICE_FRAMEWORK\\microservices-upload\\";
            connector.threadStart(Json.createObjectBuilder().build());
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                //.add("content", Json.createObjectBuilder().add("value", "{\"test\":true}"))
                .add("fileId", Json.createObjectBuilder().add("value", "testFolder/_0e2c266f-7458-43db-9544-d69ea021ccbd/text.txt"))
                .add("contentType", Json.createObjectBuilder().add("value", "Text"))
                .add("contentMIME", Json.createObjectBuilder().add("value", "text/plain"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
