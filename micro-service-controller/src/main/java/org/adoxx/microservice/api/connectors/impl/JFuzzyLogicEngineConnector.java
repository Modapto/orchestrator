package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.rule.FuzzyRuleSet;
import net.sourceforge.jFuzzyLogic.rule.Variable;

public class JFuzzyLogicEngineConnector extends SyncConnectorA{

    private FuzzyRuleSet frs = null;
    
    @Override
    public String getName() {
        return "JFuzzyLogic FCL Engine Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "A connector to an internal JFuzzyLogic FCL Engine library")
            .add("de", "A connector to an internal JFuzzyLogic FCL Engine library")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("fclFilePath", Json.createObjectBuilder()
                .add("name", "FCL File Path")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The FCL standard complient file containing the fuzzy rules you defined (Optional)")
                    .add("de", "The FCL standard complient file containing the fuzzy rules you defined (Optional)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("requireUpload", true)))
            .add("fclFileContent", Json.createObjectBuilder()
                .add("name", "FCL File Content")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The FCL standard complient text containing the fuzzy rules you defined (Optional)")
                    .add("de", "The FCL standard complient file containing the fuzzy rules you defined (Optional)"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("variables", Json.createObjectBuilder()
                .add("name", "Decision Variables")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The variables (expressed in JSON format) that will be available during the evaluation of the decision")
                    .add("de", "The variables (expressed in JSON format) that will be available during the evaluation of the decision"))
                .add("value", "")
                .add("sample", "{\"variable_1_name\":\"variable_value_string\", \"variable_2_name\":variable_value_int}"))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  results : {" + "\n"
                + "    output_name_1 : 'output_variable_string_value', " + "\n"
                + "    output_name_2 : output_variable_number_value, " + "\n"
                + "    output_name_3 : output_variable_boolean_value, " + "\n"
                + "    ..." + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        String fclFilePath = startConfiguration.getJsonObject("fclFilePath")==null?"":startConfiguration.getJsonObject("fclFilePath").getString("value", "");
        String fclFileContent = startConfiguration.getJsonObject("fclFileContent")==null?"":startConfiguration.getJsonObject("fclFileContent").getString("value", "");
        if(fclFilePath.isEmpty() && fclFileContent.isEmpty()) throw new Exception("fclFilePath or fclFileContent not provided");
        
        FIS fis = null;
        if (!fclFilePath.isEmpty())
            fis = FIS.load(Utils.revealLocalFile(fclFilePath), false);
        else
            fis = FIS.createFromString(fclFileContent, false);
        if(fis == null)
            throw new Exception("You provided a not valid FCL");
        frs = fis.getFuzzyRuleSet();
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {

        String variables = configuration.getJsonObject("variables")==null?"":configuration.getJsonObject("variables").getString("value", "");
        if(variables.isEmpty()) throw new Exception("variables not provided");
        JsonObject variablesJson = null;
        try{
            variablesJson = Json.createReader(new StringReader(variables)).readObject();
        }catch(Exception ex) {
            throw new Exception("variables is not in the JSON format: "+variables+"\n"+ex.getMessage());
        }
        
        for(Entry<String, JsonValue> entry : variablesJson.entrySet()) {
            if(entry.getValue().getValueType().compareTo(ValueType.NUMBER) != 0)
                throw new Exception("The provided variable value is not a number: " + entry.getKey());
            frs.setVariable(entry.getKey(), ((JsonNumber)entry.getValue()).doubleValue());
        }
        frs.evaluate();
        
        JsonObjectBuilder resultsJson = Json.createObjectBuilder();
        for(Entry<String, Variable> entry : frs.getVariables().entrySet()) {
            //System.out.println(entry.getKey() + ": " + entry.getValue().getLatestDefuzzifiedValue() + " - " + entry.getValue().getValue());
            double val = entry.getValue().getLatestDefuzzifiedValue();
            if(!Double.isNaN(val))
                resultsJson.add(entry.getKey(), val);
        }
        
        JsonObject out = Json.createObjectBuilder()
            .add("results", resultsJson)
            .add("print", frs.toString())
            .build();
        return out;
    }
    
    @Override
    public void stop() throws Exception {
        frs = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        JFuzzyLogicEngineConnector connector = new JFuzzyLogicEngineConnector();
        Utils.uploadFolder = "";
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("fclFilePath", Json.createObjectBuilder().add("value", "D:\\ADOXX.ORG\\MY_MICROSERVICE_FRAMEWORK\\TEST\\mBot.fcl"))
                .add("fclFileContent", Json.createObjectBuilder().add("value", ""))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("variables", Json.createObjectBuilder().add("value", Json.createObjectBuilder()
                    //.add("service", 3)
                    //.add("food", 7)
                    .add("angle", -50)
                    .build().toString()))
                .build()
            );
            System.out.println(callOutputJson);
            System.out.println(callOutputJson.getString("print"));
        } finally {connector.threadStop();}
    }
    */
}
