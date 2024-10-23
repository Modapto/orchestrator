package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.adoxx.microservice.utils.Utils.HttpResults;

public class TwitterPOSTConnector extends SyncConnectorA {
    
    @Override
    public String getName() {
        return "Twitter Post Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Publish a Tweet in Twitter")
            .add("de", "Publish a Tweet in Twitter")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("oauth_consumer_key", Json.createObjectBuilder()
                .add("name", "oauth_consumer_key")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The oauth_consumer_key as provided by Twitter")
                    .add("de", "The oauth_consumer_key as provided by Twitter"))
                .add("value", ""))
            .add("oauth_consumer_secret", Json.createObjectBuilder()
                .add("name", "oauth_consumer_secret")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The oauth_consumer_secret as provided by Twitter")
                    .add("de", "The oauth_consumer_secret as provided by Twitter"))
                .add("value", ""))
            .add("oauth_token", Json.createObjectBuilder()
                .add("name", "oauth_token")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The oauth_token as provided by Twitter")
                    .add("de", "The oauth_token as provided by Twitter"))
                .add("value", ""))
            .add("oauth_token_secret", Json.createObjectBuilder()
                .add("name", "oauth_token_secret")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The oauth_token_secret as provided by Twitter")
                    .add("de", "The oauth_token_secret as provided by Twitter"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("text", Json.createObjectBuilder()
                .add("name", "Tweet")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The text to tweet")
                    .add("de", "The text to tweet"))
                .add("value", ""))
            .add("respondToId", Json.createObjectBuilder()
                .add("name", "respondToId")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Id of the post to respond (optional. Is the 'id_str' returned by the Twitter search API)")
                    .add("de", "Id of the post to respond (optional. Is the 'id_str' returned by the Twitter search API)"))
                .add("value", ""))
            .add("image", Json.createObjectBuilder()
                .add("name", "image")
                .add("description", Json.createObjectBuilder()
                    .add("en", "An image to attach to the tweet (optional)")
                    .add("de", "An image to attach to the tweet (optional)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("requireUpload", true)))
            .add("image2", Json.createObjectBuilder()
                .add("name", "image2")
                .add("description", Json.createObjectBuilder()
                    .add("en", "A second image to attach to the tweet (optional)")
                    .add("de", "A second image to attach to the tweet (optional)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("requireUpload", true)))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "Json as returned from the Twitter Statuses API";
    }
    
    private String oauth_consumer_key, oauth_consumer_secret, oauth_token, oauth_token_secret = null;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        oauth_consumer_key = startConfiguration.getJsonObject("oauth_consumer_key")==null?"":startConfiguration.getJsonObject("oauth_consumer_key").getString("value", "");
        if(oauth_consumer_key.isEmpty()) throw new Exception("oauth_consumer_key not provided");
        oauth_consumer_secret = startConfiguration.getJsonObject("oauth_consumer_secret")==null?"":startConfiguration.getJsonObject("oauth_consumer_secret").getString("value", "");
        if(oauth_consumer_secret.isEmpty()) throw new Exception("oauth_consumer_secret not provided");
        oauth_token = startConfiguration.getJsonObject("oauth_token")==null?"":startConfiguration.getJsonObject("oauth_token").getString("value", "");
        if(oauth_token.isEmpty()) throw new Exception("oauth_token not provided");
        oauth_token_secret = startConfiguration.getJsonObject("oauth_token_secret")==null?"":startConfiguration.getJsonObject("oauth_token_secret").getString("value", "");
        if(oauth_token_secret.isEmpty()) throw new Exception("oauth_token_secret not provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String text = configuration.getJsonObject("text")==null?"":configuration.getJsonObject("text").getString("value", "");
        if(text.isEmpty()) throw new Exception("text not provided");
        String respondToId = configuration.getJsonObject("respondToId")==null?"":configuration.getJsonObject("respondToId").getString("value", "");
        String image = configuration.getJsonObject("image")==null?"":configuration.getJsonObject("image").getString("value", "");
        String image2 = configuration.getJsonObject("image2")==null?"":configuration.getJsonObject("image2").getString("value", "");
        
        HashMap<String, String> parametersMap = new HashMap<String, String>();
        
        String httpMethod = "POST";
        String apiEndpoint = "https://api.twitter.com/1.1/statuses/update.json";
        parametersMap.put("status", text);
        
        String media_ids = "";
        if(!image.isEmpty()) {
            String mediaId = uploadImage(obtainBase64(image));
            media_ids += mediaId + ",";
        }
        if(!image2.isEmpty()) {
            String mediaId = uploadImage(obtainBase64(image2));
            media_ids += mediaId + ",";
        }
        if(!media_ids.isEmpty()) {
            media_ids = media_ids.substring(0, media_ids.length()-1);
            parametersMap.put("media_ids", media_ids);
        }
        
        if(!respondToId.isEmpty()) {
            parametersMap.put("in_reply_to_status_id", respondToId);
        }
        
        ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
        htmlHeaderList.add(new String[]{"Content-Type", "application/x-www-form-urlencoded"});
        htmlHeaderList.add(new String[]{"Authorization", Utils.createOAuth10HttpAuthorizationHeader(httpMethod, apiEndpoint, oauth_consumer_key, oauth_consumer_secret, oauth_token, oauth_token_secret, parametersMap)});
        String postData = "";
        for(Entry<String, String> parameterEntry : parametersMap.entrySet())
            postData += URLEncoder.encode(parameterEntry.getKey(), "UTF-8") + "=" + URLEncoder.encode(parameterEntry.getValue(), "UTF-8") + "&";
        if(!postData.isEmpty())
            postData = postData.substring(0, postData.length()-1);
        
        HttpResults out = Utils.sendHTTP(apiEndpoint, httpMethod, postData, htmlHeaderList, true, true);
        
        if(out.headerMap.get("content-type") == null)
            throw new Exception("Impossible to identify the content-type header");
        
        String returnedContentType = out.headerMap.get("content-type").get(0);
        if(!returnedContentType.startsWith("application/json"))
            throw new Exception("Unexpected Content-Type header: "+returnedContentType+"; Expected application/json");
        
        String returnedString = new String(out.data, "UTF-8");
        JsonObject returnedDataJson = Json.createReader(new StringReader(returnedString)).readObject();
        
        if(returnedDataJson.containsKey("errors"))
            throw new Exception("Error returned from Twitter service: " + returnedString);
        
        return returnedDataJson;
        /*
        return Json.createObjectBuilder()
            .add("dataFormat", "application/json")
            .add("data", "")
            .add("dataBase64", Utils.base64Encode(out.data))
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
        */
    }
    
    private String obtainBase64(String data) throws Exception {
        byte[] dataBytes = null;
        String err = "";
        if(data.startsWith("http"))
            dataBytes = Utils.sendHTTP(data, "GET", null, null, true, true).data; //Utils.toByteArray(new URL(data).openStream());
        if(dataBytes == null) {
            try {
                dataBytes = Utils.base64Decode(data);
            }catch(Exception e) { err += "\nBase64 format check error: " + e.getMessage(); }
        }
        if(dataBytes == null) {
            try {
                dataBytes = Utils.downloadLocalFile(data);
            }catch(Exception e) { err += "\nLocal file check error: " + e.getMessage(); }
        }
        if(dataBytes == null)
            throw new Exception("Impossible to identify the parameter format:\n" + err);
        
        return Utils.base64Encode(dataBytes);
    }
    
    private String uploadImage(String imageB64) throws Exception {
        HashMap<String, String> parametersMap = new HashMap<String, String>();
        String httpMethod = "POST";
        String apiEndpoint = "https://upload.twitter.com/1.1/media/upload.json";
        parametersMap.put("media_data", imageB64);
        
        ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
        htmlHeaderList.add(new String[]{"Content-Type", "application/x-www-form-urlencoded"});
        htmlHeaderList.add(new String[]{"Authorization", Utils.createOAuth10HttpAuthorizationHeader(httpMethod, apiEndpoint, oauth_consumer_key, oauth_consumer_secret, oauth_token, oauth_token_secret, parametersMap)});
        String postData = "";
        for(Entry<String, String> parameterEntry : parametersMap.entrySet())
            postData += URLEncoder.encode(parameterEntry.getKey(), "UTF-8") + "=" + URLEncoder.encode(parameterEntry.getValue(), "UTF-8") + "&";
        if(!postData.isEmpty())
            postData = postData.substring(0, postData.length()-1);
        
        HttpResults out = Utils.sendHTTP(apiEndpoint, httpMethod, postData, htmlHeaderList, true, true);
        
        if(out.headerMap.get("content-type") == null)
            throw new Exception("Impossible to identify the content-type header");
        
        String returnedContentType = out.headerMap.get("content-type").get(0);
        if(!returnedContentType.startsWith("application/json"))
            throw new Exception("Unexpected Content-Type header: "+returnedContentType+"; Expected application/json");
        
        String returnedString = new String(out.data, "UTF-8");
        JsonObject returnedDataJson = Json.createReader(new StringReader(returnedString)).readObject();
        
        String media_id_string = returnedDataJson.getString("media_id_string", "");
        if(media_id_string.isEmpty())
            throw new Exception("Error returned from Twitter Upload Media service: " + returnedString);

        return media_id_string;
    }
    
    @Override
    public void stop() throws Exception {
        oauth_consumer_key = null;
        oauth_consumer_secret = null;
        oauth_token = null;
        oauth_token_secret = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        TwitterPOSTConnector connector = new TwitterPOSTConnector();
        Utils.uploadFolder = "";
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("oauth_consumer_key", Json.createObjectBuilder().add("value", "TO_PROVIDE")) //TO_PROVIDE
                .add("oauth_consumer_secret", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .add("oauth_token", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .add("oauth_token_secret", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("text", Json.createObjectBuilder().add("value", "AutoTweet Test :)"))
                .add("respondToId", Json.createObjectBuilder().add("value", ""))
                .add("image", Json.createObjectBuilder().add("value", "D:\\0-PERSONAL\\foto Curriculum.jpg"))
                .add("image2", Json.createObjectBuilder().add("value", "D:\\0-PERSONAL\\foto Curriculum.jpg"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
