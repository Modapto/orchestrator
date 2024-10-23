package org.adoxx.microservice.api.connectors.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class SPM2018DemoConnector extends SyncConnectorA{
    
    @Override
    public String getName() {
        return "SPM2018 Demo Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "A simple connector for the Demo of SPM2018")
            .add("de", "A simple connector for the Demo of SPM2018")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("OP", Json.createObjectBuilder()
                    .add("name", "Operation")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "Operation")
                        .add("de", "Operation"))
                    .add("value", "")
                    .add("moreInfos", Json.createObjectBuilder()
                        .add("choiceValues", Json.createArrayBuilder().add("GET").add("STARTBAD").add("STARTGOOD"))))
            .add("KPI", Json.createObjectBuilder()
                    .add("name", "KPI ID")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "KPI ID")
                        .add("de", "KPI ID"))
                    .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  columns : ['value', 'instant']," + "\n"
                + "  data : [{"
                + "    value: '',"
                + "    instant: ''"
                + "  }, {"
                + "    value: '',"
                + "    instant: ''"
                + "  }]," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }
    
    static HashMap<String, ArrayList<SimpleEntry<String, String>>> db = null;
    static HashMap<String, SimpleEntry<Integer, Integer>> rangeDb = null;
    
    Random random = null;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        db = new HashMap<String, ArrayList<SimpleEntry<String, String>>>();
        rangeDb = new HashMap<String, SimpleEntry<Integer, Integer>>();
        random = new Random();
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String op = configuration.getJsonObject("OP")==null?"GET":configuration.getJsonObject("OP").getString("value", "GET");
        String kpiId = configuration.getJsonObject("KPI")==null?"":configuration.getJsonObject("KPI").getString("value", "");
        if(kpiId.isEmpty()) throw new Exception("KPI parameter empty");
        
        if(op.equals("GET")) {
            ArrayList<SimpleEntry<String, String>> kpiData = db.get(kpiId);
            if(kpiData == null) {
                kpiData = new ArrayList<SimpleEntry<String, String>>();
                db.put(kpiId, kpiData);
            }
            
            SimpleEntry<Integer, Integer> minMaxRange = rangeDb.get(kpiId);
            if(minMaxRange == null) {
                minMaxRange = new SimpleEntry<Integer, Integer>(1, 3);
                rangeDb.put(kpiId, minMaxRange);
            }
            int min = minMaxRange.getKey();
            int max = minMaxRange.getValue();
            int val = random.nextInt((max-min)+1)+min;
            kpiData.add(0, new SimpleEntry<String, String>(""+val, Utils.getCurrentTime()));
            
            JsonArrayBuilder dataJsonArray = Json.createArrayBuilder();
            for(SimpleEntry<String, String> pair : kpiData) {
                dataJsonArray.add(Json.createObjectBuilder()
                    .add("value", pair.getKey())
                    .add("instant", pair.getValue())
                );
            }
            
            return Json.createObjectBuilder()
                .add("columns", Json.createArrayBuilder().add("value").add("instant"))
                .add("data", dataJsonArray)
                .add("moreInfo", Json.createObjectBuilder()
                    .add("retrievalTime", Utils.getCurrentTime())
                ).build();
        }
        
        if(op.equals("STARTBAD")) {
            rangeDb.put(kpiId, new SimpleEntry<Integer, Integer>(4, 5));
            return Json.createObjectBuilder()
                .add("status", "OK")
            .build();
        }

        if(op.equals("STARTGOOD")) {
            rangeDb.put(kpiId, new SimpleEntry<Integer, Integer>(1, 3));
            return Json.createObjectBuilder()
                .add("status", "OK")
            .build();
        }

        throw new Exception("Operation " + op + " not recognized");
    }
    
    @Override
    public void stop() throws Exception {
        db = null;
        rangeDb = null;
    }
    
    
    public static void main(String[] argv) throws Exception{
        SPM2018DemoConnector connector = new SPM2018DemoConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .build()
            );
            connector.waitThreadStart();
            connector.performCallSafe(Json.createObjectBuilder()
                    .add("OP", Json.createObjectBuilder().add("value", "STARTBAD"))
                    .add("KPI", Json.createObjectBuilder().add("value", "2"))
                    .build()
                );
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("OP", Json.createObjectBuilder().add("value", "GET"))
                .add("KPI", Json.createObjectBuilder().add("value", "1"))
                .build()
            );
            System.out.println(callOutputJson);
            callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("OP", Json.createObjectBuilder().add("value", "GET"))
                .add("KPI", Json.createObjectBuilder().add("value", "2"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    
}
