package org.adoxx.microservice.api.connectors.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.adoxx.microservice.api.MicroserviceController;
import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class DashboardConnector extends SyncConnectorA{

    private JsonArray modelList = null;
    
    @Override
    public String getName() {
        return "Dashboard Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "A connector that calculate and return the KPIs and Goals specified in the dashboard json provided")
            .add("de", "A connector that calculate and return the KPIs and Goals specified in the dashboard json provided")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("dashboardJSONPath", Json.createObjectBuilder()
                .add("name", "Dashboard JSON Path")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Uri/Path of the dashboard configuration JSON file (OPTIONAL)")
                    .add("de", "Uri/Path of the dashboard configuration JSON file (OPTIONAL)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("requireUpload", true)))
            .add("dashboardJSON", Json.createObjectBuilder()
                .add("name", "Dashboard JSON Content")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Content of the dashboard configuration JSON file (OPTIONAL)")
                    .add("de", "Content of the dashboard configuration JSON file (OPTIONAL)"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("filterIdList", Json.createObjectBuilder()
                .add("name", "Filtered KPI/Goal Id List")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The id of all the KPIs and Goals to calculate in JSON array format. (optional)")
                    .add("de", "The id of all the KPIs and Goals to calculate in JSON array format. (optional)"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
            + "{" + "\n"
            + "  kpiValues: {" + "\n"
            + "    kpi_id_1: {" + "\n"
            + "      kpiValue: {...}," + "\n"
            + "      targetRangeValue: {...}," + "\n"
            + "      alertRangeValueList: [...]" + "\n"
            + "    }, ..." + "\n"
            + "  }," + "\n"
            + "  goalValues: {" + "\n"
            + "    goal_id_1: {" + "\n"
            + "      goalValue: {...}" + "\n"
            + "    }, ..." + "\n"
            + "  }," + "\n"
            + "  moreInfo: {" + "\n"
            + "    retrievalTime: ''" + "\n"
            + "  }," + "\n"
            + "}" + "\n"
        ;
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        JsonObject dashboardJSON = null;
        String dashboardJSONPath = startConfiguration.getJsonObject("dashboardJSONPath")==null?"":startConfiguration.getJsonObject("dashboardJSONPath").getString("value", "");
        if(!dashboardJSONPath.isEmpty()) {
            try{
                InputStream input = dashboardJSONPath.startsWith("http")?new URL(dashboardJSONPath).openStream():new FileInputStream(new File(Utils.revealLocalFile(dashboardJSONPath)));
                dashboardJSON = Json.createReader(new InputStreamReader(input)).readObject();
            }catch(Exception ex) {
                throw new Exception("The file at " + dashboardJSONPath + " is not in a correct JSON format:\n" + ex.getMessage());
            }
        }
        
        String dashboardJSONContent = startConfiguration.getJsonObject("dashboardJSON")==null?"":startConfiguration.getJsonObject("dashboardJSON").getString("value", "");
        if(!dashboardJSONContent.isEmpty()) {
            try{
                dashboardJSON = Json.createReader(new StringReader(dashboardJSONContent)).readObject();
            }catch(Exception ex) {
                throw new Exception("The content provided is not in a correct JSON format:\n" + ex.getMessage());
            }
        }
        
        if(dashboardJSON == null) throw new Exception("dashboardJSONPath or dashboardJSON not provided");
        
        modelList = dashboardJSON.getJsonArray("modelList");
        if(modelList == null) {
            if(dashboardJSON.getJsonObject("dashboardStatus") != null && dashboardJSON.getJsonObject("dashboardStatus").getJsonObject("kpiModel") != null)
                modelList = dashboardJSON.getJsonObject("dashboardStatus").getJsonObject("kpiModel").getJsonArray("modelList");
        }
        if(modelList == null) throw new Exception("Incorrect JSON: modelList array not found");
    }
    
    private HashMap<String, JsonObject> calculatedKPIs;
    private HashMap<String, JsonObject> calculatedGoals;
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String filterIdListString = configuration.getJsonObject("filterIdList")==null?"":configuration.getJsonObject("filterIdList").getString("value", "");
        JsonArray filterIdList = null;
        try{
            filterIdList = filterIdListString.equals("")?Json.createArrayBuilder().build():Json.createReader(new StringReader(filterIdListString)).readArray();
        }catch(Exception ex) {
            throw new Exception("filterIdList is not a JSON array: "+filterIdListString+"\n"+ex.getMessage());
        }
        
        calculatedKPIs = new HashMap<String, JsonObject>();
        calculatedGoals = new HashMap<String, JsonObject>();
        
        
        for(JsonValue modelVal : modelList) {
            JsonObject modelJson = modelVal.asJsonObject();
            JsonArray kpiList = modelJson.getJsonArray("kpiList");
            for(JsonValue kpiVal : kpiList) {
                JsonObject kpiJson = kpiVal.asJsonObject();
                String kpiId = kpiJson.getString("id");
                if(!filterIdList.isEmpty() && !filterIdList.contains(kpiJson.get("id")))
                    continue;
                if(calculatedKPIs.containsKey(kpiId) && calculatedKPIs.get(kpiId)==null)
                    throw new Exception("Incorrect model: a dependency loop as been identified in the model for the KPI " + kpiId);
                if(!calculatedKPIs.containsKey(kpiId))
                    calculatedKPIs.put(kpiId, calculateKPI(kpiJson));
            }
            
            JsonArray goalList = modelJson.getJsonArray("goalList");
            for(JsonValue goalVal : goalList) {
                JsonObject goalJson = goalVal.asJsonObject();
                String goalId = goalJson.getString("id");
                if(!filterIdList.isEmpty() && !filterIdList.contains(goalJson.get("id")))
                    continue;
                if(calculatedGoals.containsKey(goalId) && calculatedGoals.get(goalId)==null)
                    throw new Exception("Incorrect model: a dependency loop as been identified in the model for the Goal " + goalId);
                if(!calculatedGoals.containsKey(goalId))
                    calculatedGoals.put(goalId, calculateGoal(goalJson));
            }
        }
        
        JsonObjectBuilder kpiValuesBuilder = Json.createObjectBuilder();
        for(Entry<String, JsonObject> entry: calculatedKPIs.entrySet())
            kpiValuesBuilder.add(entry.getKey(), entry.getValue());
        
        JsonObjectBuilder goalValuesBuilder = Json.createObjectBuilder();
        for(Entry<String, JsonObject> entry: calculatedGoals.entrySet())
            goalValuesBuilder.add(entry.getKey(), entry.getValue());
        
        JsonObject ret = Json.createObjectBuilder()
            .add("kpiValues", kpiValuesBuilder)
            .add("goalValues", goalValuesBuilder)
            .add("moreInfo", Json.createObjectBuilder()
                .add("retrievalTime", Utils.getCurrentTime())
            ).build();
        return ret;
    }
    
    private JsonObject calculateGoal(JsonObject goalJson) throws Exception {
        String goalId = goalJson.getString("id");
        calculatedGoals.put(goalId, null);
        
        JsonArrayBuilder requiredGoalValueListBuilder = Json.createArrayBuilder();
        for(JsonValue requiredGoalIdVal : goalJson.getJsonArray("requiredGoalIdList")) {
            String requiredGoalId = ((JsonString) requiredGoalIdVal).getString();
            JsonObject requiredGoal = find("goalList", requiredGoalId);
            if(calculatedGoals.containsKey(requiredGoalId) && calculatedGoals.get(requiredGoalId)==null)
                throw new Exception("Incorrect model: a dependency loop as been identified in the model for the Goal " + goalId + " requiring goal " + requiredGoalId);
            if(!calculatedGoals.containsKey(requiredGoalId))
                calculatedGoals.put(requiredGoalId, calculateGoal(requiredGoal));
            
            String error = calculatedGoals.get(requiredGoalId).getString("error", "");
            if (!error.isEmpty())
                throw new Exception("The required sub-goal with id " + requiredGoal.getString("id") + " have an error: " + error);
            
            requiredGoalValueListBuilder.add(Json.createObjectBuilder()
                .add("id", requiredGoal.getString("id"))
                .add("name", requiredGoal.getString("name"))
                .add("description", requiredGoal.getString("description"))
                .add("value", calculatedGoals.get(requiredGoalId).getJsonObject("goalValue"))
            );
        }
        
        JsonArrayBuilder requiredKpiValueListBuilder = Json.createArrayBuilder();
        for(JsonValue requiredKpiIdVal : goalJson.getJsonArray("requiredKpiIdList")) {
            String requiredKpiId = ((JsonString) requiredKpiIdVal).getString();
            JsonObject requiredKpi = find("kpiList", requiredKpiId);
            if(calculatedKPIs.containsKey(requiredKpiId) && calculatedKPIs.get(requiredKpiId)==null)
                throw new Exception("Incorrect model: a dependency loop as been identified in the model for the Goal " + goalId + " requiring kpi " + requiredKpiId);
            if(!calculatedKPIs.containsKey(requiredKpiId))
                calculatedKPIs.put(requiredKpiId, calculateKPI(requiredKpi));
            
            String error = calculatedKPIs.get(requiredKpiId).getString("error", "");
            if (!error.isEmpty())
                throw new Exception("The required sub-kpi with id " + requiredKpi.getString("id") + " have an error: " + error);
            
            requiredKpiValueListBuilder.add(Json.createObjectBuilder()
                .add("id", requiredKpi.getString("id"))
                .add("name", requiredKpi.getString("name"))
                .add("description", requiredKpi.getString("description"))
                .add("fields", requiredKpi.getJsonArray("fields"))
                .add("value", calculatedKPIs.get(requiredKpiId).getJsonObject("kpiValue"))
            );
        }
        
        JsonObject goalValue = null;
        String connectedAlgorithmId = goalJson.getString("connectedAlgorithmId", "");
        if(!connectedAlgorithmId.equals("")) {
            JsonObject connectedAlgorithmJson = find("algorithmList", connectedAlgorithmId);
            HashMap<String, Object> jsEngineParamenters = new HashMap<String, Object>();
            String code = connectedAlgorithmJson.getString("code", "");
            jsEngineParamenters.put("_code", code);
            jsEngineParamenters.put("_requiredGoalValueList", requiredGoalValueListBuilder.build().toString());
            jsEngineParamenters.put("_requiredKpiValueList", requiredKpiValueListBuilder.build().toString());
            String alg = "var requiredGoalValueList = JSON.parse(_requiredGoalValueList);\n";
            alg += "var requiredKpiValueList = JSON.parse(_requiredKpiValueList);\n";
            alg += "var fnInitCode = \"var allDependencySucceed = function(aggregationType){ var status=1;var failedGoals='';var failedKPIs='';for(var i=0;i<requiredGoalValueList.length;i++){status=requiredGoalValueList[i].value.status;if(status<=0){failedGoals+=requiredGoalValueList[i].name+'; ';break;}} if(status>0) for(var i=0;i<requiredKpiValueList.length;i++){status=requiredKpiValueList[i].value.targetRangeAlgorithmResult!=null?requiredKpiValueList[i].value.targetRangeAlgorithmResult.status:0;if(status<0){failedKPIs+=requiredKpiValueList[i].name+'; ';break;}} return{status:status,moreInfo:{failedGoals:failedGoals,failedKPIs:failedKPIs}};};\";\n";
            alg += "fnInitCode += \"var oneDependencySucceed = function(aggregationType){ var status=-1;var failedGoals='';var failedKPIs='';for(var i=0;i<requiredGoalValueList.length;i++){if(requiredGoalValueList[i].value.status<=0) failedGoals+=requiredGoalValueList[i].name+'; ';if(requiredGoalValueList[i].value.status>0) status=requiredGoalValueList[i].value.status;} for(var i=0;i<requiredKpiValueList.length;i++){if(requiredKpiValueList[i].value.targetRangeAlgorithmResult!=null) if(requiredKpiValueList[i].value.targetRangeAlgorithmResult.status<=0) failedKPIs+=requiredKpiValueList[i].name+'; ';else status=requiredKpiValueList[i].value.targetRangeAlgorithmResult.status;} return{status:status,moreInfo:{failedGoals:failedGoals,failedKPIs:failedKPIs}}; };\";";
            alg += "var algF = new Function('requiredGoalValueList', 'requiredKpiValueList', fnInitCode + '\\n' + _code);\n";
            alg += "var goalMeasure = algF(requiredGoalValueList, requiredKpiValueList);\n";
            alg += "JSON.stringify(goalMeasure);\n";
            Object javascriptOutput = Utils.javascriptSafeEval(jsEngineParamenters, alg, false);
            goalValue = Json.createReader(new StringReader((String)javascriptOutput)).readObject();
        }
        
        if(connectedAlgorithmId.equals(""))
            throw new Exception("connectedAlgorithmId can not be empty");
        if(goalValue == null)
            throw new Exception("goalValue can not be null");
        try{
            goalValue.getJsonNumber("status");
        }catch(Exception ex) {
            throw new Exception("The goal have an incorrect format: Expected {status:number} Obtained:\n"+goalValue.toString());
        }
        
        return Json.createObjectBuilder()
            .add("name", goalJson.getString("name"))
            .add("description", goalJson.getString("description"))
            .add("goalValue", goalValue)
            .add("moreInfo", goalJson.getJsonObject("moreInfo"))
            .build();
    }
    
    private JsonObject calculateKPI(JsonObject kpiJson) throws Exception {
        String kpiId = kpiJson.getString("id");
        calculatedKPIs.put(kpiId, null);
        
        JsonArrayBuilder requiredKpiValueListBuilder = Json.createArrayBuilder();
        for(JsonValue requiredKpiIdVal : kpiJson.getJsonArray("requiredKpiIdList")) {
            String requiredKpiId = ((JsonString) requiredKpiIdVal).getString();
            JsonObject requiredKpi = find("kpiList", requiredKpiId);
            if(calculatedKPIs.containsKey(requiredKpiId) && calculatedKPIs.get(requiredKpiId)==null)
                throw new Exception("Incorrect model: a dependency loop as been identified in the model for the KPI " + kpiId + " requiring kpi " + requiredKpiId);
            if(!calculatedKPIs.containsKey(requiredKpiId))
                calculatedKPIs.put(requiredKpiId, calculateKPI(requiredKpi));
            
            String error = calculatedKPIs.get(requiredKpiId).getString("error", "");
            if (!error.isEmpty())
                throw new Exception("The required sub-kpi with id " + requiredKpi.getString("id") + " have an error: " + error);
            
            requiredKpiValueListBuilder.add(Json.createObjectBuilder()
                .add("id", requiredKpi.getString("id"))
                .add("name", requiredKpi.getString("name"))
                .add("description", requiredKpi.getString("description"))
                .add("fields", requiredKpi.getJsonArray("fields"))
                .add("value", calculatedKPIs.get(requiredKpiId).getJsonObject("kpiValue"))
            );
        }
        
        JsonObject kpiValue = null;
        String connectedDataSourceId = kpiJson.getString("connectedDataSourceId", "");
        if(!connectedDataSourceId.equals("")) {
            JsonObject connectedDataSourceJson = find("dataSourceList", connectedDataSourceId);
            String microserviceId = connectedDataSourceJson.getString("microserviceId", "");
            if(microserviceId.equals(""))
                throw new Exception("microserviceId can not be empty in a datasource definition");
            String operationId = connectedDataSourceJson.getString("microserviceOperationId", "");
            JsonObject microserviceInputs = connectedDataSourceJson.getJsonObject("microserviceInputs");
            kpiValue = MicroserviceController.unique().callMicroserviceForced(microserviceId, operationId, microserviceInputs);
        }
        
        String connectedAlgorithmId = kpiJson.getString("connectedAlgorithmId", "");
        if(!connectedAlgorithmId.equals("")) {
            JsonObject connectedAlgorithmJson = find("algorithmList", connectedAlgorithmId);
            HashMap<String, Object> jsEngineParamenters = new HashMap<String, Object>();
            String code = connectedAlgorithmJson.getString("code", "");
            jsEngineParamenters.put("_code", code);
            jsEngineParamenters.put("_dataSourceOutput", kpiValue==null?null:kpiValue.toString());
            jsEngineParamenters.put("_requiredKpiValueList", requiredKpiValueListBuilder.build().toString());
            String alg = "var dataSourceOutput = _dataSourceOutput==null?null:JSON.parse(_dataSourceOutput);\n";
            alg += "var requiredKpiValueList = JSON.parse(_requiredKpiValueList);\n";
            alg += "var fnInitCode = \"function combine(fieldAggList){fieldAggList=fieldAggList==null?[]:fieldAggList; var combineMethods={sum:function(field,rowNum){var sum=0;requiredKpiValueList.forEach(function(requiredKpiValue){sum+=requiredKpiValue.value.data.length>rowNum?Number(requiredKpiValue.value.data[rowNum][field]):0;});return sum;},avg:function(field,rowNum){var avg=0;requiredKpiValueList.forEach(function(requiredKpiValue){avg+=requiredKpiValue.value.data.length>rowNum?Number(requiredKpiValue.value.data[rowNum][field]):0;});return(avg/requiredKpiValueList.length);},min:function(field,rowNum){var min=null;requiredKpiValueList.forEach(function(requiredKpiValue){var val=requiredKpiValue.value.data.length>rowNum?Number(requiredKpiValue.value.data[rowNum][field]):null;min=min==null?val:val!=null&&val<min?val:min;});return min;},max:function(field,rowNum){var max=null;requiredKpiValueList.forEach(function(requiredKpiValue){var val=requiredKpiValue.value.data.length>rowNum?Number(requiredKpiValue.value.data[rowNum][field]):null;max=max==null?val:val!=null&&val>max?val:max;});return max;},first:function(field,rowNum){return requiredKpiValueList.length!=0&&requiredKpiValueList[0].value.data.length!=0?requiredKpiValueList[0].value.data[rowNum][field]:null;}};var ret={columns:[],data:[],moreInfo:{}};var maxRow=requiredKpiValueList[0]?requiredKpiValueList[0].value.data.length:0;for(var rowNum=0;rowNum<maxRow;rowNum++){var obj={};fieldAggList.forEach(function(fieldAgg){obj[fieldAgg.field]=combineMethods[fieldAgg.aggregation](fieldAgg.field,rowNum);});ret.data.push(obj);} fieldAggList.forEach(function(fieldAgg){ret.columns.push(fieldAgg.field);});return ret;}\";\n";
            alg += "fnInitCode += \"function calculateFields(functions,aliases){var _getKPI=function(id){for(var i=0;i<requiredKpiValueList.length;i++) if(requiredKpiValueList[i].id==id) return requiredKpiValueList[i];throw'Impossible to find the required kpi '+id;};var ret={columns:[],data:[],moreInfo:{}};Object.keys(functions).forEach(function(field){ret.columns.push(field);ret.moreInfo[field]=functions[field];});var dataLength=0;for(var i=0;i<requiredKpiValueList.length;i++) if(requiredKpiValueList[i].value.data.length>dataLength) dataLength=requiredKpiValueList[i].value.data.length;for(var i=0;i<dataLength;i++){var toEval='';Object.keys(aliases).forEach(function(alias){toEval+='var '+alias+'=_getKPI(\\\"'+aliases[alias]+'\\\").value.data['+i+']; ';});toEval+='var evalRet={';Object.keys(functions).forEach(function(field){toEval+='\\\"'+field+'\\\": function () { try{ return '+functions[field]+'; } catch(e) { return e; } }(), ';});toEval+='};';eval(toEval);var retData={};Object.keys(functions).forEach(function(field){retData[field]=evalRet[field];});ret.data.push(retData);} return ret;}\";\n";
            alg += "fnInitCode += \"function round(x,digits){return parseFloat(x.toFixed(digits))}\";";
            alg += "var algF = new Function('dataSourceOutput', 'requiredKpiValueList', fnInitCode + '\\n' + _code);\n";
            alg += "var kpiMeasure = algF(dataSourceOutput, requiredKpiValueList);\n";
            alg += "JSON.stringify(kpiMeasure);\n";
            Object javascriptOutput = Utils.javascriptSafeEval(jsEngineParamenters, alg, false);
            kpiValue = Json.createReader(new StringReader((String)javascriptOutput)).readObject();
        }
        
        if(connectedDataSourceId.equals("") && connectedAlgorithmId.equals(""))
            throw new Exception("connectedDataSourceId and connectedAlgorithmId can not be both empty");    
        if(kpiValue == null)
            throw new Exception("kpiValue can not be null");
        try{
            kpiValue.getJsonArray("columns").isEmpty();
            kpiValue.getJsonArray("data").isEmpty();
        }catch(Exception ex) {
            throw new Exception("The kpi measure have an incorrect format: Expected {columns:[...], data:[...]} Obtained:\n"+kpiValue.toString());
        }
        for(int i=0;i<kpiJson.getJsonArray("fields").size();i++)
            if(!kpiValue.getJsonArray("columns").contains(kpiJson.getJsonArray("fields").getJsonObject(i).get("name")))
                throw new Exception("The kpi value do not contain the required field " + kpiJson.getJsonArray("fields").getJsonObject(i).get("name").toString());
        
        String targetRangeAlgorithmId = kpiJson.getString("targetRangeAlgorithmId", "");
        JsonObject targetRangeValue = null;
        if(!targetRangeAlgorithmId.equals("")) {
            JsonObject targetRangeAlgorithmJson = find("algorithmList", targetRangeAlgorithmId);
            HashMap<String, Object> jsEngineParamenters = new HashMap<String, Object>();
            String code = targetRangeAlgorithmJson.getString("code", "");
            jsEngineParamenters.put("_code", code);
            jsEngineParamenters.put("_moreInfo", kpiJson.getJsonObject("moreInfo").toString());
            jsEngineParamenters.put("_kpiMeasure", kpiValue.toString());
            String alg = "var fnRangeEvalTarget = \"var evaluateRange=function(range){if(kpiValue.data.length==0){return{status:0,statusList:[],moreInfo:{details:'No data available'}};}var statusList=[];var status=0;var varDec;for(var i=0;i<kpiValue.data.length;i++){varDec='';kpiValue.columns.forEach(function(key){varDec+='var '+key+'=kpiValue.data['+i+'].'+key+'; ';});try{var evalF=new Function('kpiValue',varDec+'return '+range+';');var evalRet=evalF(kpiValue);statusList.push(evalRet?1:-1);if(i==0)status=evalRet?1:-1;}catch(e){statusList.push(0);console.log(e);}}return{status:status,statusList:statusList,moreInfo:{rule:range}};};\";\n";
            alg += "var algTargetRangeF = new Function('kpiMoreInfo', 'kpiValue', fnRangeEvalTarget + _code);\n";
            alg += "var targetRangeAlgorithmResult = algTargetRangeF(JSON.parse(_moreInfo), JSON.parse(_kpiMeasure));\n";
            alg += "out(targetRangeAlgorithmResult);\n";
            Object javascriptOutput = Utils.javascriptSafeEval(jsEngineParamenters, alg, false);
            targetRangeValue = Json.createReader(new StringReader((String)javascriptOutput)).readObject();
        }
        
        JsonArrayBuilder alertRangeValueListBuilder = Json.createArrayBuilder();
        for(JsonValue alertRangeAlgorithmId : kpiJson.getJsonArray("alertRangeAlgorithmIdList")) {
            JsonObject alertRangeAlgorithmJson = find("algorithmList", ((JsonString)alertRangeAlgorithmId).getString());
            HashMap<String, Object> jsEngineParamenters = new HashMap<String, Object>();
            String code = alertRangeAlgorithmJson.getString("code", "");
            jsEngineParamenters.put("_code", code);
            jsEngineParamenters.put("_moreInfo", kpiJson.getJsonObject("moreInfo").toString());
            jsEngineParamenters.put("_kpiMeasure", kpiValue.toString());
            String alg = "var fnRangeEvalAlert = \"var evaluateRange=function(range,successColor){if(kpiValue.data.length==0){return{status:0,statusList:[],moreInfo:{details:'No data available'}};} var statusList=[];var status=0;var varDec;for(var i=0;i<kpiValue.data.length;i++){varDec='';kpiValue.columns.forEach(function(key){varDec+='var '+key+'=kpiValue.data['+i+'].'+key+'; ';});try{var evalF=new Function('kpiValue',varDec+'return '+range+';');var evalRet=evalF(kpiValue);statusList.push(evalRet?1:-1);if(i==0) status=evalRet?1:-1;}catch(e){statusList.push(0);console.log(e);}} var ret={status:status,statusList:statusList,moreInfo:{rule:range}};if(successColor){ret.widgetSpecificCustomization={alertSuccessColor:successColor&&successColor!=''?successColor:'yellow'};} return ret;};\";\n";
            alg += "var algAlertRangeF = new Function('kpiMoreInfo', 'kpiValue', fnRangeEvalAlert + _code);\n";
            alg += "var alertRangeAlgorithmResult = algAlertRangeF(JSON.parse(_moreInfo), JSON.parse(_kpiMeasure));\n";
            alg += "out(alertRangeAlgorithmResult);\n";
            Object javascriptOutput = Utils.javascriptSafeEval(jsEngineParamenters, alg, false);
            JsonObject alertRangeValue = Json.createReader(new StringReader((String)javascriptOutput)).readObject();
            alertRangeValueListBuilder.add(alertRangeValue);
        }
        
        return Json.createObjectBuilder()
            .add("name", kpiJson.getString("name"))
            .add("description", kpiJson.getString("description"))
            .add("fields", kpiJson.getJsonArray("fields"))
            .add("kpiValue", kpiValue)
            .add("targetRangeValue", targetRangeValue==null?JsonValue.NULL:targetRangeValue)
            .add("alertRangeValueList", alertRangeValueListBuilder)
            .add("moreInfo", kpiJson.getJsonObject("moreInfo"))
            .build();
    }
    
    private JsonObject find(String arrayKey, String id) throws Exception {
        for(JsonValue modelVal : modelList) {
            JsonObject modelJson = modelVal.asJsonObject();
            JsonArray list = modelJson.getJsonArray(arrayKey);
            for(JsonValue val : list) {
                JsonObject json = val.asJsonObject();
                if(json.getString("id", "").equals(id))
                    return json;
            }
        }
        throw new Exception("Impossible to find the object with id " + id + " inside " + arrayKey);
    }
    
    @Override
    public void stop() throws Exception {
        modelList = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        DashboardConnector connector = new DashboardConnector();
        Utils.uploadFolder = "";
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("dashboardJSON", Json.createObjectBuilder().add("value", ""))
                .add("dashboardJSONPath", Json.createObjectBuilder().add("value", "D:\\ADOXX.ORG\\DASHBOARD\\dashboard_src\\src\\main\\resources\\fca_model_sample.json"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("filterIdList", Json.createObjectBuilder().add("value", "[]"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
