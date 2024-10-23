package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.adoxx.microservice.utils.Utils.HttpResults;

public class FacebookGraphConnector extends SyncConnectorA {
    /*
     - Steps to generate an eccess token: https://elfsight.com/blog/2017/10/how-to-get-facebook-access-token/
     - Page for the app creation: https://developers.facebook.com/apps/
     - Page for the access token creation & api test : https://developers.facebook.com/tools/explorer/
     */
    @Override
    public String getName() {
        return "Facebook Graph Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Connector for the Facebook Graph API")
            .add("de", "Connector for the Facebook Graph API")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("access_token", Json.createObjectBuilder()
                .add("name", "access_token")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The access_token as provided by Facebook (can be generated using the FB API Explorer tool https://developers.facebook.com/tools/explorer/)")
                    .add("de", "The access_token as provided by Facebook (can be generated using the FB API Explorer tool https://developers.facebook.com/tools/explorer/)"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("action", Json.createObjectBuilder()
                .add("name", "Action")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The action to perform (GET|POST|DELETE)")
                    .add("de", "The action to perform (GET|POST|DELETE)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder().add("GET").add("POST").add("DELETE"))))
            .add("nodeQuery", Json.createObjectBuilder()
                .add("name", "FB Node Query")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The node query (can be tested using the FB API Explorer tool https://developers.facebook.com/tools/explorer/ )")
                    .add("de", "The node query (can be tested using the FB API Explorer tool https://developers.facebook.com/tools/explorer/ )"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A Json Object as returned by the Facebook Graph API.\n"
                + "For more documentation have a look at https://developers.facebook.com/docs/graph-api/using-graph-api \n"
                + "The output is the same as returned by the official Facebook Graph Explorer: https://developers.facebook.com/tools/explorer/ \n";
    }
    
    private String access_token = null;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        access_token = startConfiguration.getJsonObject("access_token")==null?"":startConfiguration.getJsonObject("access_token").getString("value", "");
        if(access_token.isEmpty()) throw new Exception("access_token not provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String action = configuration.getJsonObject("action")==null?"":configuration.getJsonObject("action").getString("value", "");
        if(action.isEmpty() || (!action.toUpperCase().equals("GET") && !action.toUpperCase().equals("POST") && !action.toUpperCase().equals("DELETE"))) throw new Exception("action must be set to GET, POST or DELETE");
        String nodeQuery = configuration.getJsonObject("nodeQuery")==null?"":configuration.getJsonObject("nodeQuery").getString("value", "");
        if(nodeQuery.isEmpty()) throw new Exception("nodeQuery not provided");
        nodeQuery += (nodeQuery.contains("?")?"&":"?") + "access_token="+access_token;
        
        String apiEndpoint = "https://graph.facebook.com/v3.0/"+nodeQuery;
        
        ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
        htmlHeaderList.add(new String[]{"Content-Type", "application/x-www-form-urlencoded"});
            
        HttpResults out = Utils.sendHTTP(apiEndpoint, action, null, htmlHeaderList, true, true);
        
        if(out.headerMap.get("content-type") == null)
            throw new Exception("Impossible to identify the content-type header");
        
        String returnedContentType = out.headerMap.get("content-type").get(0);
        if(!returnedContentType.startsWith("application/json"))
            throw new Exception("Unexpected content-type header: "+returnedContentType+"; Expected application/json");
        
        String returnedString = new String(out.data, "UTF-8");
        //System.out.println(returnedString);
        JsonObject returnedDataJson = Json.createReader(new StringReader(returnedString)).readObject();
        
        if(returnedDataJson.containsKey("error"))
            throw new Exception("Error returned from Facebook service: " + returnedString);

        return returnedDataJson;
        /*
        return Json.createObjectBuilder()
            .add("dataMIME", "application/json")
            .add("dataJson", returnedDataJson)
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
        */
    }
    
    @Override
    public void stop() throws Exception {
        access_token = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        FacebookGraphConnector connector = new FacebookGraphConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("access_token", Json.createObjectBuilder().add("value", "TO_PROVIDE"))//TO_PROVIDE
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("action", Json.createObjectBuilder().add("value", "GET"))
                .add("nodeQuery", Json.createObjectBuilder().add("value", "me/posts?fields=message"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
