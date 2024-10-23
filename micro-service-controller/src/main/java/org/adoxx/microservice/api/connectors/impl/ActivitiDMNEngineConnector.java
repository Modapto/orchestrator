package org.adoxx.microservice.api.connectors.impl;
/*
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
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

import org.activiti.dmn.api.DmnDecisionTable;
import org.activiti.dmn.api.DmnDeployment;
import org.activiti.dmn.api.RuleEngineExecutionResult;
import org.activiti.dmn.engine.DmnEngine;
import org.activiti.dmn.engine.DmnEngineConfiguration;
import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class ActivitiDMNEngineConnector extends SyncConnectorA{

    private String dmnFilePath = "";
    private DmnEngine dmnEngine = null;
    private List<DmnDecisionTable> dmnDecisionList = null;
    
    @Override
    public String getName() {
        return "Activiti DMN Engine Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "A connector to an internal Activiti DMN Engine library")
            .add("de", "A connector to an internal Activiti DMN Engine library")
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
        dmnEngine = DmnEngineConfiguration.createStandaloneInMemDmnEngineConfiguration().buildDmnEngine();
        
        //deploy dmn file in engine
        File dmnFile = new File(Utils.revealLocalFile(dmnFilePath));
		DmnDeployment deployment = dmnEngine.getDmnRepositoryService().createDeployment().addInputStream(dmnFile.getName(), new FileInputStream(dmnFile)).deploy();
		//get all decision tables
		dmnDecisionList = dmnEngine.getDmnRepositoryService().createDecisionTableQuery().deploymentId(deployment.getId()).list();
    
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        //https://docs.Activiti.org/manual/7.4/user-guide/dmn-engine/evaluate-decisions/
        //https://docs.Activiti.org/javadoc/Activiti-bpm-platform/7.8/
        //http://www.bpm-guide.de/2016/10/19/5-reasons-to-switch-from-activiti-to-Activiti/
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

        DmnDecisionTable dmnDecision = null;
        for(DmnDecisionTable dmnDecisionTmp : dmnDecisionList)
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
        
        RuleEngineExecutionResult decisionResults = dmnEngine.getDmnRuleService().executeDecisionByKey(dmnDecision.getKey(), variablesMap);
        
        JsonArrayBuilder decisionResultsJsonArray = Json.createArrayBuilder();
        
        //iterate through result and construct response
        Map<String, Object> resultMap = decisionResults.getResultVariables();
        Iterator<String> keySetIterator = resultMap.keySet().iterator();
        while (keySetIterator.hasNext()) {
        	JsonObjectBuilder decisionResultJson = Json.createObjectBuilder();
			String key = (String) keySetIterator.next();
			Object val = resultMap.get(key);
			if(val instanceof String)
                decisionResultJson.add(key, (String)val);
            else if(val instanceof Boolean)
                decisionResultJson.add(key, (Boolean)val);
            else if(val instanceof Integer)
                decisionResultJson.add(key, (Integer)val);
            else if(val instanceof Double)
                decisionResultJson.add(key, (Double)val);
            else if(val==null)
                decisionResultJson.add(key, JsonValue.NULL);
            else
                throw new Exception("The output value of " + key + " have an unrecognized format: " + val.getClass());
			decisionResultsJsonArray.add(decisionResultJson);
		}
      

        JsonObject decisionResultsJson = Json.createObjectBuilder().add("DecisionResultList", decisionResultsJsonArray).build();
        return decisionResultsJson;

    }
    
    @Override
    public void stop() throws Exception {
        dmnFilePath = "";
        dmnEngine = null;
        dmnDecisionList = null;
    }

    public static void main(String[] argv) throws Exception{
        ActivitiDMNEngineConnector connector = new ActivitiDMNEngineConnector();
        Utils.uploadFolder = "";
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("dmnFilePath", Json.createObjectBuilder().add("value", "D:\\ADOXX.ORG\\MY_MICROSERVICE_FRAMEWORK\\TEST\\knowledgeRules.dmn"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("decisionKey", Json.createObjectBuilder().add("value", "DecisionSupportRules"))
                .add("variables", Json.createObjectBuilder().add("value", Json.createObjectBuilder().add("operator", "Francesca").add("problemDescription", "Here’s looking at you, kid (Casablanca, 1942)").add("triggerStage", 8).add("category", "machine").build().toString()))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    
}
*/