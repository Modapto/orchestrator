package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class FusekiRESTConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "Jena Fuseki Triplestore Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Get data from an Apache Jena Fuseki triplestore")
            .add("de", "Get data from an Apache Jena Fuseki triplestore")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("endpoint", Json.createObjectBuilder()
                .add("name", "Endpoint")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Url of the triple store")
                    .add("de", "Url of the triple store"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("sparqlQuery", Json.createObjectBuilder()
                .add("name", "Query")
                .add("description", Json.createObjectBuilder()
                    .add("en", "SPARQL Query")
                    .add("de", "SPARQL Query"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  columns : ['_query_column_name_1', '_query_column_name_2', ...]," + "\n"
                + "  data : [{" + "\n"
                + "    _query_column_name_1 : 'query_result_row_1_column_1'," + "\n"
                + "    _query_column_name_1 : 'query_result_row_1_column_2'," + "\n"
                + "    ..." + "\n"
                + "  }, {" + "\n"
                + "    _query_column_name_1 : 'query_result_row_2_column_1'," + "\n"
                + "    _query_column_name_2 : 'query_result_row_2_column_2'," + "\n"
                + "    ..." + "\n"
                + "  }]," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                ;
    }
    
    private String endpoint = "";
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        endpoint = startConfiguration.getJsonObject("endpoint")==null?"":startConfiguration.getJsonObject("endpoint").getString("value", "");
        if(endpoint.isEmpty()) throw new Exception("Fuseki endpoint not provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String sparqlQuery = configuration.getJsonObject("sparqlQuery")==null?"":configuration.getJsonObject("sparqlQuery").getString("value", "");
        if(sparqlQuery.isEmpty()) throw new Exception("SPARQL query not provided");
        
        ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
        htmlHeaderList.add(new String[]{"Content-Type", "application/x-www-form-urlencoded"});
        String resultJsonS = new String(Utils.sendHTTP(endpoint, "POST", "query="+URLEncoder.encode(sparqlQuery, "UTF-8"), htmlHeaderList, true, true).data, "UTF-8");
        JsonObject resultJson = Json.createReader(new StringReader(resultJsonS)).readObject();
        JsonArray resultList = resultJson.getJsonObject("results").getJsonArray("bindings");
        JsonArray varsList = resultJson.getJsonObject("head").getJsonArray("vars");
        
        JsonArrayBuilder dataList = Json.createArrayBuilder();
        
        for(JsonValue result : resultList){
            JsonObjectBuilder objBuilder = Json.createObjectBuilder();
            for(Entry<String, JsonValue> entry : ((JsonObject)result).entrySet())
                objBuilder.add(entry.getKey(), ((JsonObject)entry.getValue()).getString("value"));
            dataList.add(objBuilder);
        }
        
        return Json.createObjectBuilder()
            .add("columns", varsList)
            .add("data", dataList)
            .add("moreInfo", Json.createObjectBuilder().add("retrievalTime", Utils.getCurrentTime()))
            .build();
    }
    
    @Override
    public void stop() throws Exception {
        endpoint = "";
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        FusekiRESTConnector connector = new FusekiRESTConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("endpoint", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("sparqlQuery", Json.createObjectBuilder().add("value", "TO_PROVIDE"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
