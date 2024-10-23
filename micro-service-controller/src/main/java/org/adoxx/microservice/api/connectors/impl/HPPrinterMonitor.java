package org.adoxx.microservice.api.connectors.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class HPPrinterMonitor extends SyncConnectorA {
    @Override
    public String getName() {
        return "HP Printer Monitor Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Return color levels of the provided HP printer")
            .add("de", "Return color levels of the provided HP printer")
            .build();
    }
    
    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("ip", Json.createObjectBuilder()
                .add("name", "Printer IP")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The printer IP or Hostname")
                    .add("de", "The printer IP or Hostname"))
                .add("value", "")).build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder().build();
    }

    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  black : 60," + "\n"
                + "  cyan : 70," + "\n"
                + "  magenta : 50," + "\n"
                + "  yellow : 40," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }
    
    private String ip;

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        ip = startConfiguration.getJsonObject("ip")==null?"":startConfiguration.getJsonObject("ip").getString("value", "");
        if(ip.isEmpty()) throw new Exception("ip not provided");
    }

    @Override
    public JsonObject performCall(JsonObject callConfiguration) throws Exception {
        String monitorPage = "http://"+ip+"/mSupplyStatus.html";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(monitorPage).openStream()));
        String line=null;
        String line_previous=null;
        String black="", cyan="", magenta="", yellow="";
        
        while((line=bufferedReader.readLine()) != null ) {
            if(line.contains("BACKGROUND-COLOR: #000000"))
                black = line_previous.substring(line_previous.indexOf("WIDTH:") + 6, line_previous.indexOf("%"));
            if(line.contains("BACKGROUND-COLOR: #1be0d7"))
                cyan = line_previous.substring(line_previous.indexOf("WIDTH:") + 6, line_previous.indexOf("%"));
            if(line.contains("BACKGROUND-COLOR: #fe005f"))
                magenta = line_previous.substring(line_previous.indexOf("WIDTH:") + 6, line_previous.indexOf("%"));
            if(line.contains("BACKGROUND-COLOR: #ffdc04"))
                yellow = line_previous.substring(line_previous.indexOf("WIDTH:") + 6, line_previous.indexOf("%"));
            line_previous = line;
        }
        return Json.createObjectBuilder()
            .add("black", Integer.parseInt(black))
            .add("cyan", Integer.parseInt(cyan))
            .add("magenta", Integer.parseInt(magenta))
            .add("yellow", Integer.parseInt(yellow))
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
    }

    @Override
    public void stop() throws Exception {
        ip = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        HPPrinterMonitor connector = new HPPrinterMonitor();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("ip", Json.createObjectBuilder().add("value", "10.0.1.40"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder().build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
