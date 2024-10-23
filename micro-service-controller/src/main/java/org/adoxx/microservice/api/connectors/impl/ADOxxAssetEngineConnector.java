package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.adoxx.microservice.utils.Utils.HttpResults;

public class ADOxxAssetEngineConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "ADOxx Asset Engine Connector";
    }
    
    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Module for connection to the ADOxx Asset Engine")
            .add("de", "Module for connection to the ADOxx Asset Engine")
            .build();
    }
    
    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("endpoint", Json.createObjectBuilder()
                .add("name", "Endpoint")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Service REST endpoint")
                    .add("de", "Service REST endpoint"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("serviceId", Json.createObjectBuilder()
                .add("name", "Service Id")
                .add("description", Json.createObjectBuilder()
                    .add("en", "ID of the data service")
                    .add("de", "ID of the data service"))
                .add("value", ""))
            .add("periodStartDate", Json.createObjectBuilder()
                .add("name", "Period Start Date")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The data retrieval starting period in the format YYYY.MM.DD HH:MM")
                    .add("de", "The data retrieval starting period in the format YYYY.MM.DD HH:MM"))
                .add("value", ""))
            .add("periodEndDate", Json.createObjectBuilder()
                .add("name", "Period End Date")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The data retrieval ending period in the format YYYY.MM.DD HH:MM")
                    .add("de", "The data retrieval ending period in the format YYYY.MM.DD HH:MM"))
                .add("value", ""))
            .add("aggregationPeriod", Json.createObjectBuilder()
                .add("name", "Aggregation Period")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The data aggregation period (es: quaterly|hourly|daily|weekly|monthly|yearly|businesshours|raw)")
                    .add("de", "The data aggregation period (es: quaterly|hourly|daily|weekly|monthly|yearly|businesshours|raw)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder()
                        .add("quaterly")
                        .add("hourly")
                        .add("daily")
                        .add("weekly")
                        .add("monthly")
                        .add("yearly")
                        .add("businesshours")
                        .add("raw"))))
            .add("aggregationMechanism", Json.createObjectBuilder()
                .add("name", "Aggregation Mechanism")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The data aggregation mechanism (es: average|sum)")
                    .add("de", "The data aggregation mechanism (es: average|sum)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder()
                        .add("average")
                        .add("sum"))))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{"
                + "  columns : ['time', 'value', 'extrapolated', 'unit', 'target', 'comment']," + "\n"
                + "  data : [{" + "\n"
                + "    time : '', " + "\n"
                + "    value : ''," + "\n"
                + "    extrapolated : ''" + "\n"
                + "    unit : ''," + "\n"
                + "    target : ''," + "\n"
                + "    comment : '" + "\n"
                + "  }, {" + "\n"
                + "    ..." + "\n"
                + "  }]," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''," + "\n"
                + "    storeID : ''," + "\n"
                + "    script : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                ;
    }
    
    private String endpoint = "";
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        endpoint = startConfiguration.getJsonObject("endpoint")==null?"":startConfiguration.getJsonObject("endpoint").getString("value", "");
        if(endpoint.isEmpty()) throw new Exception("endpoint not provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String serviceId = configuration.getJsonObject("serviceId")==null?"":configuration.getJsonObject("serviceId").getString("value", "");
        if(serviceId.isEmpty()) throw new Exception("serviceId not provided");
        String periodStartDate = configuration.getJsonObject("periodStartDate")==null?"":configuration.getJsonObject("periodStartDate").getString("value", "");
        if(periodStartDate.isEmpty())
            periodStartDate = new SimpleDateFormat("yyyy.MM.dd HH:mm").format(new Date(System.currentTimeMillis()-TimeUnit.DAYS.toMillis(31)));
        String periodEndDate = configuration.getJsonObject("periodEndDate")==null?"":configuration.getJsonObject("periodEndDate").getString("value", "");
        if(periodEndDate.isEmpty())
            periodEndDate = new SimpleDateFormat("yyyy.MM.dd HH:mm").format(new Date());
        String aggregationPeriod = configuration.getJsonObject("aggregationPeriod")==null?"":configuration.getJsonObject("aggregationPeriod").getString("value", "");
        if(aggregationPeriod.isEmpty()) 
            aggregationPeriod = "daily";
        String aggregationMechanism = configuration.getJsonObject("aggregationMechanism")==null?"":configuration.getJsonObject("aggregationMechanism").getString("value", "");
        if(aggregationMechanism.isEmpty()) 
            aggregationMechanism = "average";
        
        String serviceFullUrl = endpoint+"/dataexplorer/getLiveHistoryJson?dataServiceID="+serviceId+"&aggregatePeriod="+aggregationPeriod+"&aggregateMechanism="+aggregationMechanism+"&startDate="+URLEncoder.encode(periodStartDate, "UTF-8")+"&endDate="+URLEncoder.encode(periodEndDate, "UTF-8");
        
        ArrayList<String[]> headerList = new ArrayList<String[]>();
        headerList.add(new String[]{"Content-Type", "application/x-www-form-urlencoded"});
        HttpResults out = Utils.sendHTTP(serviceFullUrl, "GET", null, headerList , true, true);
        if(out.data.length==0)
            throw new Exception("The Asset Engine returned an empty response");
        String serviceOutput = new String(out.data, "UTF-8");
        JsonObject serviceOutputJson = null;
        try {
            serviceOutputJson = Json.createReader(new StringReader(serviceOutput)).readObject();
        } catch(Exception e) {
            throw new Exception("The Asset Engine returned an invalid JSON: " + serviceOutput);
        }
        if(!serviceOutputJson.getBoolean("success"))
            throw new Exception("The Asset Engine returned the following error: (" + serviceOutputJson.getString("ecode", "null") + ") " + serviceOutputJson.getString("errorText", "null"));
        
        JsonArrayBuilder dataList = Json.createArrayBuilder();
        for(JsonValue dataV : serviceOutputJson.getJsonArray("datalist")) {
            JsonObject dataO = (JsonObject) dataV;
            dataList.add(Json.createObjectBuilder()
                .add("time", ""+Utils.getTime(dataO.getJsonNumber("valueDate").longValue()))
                .add("value", ""+dataO.getJsonNumber("value").doubleValue())
                .add("extrapolated", ""+dataO.getBoolean("extrapolated"))
                .add("unit", ""+dataO.getString("unit", ""))
                .add("target", ""+dataO.getJsonNumber("target").doubleValue())
                .add("comment", ""+dataO.getString("comment"))
            );
        }

        return Json.createObjectBuilder()
            .add("columns", Json.createArrayBuilder()
                .add("time")
                .add("value")
                .add("extrapolated")
                .add("unit")
                .add("target")
                .add("comment")
            ).add("data", dataList)
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
                .add("storeID", serviceOutputJson.getString("storeID", ""))
                .add("script", serviceOutputJson.getString("script", ""))
            ).build();
    }
    
    @Override
    public void stop() throws Exception {
        endpoint = "";
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        ADOxxAssetEngineConnector connector = new ADOxxAssetEngineConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("endpoint", Json.createObjectBuilder().add("value", "https://orbeet.boc-group.eu/DataAssetEngine/services/rest"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                 .add("serviceId", Json.createObjectBuilder().add("value", "98087"))
                .add("periodStartDate", Json.createObjectBuilder().add("value", "2017.10.04 10:13"))
                .add("periodEndDate", Json.createObjectBuilder().add("value", "2017.12.11 10:13"))
                .add("aggregationPeriod", Json.createObjectBuilder().add("value", "weekly"))
                .add("aggregationMechanism", Json.createObjectBuilder().add("value", "average"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
