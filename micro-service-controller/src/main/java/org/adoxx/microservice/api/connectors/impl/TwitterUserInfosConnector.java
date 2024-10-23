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

public class TwitterUserInfosConnector extends SyncConnectorA {
    
    @Override
    public String getName() {
        return "Twitter User Infos Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Obtain various informations of a Twitter user")
            .add("de", "Obtain various informations of a Twitter user")
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
            .add("userId", Json.createObjectBuilder()
                .add("name", "User ID")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The ID of the user to ask for informations (alternative to User Screen Name)")
                    .add("de", "The ID of the user to ask for informations (alternative to User Screen Name)"))
                .add("value", ""))
            .add("userScreenName", Json.createObjectBuilder()
                .add("name", "User Screen Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The Screen Name of the user to ask for informations (alternative to User ID)")
                    .add("de", "The Screen Name of the user to ask for informations (alternative to User ID)"))
                .add("value", ""))
            .add("infoType", Json.createObjectBuilder()
                .add("name", "Information Type")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The type of results to obtain (followers|friends|details)")
                    .add("de", "The type of results to obtain (followers|friends|details)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder().add("followers").add("friends").add("details"))))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "Json as returned from the Twitter Search APIs";
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
        String userId = configuration.getJsonObject("userId")==null?"":configuration.getJsonObject("userId").getString("value", "");
        String userScreenName = configuration.getJsonObject("userScreenName")==null?"":configuration.getJsonObject("userScreenName").getString("value", "");
        if(userId.isEmpty() && userScreenName.isEmpty()) throw new Exception("userId or userScreenName not provided");
        String infoType = configuration.getJsonObject("infoType")==null?"":configuration.getJsonObject("infoType").getString("value", "");
        if(infoType.isEmpty()) throw new Exception("infoType not provided");
        
        HashMap<String, String> parametersMap = new HashMap<String, String>();
        
        String httpMethod = "GET";
        String apiEndpoint = "https://api.twitter.com/1.1/search/tweets.json";
        if(!userId.isEmpty())
            parametersMap.put("user_id", userId);
        if(!userScreenName.isEmpty())
            parametersMap.put("screen_name", userScreenName);
        if(infoType.toLowerCase().equals("followers"))
            apiEndpoint = "https://api.twitter.com/1.1/followers/ids.json";
        else if(infoType.toLowerCase().equals("friends"))
            apiEndpoint = "https://api.twitter.com/1.1/friends/ids.json";
        else if(infoType.toLowerCase().equals("details"))
            apiEndpoint = "https://api.twitter.com/1.1/users/show.json";
        else
            throw new Exception("infoType " + infoType + " not recognized");
        
        ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
        htmlHeaderList.add(new String[]{"Content-Type", "application/x-www-form-urlencoded"});
        htmlHeaderList.add(new String[]{"Authorization", Utils.createOAuth10HttpAuthorizationHeader(httpMethod, apiEndpoint, oauth_consumer_key, oauth_consumer_secret, oauth_token, oauth_token_secret, parametersMap)});
        String postData = "";
        for(Entry<String, String> parameterEntry : parametersMap.entrySet())
            postData += URLEncoder.encode(parameterEntry.getKey(), "UTF-8") + "=" + URLEncoder.encode(parameterEntry.getValue(), "UTF-8") + "&";
        if(!postData.isEmpty())
            postData = postData.substring(0, postData.length()-1);
        if(httpMethod.equals("GET")) {
            apiEndpoint += "?" + postData;
            postData = "";
        }
            
        HttpResults out = Utils.sendHTTP(apiEndpoint, httpMethod, postData, htmlHeaderList, true, true);
        
        if(out.headerMap.get("content-type") == null)
            throw new Exception("Impossible to identify the content-type header");
        
        String returnedContentType = out.headerMap.get("content-type").get(0);
        if(!returnedContentType.startsWith("application/json"))
            throw new Exception("Unexpected Content-Type header: "+returnedContentType+"; Expected application/json");
        
        String returnedString = new String(out.data, "UTF-8");
        //System.out.println(returnedString);
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
    
    @Override
    public void stop() throws Exception {
        oauth_consumer_key = null;
        oauth_consumer_secret = null;
        oauth_token = null;
        oauth_token_secret = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        TwitterUserInfosConnector connector = new TwitterUserInfosConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("oauth_consumer_key", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .add("oauth_consumer_secret", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .add("oauth_token", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .add("oauth_token_secret", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("userScreenName", Json.createObjectBuilder().add("value", "DamianoFalcioni"))
                .add("infoType", Json.createObjectBuilder().add("value", "details"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
