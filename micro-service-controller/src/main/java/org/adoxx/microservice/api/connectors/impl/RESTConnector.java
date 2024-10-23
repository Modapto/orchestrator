package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.adoxx.microservice.utils.Utils.HttpResults;

public class RESTConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "REST Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Get data from a REST service")
            .add("de", "Get data from a REST service")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("method", Json.createObjectBuilder()
                .add("name", "Method")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Http connection method: GET/POST/HEAD/OPTIONS/PUT/DELETE/TRACE/CONNECT/PATCH")
                    .add("de", "Http connection method: GET/POST/HEAD/OPTIONS/PUT/DELETE/TRACE/CONNECT/PATCH"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder().add("GET").add("POST").add("HEAD").add("OPTIONS").add("PUT").add("DELETE").add("TRACE").add("CONNECT").add("PATCH"))))
            .add("requestContentType", Json.createObjectBuilder()
                .add("name", "Content Type")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Http request content type")
                    .add("de", "Http request content type"))
                .add("value", ""))
            .add("additionalHeaders", Json.createObjectBuilder()
                .add("name", "Additional Headers")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Additional headers to append to the request, separated by \\n (\"Accept: application/json\\nAuthorization: Basic YTphQGE=\")")
                    .add("de", "Additional headers to append to the request, separated by \\n (\"Accept: application/json\\nAuthorization: Basic YTphQGE=\")"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("endpoint", Json.createObjectBuilder()
                .add("name", "Endpoint")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Url of the REST service")
                    .add("de", "Url of the REST service"))
                .add("value", ""))
            .add("querystring", Json.createObjectBuilder()
                .add("name", "Querystring")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Query string to append to the endpoint (will be escaped)")
                    .add("de", "Query string to append to the endpoint (will be escaped)"))
                .add("value", ""))
            .add("postData", Json.createObjectBuilder()
                .add("name", "POST Data")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Data to post")
                    .add("de", "Data to post"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  dataMIME : 'text/*' / 'application/json' / 'all the other cases'," + "\n"
                + "  dataText / dataJson / dataBase64 : '_PlainText_' / {_JsonObject_} / '_ContentBase64_'," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }

    private String method = "";
    private ArrayList<String[]> htmlHeaderList = null;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        method = startConfiguration.getJsonObject("method")==null?"":startConfiguration.getJsonObject("method").getString("value", "").toUpperCase();
        if(method.isEmpty() || (!method.equals("POST") && !method.equals("GET") && !method.equals("HEAD") && !method.equals("OPTIONS") && !method.equals("PUT") && !method.equals("DELETE") && !method.equals("TRACE") && !method.equals("CONNECT") && !method.equals("PATCH")))
            throw new Exception("Invalid method provided: " + method);
        
        String requestContentType = startConfiguration.getJsonObject("requestContentType")==null?"":startConfiguration.getJsonObject("requestContentType").getString("value", "");
        String additionalHeaders = startConfiguration.getJsonObject("additionalHeaders")==null?"":startConfiguration.getJsonObject("additionalHeaders").getString("value", "");
        
        htmlHeaderList = new ArrayList<String[]>();
        if(!requestContentType.isEmpty())
            htmlHeaderList.add(new String[]{"Content-Type", requestContentType});
        
        if(!additionalHeaders.isEmpty())
            for(String additionalHeader : additionalHeaders.split("\n"))
                htmlHeaderList.add(new String[]{additionalHeader.split(":")[0].trim(), additionalHeader.split(":")[1].trim()});
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String endpoint = configuration.getJsonObject("endpoint")==null?"":configuration.getJsonObject("endpoint").getString("value", "");
        if(endpoint.isEmpty()) throw new Exception("REST endpoint not provided");
        String querystring = configuration.getJsonObject("querystring")==null?"":configuration.getJsonObject("querystring").getString("value", "");
        String postData = configuration.getJsonObject("postData")==null?"":configuration.getJsonObject("postData").getString("value", "");
        
        endpoint = encodeURL(endpoint);
        
        if(!querystring.isEmpty())
            endpoint += (endpoint.contains("?")?"&":"?") + encodeQuery(querystring);
        //if(!querystring.isEmpty())
        //    endpoint += (endpoint.contains("?")?"&":"?") + querystring;
        //endpoint = encodeFull(endpoint);

        HttpResults out = Utils.sendHTTP(endpoint, method, postData.isEmpty()?null:postData, htmlHeaderList, true, true);

        JsonArrayBuilder headerArrayJson = Json.createArrayBuilder();
        for(String headerKey : out.headerMap.keySet())
            for(String value : out.headerMap.get(headerKey))
                headerArrayJson.add(Json.createObjectBuilder().add("name", headerKey!=null?headerKey:"").add("value", value));
        
        if(out.headerMap.get("content-type") == null)
            throw new Exception("Impossible to identify the content-type header");
        String returnedContentType = out.headerMap.get("content-type").get(0);
        
        
        if(returnedContentType.toLowerCase().startsWith("text"))
            //FIXME: when a big html page is returned make more sense to return a Base64 because of the great number of character escaped
            return Json.createObjectBuilder()
                .add("dataMIME", returnedContentType.toLowerCase())
                .add("dataText", new String(out.data, "UTF-8"))
                .add("moreInfo", Json.createObjectBuilder()
                    .add("retrievalTime", Utils.getCurrentTime())
                    .add("headerList", headerArrayJson)
                ).build();
        else if(returnedContentType.toLowerCase().startsWith("application/json")) {
            JsonValue dataJson = null;
            try{
                dataJson = out.data.length!=0 ? Json.createReader(new StringReader(new String(out.data, "UTF-8"))).readValue() : Json.createObjectBuilder().build();
            } catch(Exception ex) {
                throw new Exception("The returned data is not a valid JSON:\n" + new String(out.data, "UTF-8"));
            }
            return Json.createObjectBuilder()
                .add("dataMIME", returnedContentType.toLowerCase())
                .add("dataJson", dataJson)
                .add("moreInfo", Json.createObjectBuilder()
                    .add("retrievalTime", Utils.getCurrentTime())
                    .add("headerList", headerArrayJson)
                ).build();
        } else
            return Json.createObjectBuilder()
                .add("dataMIME", returnedContentType.toLowerCase())
                .add("dataBase64", Utils.base64Encode(out.data))
                .add("moreInfo", Json.createObjectBuilder()
                    .add("retrievalTime", Utils.getCurrentTime())
                    .add("headerList", headerArrayJson)
                ).build();
    }
    
    @Override
    public void stop() throws Exception {
        method = "";
        htmlHeaderList = null;
    }
    
    private String encodeFull(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        return uri.toASCIIString();
    }
    private String encodeQuery(String queryString) throws Exception {
        String ret = "";
        for(String param : queryString.split("&")) {
            String[] key_val = param.split("=", 2);
            ret += URLEncoder.encode(key_val[0], "UTF-8") + (key_val.length!=1 ? "=" + URLEncoder.encode(key_val[1], "UTF-8") : "") + "&";
        }
        if(!ret.isEmpty())
            ret = ret.substring(0, ret.length()-1);
        return ret;
    }
    
    private String encodeURL(String endpoint) throws Exception {
        String[] protocol_url = endpoint.split("//", 2);
        String[] url = protocol_url[1].split("\\?", 2);
        return protocol_url[0] + "//" + URLEncoder.encode(url[0], "UTF-8").replace("%2F", "/").replace("%3A", ":") + ((url.length > 1)?"?"+encodeQuery(url[1]):"");
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        RESTConnector connector = new RESTConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("method", Json.createObjectBuilder().add("value", "GET"))
                .add("requestContentType", Json.createObjectBuilder().add("value", "application/x-www-form-urlencoded"))
                //.add("requestContentType", Json.createObjectBuilder().add("value", "application/json"))
                .add("additionalHeaders", Json.createObjectBuilder().add("value", ""))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("endpoint", Json.createObjectBuilder().add("value", "http://www.google.com:80"))
                //.add("endpoint", Json.createObjectBuilder().add("value", "https://eis.dke.univie.ac.at/places/byAdress/Währingerstrasse/29/200"))
                .add("querystring", Json.createObjectBuilder().add("value", ""))
                .add("postData", Json.createObjectBuilder().add("value", ""))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
