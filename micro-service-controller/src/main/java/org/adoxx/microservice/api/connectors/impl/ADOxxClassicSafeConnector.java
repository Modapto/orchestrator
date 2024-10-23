package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ADOxxClassicSafeConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "ADOxx Classic Safe Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Allow to interact with models in ADOxx Classic.\n"
                    + "Please remind that the SOAP server must be enabled \nwith the ADOScript command: CC \"AdoScript\" SERVICE start\n"
                    + "Operations allowed:\n"
                    + "-GET_MODEL_IMAGE: required parameters: modelid, scale\n"
                    + "-ADL_EXPORT: required parameters: modelid\n")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("url", Json.createObjectBuilder()
                .add("name", "URL")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Endpoint of the ADOxx SOAP service to use"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("operation", Json.createObjectBuilder()
                .add("name", "Operation")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Operation to perform"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder()
                        .add("GET_MODEL_IMAGE")
                        .add("ADL_EXPORT")
                    )))
            .add("parameters", Json.createObjectBuilder()
                .add("name", "Parameters")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Operation Parameters (expressed in JSON format ex. {\"param1\":\"val1\", \"param2\":\"val2\"})"))
                .add("value", "")
                .add("sample", "{\"param1\":\"val1\", \"param2\":\"val2\"}"))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  operation : ''" + "\n"
                + "  result : ''" + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }

    private String url = "";
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        url = startConfiguration.getJsonObject("url")==null?"":startConfiguration.getJsonObject("url").getString("value", "").toUpperCase();
        if(url.isEmpty())
            throw new Exception("url not provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String operation = configuration.getJsonObject("operation")==null?"":configuration.getJsonObject("operation").getString("value", "");
        if(operation.isEmpty()) throw new Exception("operation not provided");
        String parameters = configuration.getJsonObject("parameters")==null?"{}":configuration.getJsonObject("parameters").getString("value", "{}");
        if(parameters.isEmpty()) throw new Exception("parameters not provided");
        JsonObject parametersJson = null;
        try{
            parametersJson = Json.createReader(new StringReader(parameters)).readObject();
        }catch(Exception ex) {
            throw new Exception("parameters is not in the JSON format: " + parameters + "\n" + ex.getMessage());
        }
        
        String result = null;
        if (operation.equals("GET_MODEL_IMAGE"))
            result = op_GET_MODEL_IMAGE(parametersJson);
        if (operation.equals("ADL_EXPORT"))
            result = op_ADL_EXPORT(parametersJson);
        
        if(result == null)
            throw new Exception("Operation \"" + operation + "\" not implemented");
            
        return Json.createObjectBuilder()
            .add("operation", operation)
            .add("result", result)
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
    }
    
    @Override
    public void stop() throws Exception {
        url = "";
    }
    
    private String op_GET_MODEL_IMAGE(JsonObject parametersJson) throws Exception {
        //FIXME: use a procedure list in another file
        int modelid = parametersJson.getInt("modelid", -1);
        if(modelid == -1) throw new Exception("The parameter modelid is required for the operation GET_MODEL_IMAGE");
        String scaleS = parametersJson.getString("scale", "");
        scaleS = scaleS.isEmpty()?"1.0":scaleS;
        double scale = Double.parseDouble(scaleS);
        String adoScript = "CC \"Core\" LOAD_MODEL modelid:" + modelid + "\r\n" + "CC \"Drawing\" GEN_GFX_STR modelid:" + modelid + " scale:" + scale + " gfx-format:\"png\"\r\n" + "CC \"Core\" DISCARD_MODEL modelid:" + modelid;
        //System.out.println(adoScript);
        String resultVariable = "gfx";
        return executeAdoScript(adoScript, resultVariable);
    }
    
    private String op_ADL_EXPORT(JsonObject parametersJson) throws Exception {
        int modelid = parametersJson.getInt("modelid", -1);
        if(modelid == -1) throw new Exception("The parameter modelid is required for the operation ADL_EXPORT");
        String adoScript = "CC \"AdoScript\" GET_TEMP_FILENAME\r\n" + 
                "CC \"ImportExport\" ADL_EXPORT (filename) modelids:(" + modelid + ")\r\n" + 
                "CC \"AdoScript\" FREAD file:(filename)\r\n" + 
                "CC \"AdoScript\" FILE_DELETE file:(filename)";
        //System.out.println(adoScript);
        String resultVariable = "text";
        return executeAdoScript(adoScript, resultVariable);
    }
    
    
    private String executeAdoScript(String adoScript, String resultVariable) throws Exception {
        ArrayList<String[]> parameterList = new ArrayList<String[]>();
        parameterList.add(new String[] {"script", adoScript});
        parameterList.add(new String[] {"resultVar", resultVariable});
        Document resultXml = Utils.sendSOAP(url, "urn:AdoWS", "execute", "", parameterList, null);
        Node executeResponseEl = resultXml.getElementsByTagNameNS("urn:AdoWS","ExecuteResponse").item(0);
        String errorCode = executeResponseEl.getFirstChild().getTextContent();
        String result = executeResponseEl.getLastChild().getTextContent();
        if(!errorCode.equals("0"))
            throw new Exception("Error executing the script:\n" + adoScript + "\nError Code=" + errorCode + "\nError Desc:\n" + result);
        return result;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        ADOxxClassicSafeConnector connector = new ADOxxClassicSafeConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("url", Json.createObjectBuilder().add("value", "http://127.0.0.1"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("operation", Json.createObjectBuilder().add("value", "ADL_EXPORT"))
                .add("parameters", Json.createObjectBuilder().add("value", Json.createObjectBuilder()
                    .add("modelid", 27402)
                    .build().toString()))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
