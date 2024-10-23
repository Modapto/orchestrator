package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.adoxx.microservice.utils.Utils.HttpResults;

public class TwilioSMSSenderConnector extends SyncConnectorA {
    
    @Override
    public String getName() {
        return "Twilio SMS Sender Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Send an SMS using the Twilio service")
            .add("de", "Send an SMS using the Twilio service")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("accountSID", Json.createObjectBuilder()
                .add("name", "Account SID")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Your personal Twilio SID")
                    .add("de", "Your personal Twilio SID"))
                .add("value", ""))
            .add("accountAuthToken", Json.createObjectBuilder()
                .add("name", "Account Authentication Token")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Your personal Twilio Authentication Token")
                    .add("de", "Your personal Twilio Authentication Token"))
                .add("value", ""))
            .add("from", Json.createObjectBuilder()
                .add("name", "From")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Twilio registered From phone number")
                    .add("de", "Twilio registered From phone number"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("to", Json.createObjectBuilder()
                .add("name", "To")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The phone number of the SMS receiver")
                    .add("de", "The phone number of the SMS receiver"))
                .add("value", ""))
            .add("text", Json.createObjectBuilder()
                .add("name", "Text")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The text to send in the SMS (1600 character limit)")
                    .add("de", "The text to send in the SMS (1600 character limit)"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "Json as returned by the Twilio API";
    }
    
    private String sid, token, from = null;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        sid = startConfiguration.getJsonObject("accountSID")==null?"":startConfiguration.getJsonObject("accountSID").getString("value", "");
        if(sid.isEmpty()) throw new Exception("accountSID not provided");
        token = startConfiguration.getJsonObject("accountAuthToken")==null?"":startConfiguration.getJsonObject("accountAuthToken").getString("value", "");
        if(token.isEmpty()) throw new Exception("accountAuthToken not provided");
        from = startConfiguration.getJsonObject("from")==null?"":startConfiguration.getJsonObject("from").getString("value", "");
        if(from.isEmpty()) throw new Exception("from not provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String to = configuration.getJsonObject("to")==null?"":configuration.getJsonObject("to").getString("value", "");
        if(to.isEmpty()) throw new Exception("'to' not provided");
        String text = configuration.getJsonObject("text")==null?"":configuration.getJsonObject("text").getString("value", "");
        if(text.isEmpty()) throw new Exception("text not provided");
        if(text.length()>1600) throw new Exception("text exceed 1600 characters");
        
        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/"+sid+"/Messages.json";
        String postData = "From="+URLEncoder.encode(from, "UTF-8")+"&To="+URLEncoder.encode(to, "UTF-8")+"&Body="+URLEncoder.encode(text, "UTF-8");
        
        ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
        htmlHeaderList.add(new String[]{"Content-Type", "application/x-www-form-urlencoded"});
        htmlHeaderList.add(new String[]{"Authorization", "Basic " + Utils.base64Encode((sid+":"+token).getBytes("UTF-8"))});
        
        HttpResults out = Utils.sendHTTP(endpoint, "POST", postData, htmlHeaderList, true, true);
        
        if(out.headerMap.get("content-type") == null)
            throw new Exception("Impossible to identify the content-type header");
        
        String returnedContentType = out.headerMap.get("content-type").get(0);
        if(!returnedContentType.toLowerCase().startsWith("application/json"))
            throw new Exception("Unexpected content-type header: "+returnedContentType+"; Expected application/json");
        
        String returnedString = new String(out.data, "UTF-8");
        JsonObject returnedDataJson = Json.createReader(new StringReader(returnedString)).readObject();
        
        if(!returnedDataJson.getString("message", "").isEmpty() || !returnedDataJson.getString("error_message", "").isEmpty())
            throw new Exception("Error returned from Twilio service: " + returnedString);
        
        return returnedDataJson;
        /*
        return Json.createObjectBuilder()
            .add("dataFormat", "application/json")
            .add("data", "")
            .add("dataBase64", Utils.base64Encode(out.data))
            .add("moreInfo", Json.createObjectBuilder()
                .add("executionTime", Utils.getCurrentTime())
            ).build();
        */
    }
    
    @Override
    public void stop() throws Exception {
        sid = null;
        token = null;
        from = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        //https://www.twilio.com/console/sms/getting-started
        TwilioSMSSenderConnector connector = new TwilioSMSSenderConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("accountSID", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .add("accountAuthToken", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .add("from", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("to", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .add("text", Json.createObjectBuilder().add("value", "test message"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
