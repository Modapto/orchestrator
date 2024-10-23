package org.adoxx.microservice.api.connectors.impl;

import java.io.FileInputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;

public class CamundaDMNEngineConnector extends SyncConnectorA{

    private String dmnFilePath = "";
    private DmnEngine dmnEngine = null;
    private List<DmnDecision> dmnDecisionList = null;
    
    @Override
    public String getName() {
        return "Camunda DMN Engine Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "A connector to an internal Camunda DMN Engine library")
            .add("de", "A connector to an internal Camunda DMN Engine library")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("dmnFilePath", Json.createObjectBuilder()
                .add("name", "DMN File Path")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The DMN standard complient file containing the rules you defined")
                    .add("de", "The DMN standard complient file containing the rules you defined"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("requireUpload", true)))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("decisionKey", Json.createObjectBuilder()
                .add("name", "Decision Key")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The key inside the DMN model of the decision to evaluate")
                    .add("de", "The key inside the DMN model of the decision to evaluate"))
                .add("value", ""))
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
                + "  DecisionResultList : [{" + "\n"
                + "    dmn_table_output_name_1 : 'output_variable_string_value', " + "\n"
                + "    dmn_table_output_name_2 : output_variable_number_value, " + "\n"
                + "    dmn_table_output_name_3 : output_variable_boolean_value, " + "\n"
                + "    ..." + "\n"
                + "  }, {" + "\n"
                + "    ..." + "\n"
                + "  }]" + "\n"
                + "}" + "\n"
            ;
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        dmnFilePath = startConfiguration.getJsonObject("dmnFilePath")==null?"":startConfiguration.getJsonObject("dmnFilePath").getString("value", "");
        if(dmnFilePath.isEmpty()) throw new Exception("dmnFilePath not provided");
        dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
        dmnDecisionList = dmnEngine.parseDecisions(new FileInputStream(Utils.revealLocalFile(dmnFilePath)));
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        //https://docs.camunda.org/manual/7.4/user-guide/dmn-engine/evaluate-decisions/
        //https://docs.camunda.org/javadoc/camunda-bpm-platform/7.8/
        //http://www.bpm-guide.de/2016/10/19/5-reasons-to-switch-from-activiti-to-camunda/
        String decisionKey = configuration.getJsonObject("decisionKey")==null?"":configuration.getJsonObject("decisionKey").getString("value", "");
        if(decisionKey.isEmpty()) throw new Exception("decisionKey not provided");
        String variables = configuration.getJsonObject("variables")==null?"":configuration.getJsonObject("variables").getString("value", "");
        if(variables.isEmpty()) throw new Exception("variables not provided");
        JsonObject variablesJson = null;
        try{
            variablesJson = Json.createReader(new StringReader(variables)).readObject();
        }catch(Exception ex) {
            throw new Exception("variables is not in the JSON format: "+variables+"\n"+ex.getMessage());
        }

        DmnDecision dmnDecision = null;
        for(DmnDecision dmnDecisionTmp : dmnDecisionList)
            if(dmnDecisionTmp.getKey().equals(decisionKey))
                dmnDecision = dmnDecisionTmp;
        if(dmnDecision == null)
            throw new Exception("Impossible to find the decision with Key " + decisionKey);
        
        Map<String, Object> variablesMap = new HashMap<String, Object>();
        for(Entry<String, JsonValue> entry : variablesJson.entrySet()) {
            if(entry.getValue().getValueType().compareTo(ValueType.STRING) == 0) {
                variablesMap.put(entry.getKey(), ((JsonString)entry.getValue()).getString());
            } else if(entry.getValue().getValueType().compareTo(ValueType.NUMBER) == 0) {
                JsonNumber number = (JsonNumber)entry.getValue();
                if(number.isIntegral())
                    variablesMap.put(entry.getKey(), number.longValueExact());
                else
                    variablesMap.put(entry.getKey(), number.doubleValue());
            } else if(entry.getValue().getValueType().compareTo(ValueType.NULL) == 0)
                variablesMap.put(entry.getKey(), null);
            else if(entry.getValue().getValueType().compareTo(ValueType.FALSE) == 0)
                variablesMap.put(entry.getKey(), false);
            else if(entry.getValue().getValueType().compareTo(ValueType.TRUE) == 0)
                variablesMap.put(entry.getKey(), true);
            else
                throw new Exception("The variable " + entry.getKey() + "is in an incorrect format: " + entry.getValue().getValueType());
        }
        
        DmnDecisionResult decisionResults = dmnEngine.evaluateDecision(dmnDecision, variablesMap);
        
        JsonArrayBuilder decisionResultsJsonArray = Json.createArrayBuilder();
        for(int i=0; i<decisionResults.size(); i++) {
            JsonObjectBuilder decisionResultJson = Json.createObjectBuilder();
            DmnDecisionResultEntries decisionResult = decisionResults.get(i);
            for(Entry<String, Object> decisionOutputEntry : decisionResult.entrySet()) {
                Object val = decisionOutputEntry.getValue();
                if(val instanceof String)
                    decisionResultJson.add(decisionOutputEntry.getKey(), (String)val);
                else if(val instanceof Boolean)
                    decisionResultJson.add(decisionOutputEntry.getKey(), (Boolean)val);
                else if(val instanceof Integer)
                    decisionResultJson.add(decisionOutputEntry.getKey(), (Integer)val);
                else if(val instanceof Double)
                    decisionResultJson.add(decisionOutputEntry.getKey(), (Double)val);
                else if(val==null)
                    decisionResultJson.add(decisionOutputEntry.getKey(), JsonValue.NULL);
                else
                    throw new Exception("The output value of " + decisionOutputEntry.getKey() + " have an unrecognized format: " + val.getClass());
            }
            decisionResultsJsonArray.add(decisionResultJson);
        }
        
        JsonObject decisionResultsJson = Json.createObjectBuilder().add("DecisionResultList", decisionResultsJsonArray).build();
        return decisionResultsJson;
        /*
        return Json.createObjectBuilder()
            .add("dataMIME", "application/json")
            .add("dataJson", decisionResultsJson)
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
        */
    }
    
    @Override
    public void stop() throws Exception {
        dmnFilePath = "";
        dmnEngine = null;
        dmnDecisionList = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
      //https://github.com/camunda/camunda-bpm-examples/tree/master/dmn-engine/dmn-engine-java-main-method
        CamundaDMNEngineConnector connector = new CamundaDMNEngineConnector();
        Utils.uploadFolder = "";
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("dmnFilePath", Json.createObjectBuilder().add("value", "D:\\ADOXX.ORG\\MY_MICROSERVICE_FRAMEWORK\\TEST\\dish-decision.dmn11.xml"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("decisionKey", Json.createObjectBuilder().add("value", "decision"))
                .add("variables", Json.createObjectBuilder().add("value", Json.createObjectBuilder().add("season", "Spring").add("guestCount", 4).build().toString()))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
