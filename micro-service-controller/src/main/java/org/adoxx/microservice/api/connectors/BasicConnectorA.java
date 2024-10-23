package org.adoxx.microservice.api.connectors;

import javax.json.JsonObject;

/**
 * <h1>BasicConnectorA</h1>
 * This Abstract Class define the most basic connector type.<br>
 * A basic connector must be defined for all this services that do not require initialization and provide an answer only on requests.<br>
 * It is currently not supposed to be used directly.<br>
 * 
 * @author Damiano Falcioni
 */
public abstract class BasicConnectorA {

    /**
     * Must return the name of this connector.<br>
     * @return String The name of the connector
     */
    public abstract String getName();
    
    /**
     * Must return the description of this connector.<br>
     * @return JsonObject The description of the connector in the following format:
     * <pre>
     * {
     *      "landuage identifiery ex: en/it/.." : "description in the specific language",
     *      "another language identifiery" : "description in the specific language",
     *      ...
     * }
     * </pre>
     */
    public abstract JsonObject getDescription();
    
    /**
     * Must return the template for the configuration JSON required by {@link #performCall(JsonObject)}<br>
     * @return JsonObject The configuration template in the following format:
     * <pre>
     * {
     *      "unique name of one property " : {
     *          "name" : "A name identifier for this property",
     *          "description" : {
     *              "landuage identifiery ex: en/it/.." : "property description in the specific language",
     *              "another language identifiery" : "property description in the specific language",
     *              ...
     *          },
     *          "value" : "" //empty field that must be filled only when the configuration for {@link #performCall(JsonObject)} is created
     *      },
     *      "unique name of another property" : {...},
     *      ...
     * }
     * </pre>
     * This is the standard template for a configuration but any JSON can be created, depending on the needs of {@link #performCall(JsonObject)}
     */
    public abstract JsonObject getCallConfigurationTemplate();
    
    /**
     * Call {@link #getCallConfigurationTemplate()}, check if the response is in the valid format and return it.<br>
     * @return JsonObject A JSON object in the format specified in {@link #getCallConfigurationTemplate()}
     * @throws Exception in case of error
     */
    public JsonObject getCallConfigurationTemplateSafe() throws Exception {
        JsonObject ret = getCallConfigurationTemplate();
        checkConfigurationTemplateFormat(ret);
        return ret;
    }
    
    /**
     * Return the description of the possibile outputs for this connector<br>
     * @return the description of the possibile outputs for this connector
     * @throws Exception in case of error
     */
    public abstract String getOutputDescription() throws Exception ;
    
    /**
     * Must perform the calling operation and return the response.<br>
     * @param callConfiguration The configuration JSON as returned by {@link #getCallConfigurationTemplate()}, but filled with values
     * @return JsonObject A JSON object in one of this two formats:
     * In case the connector return a structured output
     * <pre>
     * {
     *     "columns" : ["id of one output field", "id of another output field", ...],
     *     "data" : [{
     *         "id of one output field" : "first value for this output filed",
     *         "id of another output field" : "first value for this output field",
     *         ...
     *     }, {
     *         "id of one output field" : "second value for this output field",
     *         "id of another output field" : "second value for this output field",
     *         ...
     *     },
     *     ...
     *     ],
     *     "moreInfo" : {
     *          //here you can have whathever kind of information
     *     }
     * }
     * </pre>
     * In case the connector used return an unstructured output
     * <pre>
     * {
     *     "dataFormat" : "here you will find the MIME type of the data. Ex: application/json",
     *     "data" : "If the dataFormat is of type 'text/...' here you will find the unstructured data returned by the microservice, otherwise will be empty",
     *     "dataBase64" : "If the dataFormat is NOT of type 'text/...' here you will find the unstructured data returned by the microservice encoded in BASE64, otherwise will be empty"
     *     "moreInfo" : {
     *         //here you can have whathever kind of information
     *     }
     * }
     * </pre>
     * @throws Exception in case of error
     */
    public abstract JsonObject performCall(JsonObject callConfiguration) throws Exception;
    
    /**
     * Call {@link #performCall(JsonObject)}, check if the response is in the valid format and return it.<br>
     * @param callConfiguration The configuration JSON as returned by {@link #getCallConfigurationTemplate()}, but filled with values
     * @return JsonObject A JSON object in the format specified in {@link #performCall(JsonObject)}
     * @throws Exception in case of error
     */
    public JsonObject performCallSafe(JsonObject callConfiguration) throws Exception{
        JsonObject connectorOutput = performCall(callConfiguration);
        //checkResponseStructure(connectorOutput);
        return connectorOutput;
    }
    
    protected void checkConfigurationTemplateFormat(JsonObject response) throws Exception {
        for(String inputKey : response.keySet()) {
            JsonObject input = response.getJsonObject(inputKey);
            if(input.getString("name", "").isEmpty())
                throw new Exception("The returned json of the configuration template \"" + response + "\" must contain a 'name' json string");
            if(input.getJsonObject("description") == null)
                throw new Exception("The returned json of the configuration template \"" + response + "\" must contain a 'description' json object");
            if(input.getJsonObject("description").getString("en") == null)
                throw new Exception("The returned json of the configuration template \"" + response + "\" must contain at least an 'en' json string into the 'description' json object");
            if(input.getString("value") == null)
                throw new Exception("The returned json of the configuration template \"" + response + "\" must contain a 'value' json string");
        }
    }
    
    protected static void checkResponseStructure(JsonObject response) throws Exception {
        if(response.getJsonString("dataMIME") == null)
            throw new Exception("The returned json of the connector must contain a 'dataMIME' string: " + response);
        String mime = response.getJsonString("dataMIME").getString();
        if(mime.startsWith("text/") && response.getJsonString("dataText") == null)
            throw new Exception("The returned json of the connector must contain a 'dataText' string when the mime is of type text/*: " + response);
        if(mime.startsWith("application/json") && response.getJsonObject("dataJson") == null)
            throw new Exception("The returned json of the connector must contain a 'dataJson' object when the mime is of type application/json: " + response);
        if(!mime.startsWith("application/json") && !mime.startsWith("text/") && response.getJsonString("dataBase64") == null)
            throw new Exception("The returned json of the connector must contain a 'dataBase64' json string when the mime is neither text/* or application/json: " + response);
        if(response.getJsonObject("moreInfo") == null)
            throw new Exception("The returned json of the connector must contain a 'moreInfo' json object" + response);
        
        /*
        if(response.getJsonArray("columns") != null) {
            //if(response.getJsonArray("columns") == null)
            //    throw new Exception("The returned json of the connector, \"" + response + "\" must contain a 'columns' json array");
            if(response.getJsonArray("data") == null)
                throw new Exception("The returned json of the connector, \"" + response + "\" must contain a 'data' json array");
            if(response.getJsonObject("moreInfo") == null)
                throw new Exception("The returned json of the connector, \"" + response + "\" must contain a 'moreInfo' json object");
            
            ArrayList<String> columnList = new ArrayList<String>();
            for(JsonValue column : response.getJsonArray("columns"))
                columnList.add(((JsonString)column).getString());
            
            for(JsonValue data : response.getJsonArray("data")){
                if(!(data instanceof JsonObject))
                    throw new Exception("Expected Json Object; returned : " + data.getValueType().toString() + " -> " + data.toString());
                JsonObject dataO = (JsonObject) data;
                for(String column : columnList)
                    if(dataO.getJsonString(column) == null)
                        throw new Exception("Json String " + column + " not present in the object " + dataO.toString());
            }
         } else {
             if(response.getJsonString("dataMIME") == null)
                 throw new Exception("The returned json of the connector must contain a 'dataMIME' string: " + response);
             String mime = response.getJsonString("dataMIME").getString();
             if(mime.startsWith("text/") && response.getJsonString("dataText") == null)
                 throw new Exception("The returned json of the connector must contain a 'dataText' string when the mime is of type text/*: " + response);
             if(mime.startsWith("application/json") && response.getJsonObject("dataJson") == null)
                 throw new Exception("The returned json of the connector must contain a 'dataJson' object when the mime is of type application/json: " + response);
             if(!mime.startsWith("application/json") && !mime.startsWith("text/") && response.getJsonString("dataBase64") == null)
                 throw new Exception("The returned json of the connector must contain a 'dataBase64' json string when the mime is neither text/* or application/json: " + response);
             if(response.getJsonObject("moreInfo") == null)
                 throw new Exception("The returned json of the connector must contain a 'moreInfo' json object" + response);
         }
         */
    }
}
