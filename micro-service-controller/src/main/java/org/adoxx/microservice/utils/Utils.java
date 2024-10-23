package org.adoxx.microservice.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.script.ScriptEngine;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.adoxx.microservice.api.MicroserviceController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Utils {
    
    public static String getTime(long time){
        //2017-03-16 17:00:00
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(time));
    }
    
    public static String getCurrentTime(){
        //2017-03-16 17:00:00
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
    
    public static class HttpResults{
        public byte[] data;
        public Map<String, List<String>> headerMap;
    }
    
    public static HttpResults sendHTTP(String url, String mode, String dataToSend, ArrayList<String[]> htmlHeaderList, boolean ignoreSSLSelfSigned, boolean ignoreSSLWrongCN) throws Exception{
        
        System.setProperty("java.net.useSystemProxies", "true");
        
        if(ignoreSSLSelfSigned){
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
                }, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        
        if(ignoreSSLWrongCN){
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, javax.net.ssl.SSLSession session) { return true; }
            });
        }
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        byte[] output = new byte[0];
        Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
        try {
            connection.setRequestMethod(mode.toUpperCase());
            
            if(htmlHeaderList != null)
                for(String[] htmlHeader:htmlHeaderList)
                    if(htmlHeader.length==2)
                        connection.setRequestProperty(htmlHeader[0], htmlHeader[1]);
            
            if(dataToSend!=null && !dataToSend.isEmpty()){
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Length", "" + Integer.toString(dataToSend.getBytes("UTF-8").length));
                try(DataOutputStream wr = new DataOutputStream(connection.getOutputStream())){
                    wr.write(dataToSend.getBytes("UTF-8"));
                    wr.flush();
                }
            }
            
            try {
                output = toByteArray(connection.getInputStream());
            } catch(Exception ex) {
                throw new Exception("Error in the HTTP connection. Stream response:\n" + (connection.getErrorStream()==null?"":Utils.toString(connection.getErrorStream())), ex);
            }
            
            for(Entry<String, List<String>> entry : connection.getHeaderFields().entrySet())
                if(entry.getKey() == null)
                    headerMap.put(null, entry.getValue());
                else
                    headerMap.put(entry.getKey().toLowerCase(), entry.getValue());
        } finally {
            connection.disconnect();
        }
        
        HttpResults ret = new HttpResults();
        ret.data = output;
        ret.headerMap = headerMap;
        return ret;
    }
    
    public static Document sendSOAP(String url, String methodNamespace, String methodName, String soapAction, ArrayList<String[]> parameterList, ArrayList<String[]> additionalHtmlHeaderList) throws Exception {
        Document docXml = createNewDocument();
        Element envelopeEl = docXml.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "Envelope");
        Element headerEl = docXml.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "Header");
        Element bodyEl = docXml.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        Element methodEl = docXml.createElementNS(methodNamespace, methodName);
        docXml.appendChild(envelopeEl);
        envelopeEl.appendChild(headerEl);
        envelopeEl.appendChild(bodyEl);
        bodyEl.appendChild(methodEl);
        
        for(String[] parameter:parameterList) {
            if(parameter.length!=2) throw new Exception("Incorrect parameters");
            Element paramEl = docXml.createElementNS(methodNamespace, parameter[0]);
            paramEl.appendChild(docXml.createTextNode(parameter[1]));
            methodEl.appendChild(paramEl);
        }
        String doc = getStringFromXmlDoc(docXml);
        ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
        htmlHeaderList.add(new String[]{"Content-Type", "text/xml; charset=UTF-8"});
        htmlHeaderList.add(new String[]{"SOAPAction", soapAction==null?"":soapAction});
        if(additionalHtmlHeaderList != null)
            for(String[] additionalHtmlHeader : additionalHtmlHeaderList)
                if(additionalHtmlHeader.length == 2)
                    if(!additionalHtmlHeader[0].toLowerCase().equals("soapaction") && !additionalHtmlHeader[0].toLowerCase().equals("content-type"))
                        htmlHeaderList.add(new String[]{additionalHtmlHeader[0], additionalHtmlHeader[1]});
        
        HttpResults serviceResponse = sendHTTP(url, "POST", doc, htmlHeaderList, true, true);
        Document responseXml = null;
        try {
            responseXml = getXmlDocFromBytes(serviceResponse.data);
        } catch(Exception ex) { throw new Exception("The returned data is an invalid XML:\n" + new String(serviceResponse.data, "UTF-8"));}
        
        NodeList faultList = responseXml.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/","Fault");
        if(faultList.getLength() != 0) {
            String errorDesc = "";
            NodeList faultSubList = faultList.item(0).getChildNodes();
            for(int i=0;i<faultSubList.getLength();i++)
                errorDesc += faultSubList.item(i).getLocalName() + "=" + faultSubList.item(i).getTextContent() + "\n";
            throw new Exception("SOAP Fault response:\n" + errorDesc);
        }
        
        //return getStringFromXmlDoc(responseXml.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/","Body").item(0));
        return responseXml;
    }
    
    public static Document createNewDocument() throws Exception{
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().newDocument();
    }
    
    
    public static Document getXmlDocFromBytes(byte[] xml) throws Exception{
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }
    
    public static Document getXmlDocFromString(String xml) throws Exception{
        return getXmlDocFromBytes(xml.getBytes("UTF-8"));
    }
    
    public static String getStringFromXmlDoc(Node node) throws Exception{
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }
    
    public static void copyInputStreamToOutputStream(InputStream input, OutputStream output) throws Exception{
        int n = 0;
        int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 10;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        try {
            while (-1 != (n = input.read(buffer)))
                output.write(buffer, 0, n);
        } finally {
            input.close();
        }
    }
    
    public static byte[] toByteArray(InputStream is) throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] ret;
        try {
            copyInputStreamToOutputStream(is, out);
            ret = out.toByteArray();
        } finally {
            out.close();
            out = null;
        }
        return ret;
    }
    
    public static String toString(InputStream is) throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String ret;
        try {
            copyInputStreamToOutputStream(is, out);
            ret = out.toString("UTF-8");
        } finally {
            out.close();
            out = null;
        }
        return ret;
    }
    
    public static byte[] readFile(String file) throws Exception{
        return readFile(new File(file));
    }
    
    public static byte[] readFile(File file) throws Exception{
        try(RandomAccessFile raf = new RandomAccessFile(file, "r")){
            byte[] ret = new byte[(int)raf.length()];
            raf.read(ret);
            return ret;
        }
    }
    
    public static void writeFile(byte[] data, String filePath, boolean appendData) throws Exception{
        writeFile(data, new File(filePath), appendData);
    }
    
    public static void writeFile(byte[] data, File file, boolean appendData) throws Exception{
        if(file.getParentFile()!= null && !file.getParentFile().exists())
            file.getParentFile().mkdirs();
        try(FileOutputStream fos = new FileOutputStream(file, appendData)){
            fos.write(data);
            fos.flush();
        }
    }
    
    public static byte[] base64Decode(String encodedData) {
        //return DatatypeConverter.parseBase64Binary(encodedData);
        return Base64.getDecoder().decode(encodedData);
    }
    
    public static String base64Encode(byte[] dataToEncode) {
        //return DatatypeConverter.printBase64Binary(dataToEncode);
        return Base64.getEncoder().encodeToString(dataToEncode);
    }
    
    public static String uploadFolder = "";
    static {
        uploadFolder = System.getProperty("java.io.tmpdir");
        if(!uploadFolder.endsWith("\\") && !uploadFolder.endsWith("/"))
            uploadFolder += "/";
        uploadFolder += "ADOxx_uploaded/";
    }
    /*
    public static String processLocalFilePath(String path){
        if(path == null)
            return null;
        if(!uploadFolder.endsWith("\\") && !uploadFolder.endsWith("/"))
            uploadFolder += "/";
        return path.startsWith("_")?uploadFolder+path:path;
    }
    
    public static boolean checkLocalFilePathSecurity(File file) throws Exception {
        return file.getCanonicalPath().startsWith(new File(uploadFolder).getCanonicalPath());
    }
    */
    public static void updateLocalFile(String fileId, byte[] fileContent) throws Exception{
        if(fileId == null || fileId.isEmpty())
            throw new Exception("fileId can not be empty");
        String filePath = uploadFolder + ((uploadFolder.endsWith("\\") || uploadFolder.endsWith("/"))?"":"/") + fileId;
        if(!uploadFolder.isEmpty() && !new File(filePath).getCanonicalPath().startsWith(new File(uploadFolder).getCanonicalPath()))
            throw new Exception("Security Exception: Is not allowed to write the file " + new File(filePath).getCanonicalPath());
        writeFile(fileContent, filePath, false);
    }
    public static String uploadLocalFile(String fileName, byte[] fileContent) throws Exception{
        return uploadLocalFile(null, fileName, fileContent);
    }
    public static String uploadLocalFile(String rootName, String fileName, byte[] fileContent) throws Exception{
        if(fileName == null || fileName.isEmpty())
            throw new Exception("fileName can not be empty");
        String fileId = "";
        if(rootName != null && !rootName.trim().isEmpty()) {
            fileId += (rootName.startsWith("\\") || rootName.startsWith("/"))?rootName.substring(1):rootName;
            fileId += (rootName.endsWith("\\") || rootName.endsWith("/"))?"":"/";
        }
        fileId += "_" + java.util.UUID.randomUUID() + "/";
        fileId += (fileName.startsWith("\\") || fileName.startsWith("/"))?fileName.substring(1):fileName;
        
        String filePath = uploadFolder + ((uploadFolder.endsWith("\\") || uploadFolder.endsWith("/"))?"":"/") + fileId;
        if(!uploadFolder.isEmpty() && !new File(filePath).getCanonicalPath().startsWith(new File(uploadFolder).getCanonicalPath()))
            throw new Exception("Security Exception: Is not allowed to write the file " + new File(filePath).getCanonicalPath());
        writeFile(fileContent, filePath, false);
        return fileId;
    }
    
    public static String revealLocalFile(String fileId) throws Exception {
        String filePath = uploadFolder + ((uploadFolder.endsWith("\\") || uploadFolder.endsWith("/"))?"":"/") + fileId;
        File file = new File(filePath);
        if(!uploadFolder.isEmpty() && !file.getCanonicalPath().startsWith(new File(uploadFolder).getCanonicalPath()))
            throw new Exception("Security Exception: Is not allowed to read the file " + file.getCanonicalPath());
        if(!file.isFile())
            throw new Exception("The required file don't exist: " + fileId);
        return filePath;
    }
    
    public static byte[] downloadLocalFile(String fileId) throws Exception {
        return readFile(revealLocalFile(fileId));
    }
    
    public static List<String> listLocalFiles(String rootName) throws Exception {
        if(rootName == null || rootName.isEmpty())
            throw new Exception("rootName can not be empty");
        
        rootName = (rootName.startsWith("\\") || rootName.startsWith("/"))?rootName.substring(1):rootName;
        
        String rootPath = uploadFolder + ((uploadFolder.endsWith("\\") || uploadFolder.endsWith("/"))?"":"/") + rootName;
        
        File root = new File(rootPath);
        if(!uploadFolder.isEmpty() && !root.getCanonicalPath().startsWith(new File(uploadFolder).getCanonicalPath()))
            throw new Exception("Security Exception: Is not allowed to read the content of the directory " + root.getCanonicalPath());
        if(!root.isDirectory())
            throw new Exception("The required folder don't exist: " + rootName);
        
        
        File[] foldersInRoot = root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        
        List<String> ret = new ArrayList<String>();
        
        for(File folderInRoot : foldersInRoot) {
            File[] files = folderInRoot.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            });
            for(File file : files) {
                ret.add(rootName + "/" + folderInRoot.getName() + "/" + file.getName());
            }
        }
        return ret;
    }
    
    public static Object outputAdaptation(String originalOutput, String input, String javascriptAdaptationAlgorithm, boolean allowMicroserviceCall) throws Exception {
        return outputAdaptation(originalOutput, input, javascriptAdaptationAlgorithm, allowMicroserviceCall, null);
    }
    
    public static Object outputAdaptation(String originalOutput, String input, String javascriptAdaptationAlgorithm, boolean allowMicroserviceCall, String defaultMicroserviceId) throws Exception {
        String alg = "";
        HashMap<String, Object> jsEngineParamenters = new HashMap<String, Object>();
        if(originalOutput!=null) {
            jsEngineParamenters.put("_output", originalOutput);
            alg += "output = JSON.parse(_output);\n";
        }
        if(input!=null) {
            jsEngineParamenters.put("_input", input);
            alg += "input = JSON.parse(_input);\n";
        }

        alg += javascriptAdaptationAlgorithm;
        
        return javascriptSafeEval(jsEngineParamenters, alg, allowMicroserviceCall, defaultMicroserviceId);
    }
    
    public static Object javascriptSafeEval(HashMap<String, Object> parameters, String algorithm, boolean allowMicroserviceCall) throws Exception {
        return javascriptSafeEval(parameters, algorithm, allowMicroserviceCall, null);
    }
    
    public static Object javascriptSafeEval(HashMap<String, Object> parameters, String algorithm, boolean allowMicroserviceCall, String defaultMicroserviceId) throws Exception {
        if(parameters==null)
            parameters = new HashMap<String, Object>();
        String alg = "out=function(o){return JSON.stringify(o);};\n";
        if(allowMicroserviceCall) {

            parameters.put("_callMicroservice", (Function<String, Function<String, Function<String, String>>>) (microserviceId) -> (operationId) -> (microserviceInputs) -> {
                try {
                    String msId = microserviceId == null || microserviceId.isEmpty() ? defaultMicroserviceId : microserviceId;
                    return MicroserviceController.unique().callMicroserviceForced(msId, operationId, Json.createReader(new StringReader(microserviceInputs)).readObject()).toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            alg += "callMicroservice=function(microserviceId, operationId, microserviceInputs){return JSON.parse(_callMicroservice.apply(microserviceId).apply(operationId).apply(JSON.stringify(microserviceInputs)));};\n";
            
            parameters.put("_callMicroserviceNT", (Function<String, Function<String, Function<String, String>>>) (microserviceId) -> (operationId) -> (microserviceInputs) -> {
                try {
                    String msId = microserviceId == null || microserviceId.isEmpty() ? defaultMicroserviceId : microserviceId;
                    return MicroserviceController.unique().callMicroserviceForcedNoThread(msId, operationId, Json.createReader(new StringReader(microserviceInputs)).readObject()).toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            alg += "callMicroserviceNT=function(microserviceId, operationId, microserviceInputs){return JSON.parse(_callMicroserviceNT.apply(microserviceId).apply(operationId).apply(JSON.stringify(microserviceInputs)));};\n";
            
            parameters.put("_callSyncConnectorForced", (Function<String, String>) (connectorConfiguration) -> {
                try {
                    return MicroserviceController.unique().callSyncConnectorForced(Json.createReader(new StringReader(connectorConfiguration)).readObject()).toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            alg += "callSyncConnectorForced=function(connectorConfiguration){return JSON.parse(_callSyncConnectorForced.apply(JSON.stringify(connectorConfiguration)));};\n";
        }
        
        alg += algorithm;
        
        //return Utils.javascriptSafeEval(parameters, alg, false, true, false, true, null, 20);
        //return Utils.javascriptSafeEval(parameters, alg, false, false, false, false, null, 20);
        return  Utils.javascriptSafeEval(parameters, alg);
    }
    
    public static int maxJSExecTimeInMinutes = 1;
    public static Object javascriptSafeEval(HashMap<String, Object> parameters, String algorithm) throws Exception {
        return javascriptSafeEval(parameters, algorithm, false, false, false, true, null, maxJSExecTimeInMinutes * 60);
        //return javascriptSafeEval(parameters, algorithm, true, false, false, false, null, 20);
    }
    public static Object javascriptSafeEval(HashMap<String, Object> parameters, String algorithm, boolean enableSecurityManager, boolean disableCriticalJSFunctions, boolean disableLoadJSFunctions, boolean defaultDenyJavaClasses, List<String> javaClassesExceptionList, int maxAllowedExecTimeInSeconds) throws Exception {
        // integrate separate monitor for memory consumption: https://github.com/javadelight/delight-nashorn-sandbox/blob/master/src/main/java/delight/nashornsandbox/internal/ThreadMonitor.java 
        //Think on executing the js not in a separate thread but in a separate process
        System.setProperty("java.net.useSystemProxies", "true");
        
        Policy originalPolicy = null;
        if(enableSecurityManager) {
            ProtectionDomain currentProtectionDomain = Utils.class.getProtectionDomain();
            originalPolicy = Policy.getPolicy();
            final Policy orinalPolicyFinal = originalPolicy;
            Policy.setPolicy(new Policy() {
                @Override
                public boolean implies(ProtectionDomain domain, Permission permission) {
                    if(domain.equals(currentProtectionDomain))
                        return true;
                    return orinalPolicyFinal.implies(domain, permission);
                }
            });
        }
        try {
            SecurityManager originalSecurityManager = null;
            if(enableSecurityManager) {
                originalSecurityManager = System.getSecurityManager();
                System.setSecurityManager(new SecurityManager() {
                    //allow only the opening of a socket connection (required by the JS function load())
                    @Override
                    public void checkConnect(String host, int port, Object context) {}
                    @Override
                    public void checkConnect(String host, int port) {}
                });
            }
            
            try {
                /*
                ScriptEngine engineReflex = null;
                
                try{
                    Class<?> nashornScriptEngineFactoryClass = Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
                    Class<?> classFilterClass = Class.forName("jdk.nashorn.api.scripting.ClassFilter");
                    
                    engineReflex = (ScriptEngine)nashornScriptEngineFactoryClass.getDeclaredMethod("getScriptEngine", new Class[]{Class.forName("jdk.nashorn.api.scripting.ClassFilter")}).invoke(nashornScriptEngineFactoryClass.newInstance(), Proxy.newProxyInstance(classFilterClass.getClassLoader(), new Class[]{classFilterClass}, new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if(method.getName().equals("exposeToScripts")) {
                                if(javaClassesExceptionList != null && javaClassesExceptionList.contains(args[0]))
                                    return defaultDenyJavaClasses;
                                return !defaultDenyJavaClasses;
                            }
                            throw new RuntimeException("no method found");
                        }
                    }));
                    
                    
                }catch(Exception ex) {
                    throw new Exception("Impossible to initialize the Nashorn Engine: " + ex.getMessage());
                }
                
                final ScriptEngine engine = engineReflex;
                */
                
                
                //JDK 11+ ready
                final ScriptEngine engine = new org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory().getScriptEngine(new org.openjdk.nashorn.api.scripting.ClassFilter() {

                    @Override
                    public boolean exposeToScripts(String className) {
                        if(javaClassesExceptionList != null && javaClassesExceptionList.contains(className))
                            return defaultDenyJavaClasses;
                        return !defaultDenyJavaClasses;
                    }
                });
                
                /*
                //JDK 8-11
                engine = new jdk.nashorn.api.scripting.NashornScriptEngineFactory().getScriptEngine(new jdk.nashorn.api.scripting.ClassFilter() {
                    @Override
                    public boolean exposeToScripts(String className) {
                        if(javaClassesExceptionList != null && javaClassesExceptionList.contains(className))
                            return defaultDenyJavaClasses;
                        return !defaultDenyJavaClasses;
                    }
                });
                */
                
                if(parameters != null)
                    for(Entry<String, Object> entry : parameters.entrySet())
                        engine.put(entry.getKey(), entry.getValue());
                
                if(disableCriticalJSFunctions)
                    engine.eval("quit=function(){throw 'quit() not allowed';};exit=function(){throw 'exit() not allowed';};print=function(){throw 'print() not allowed';};echo=function(){throw 'echo() not allowed';};readFully=function(){throw 'readFully() not allowed';};readLine=function(){throw 'readLine() not allowed';};$ARG=null;$ENV=null;$EXEC=null;$OPTIONS=null;$OUT=null;$ERR=null;$EXIT=null;");
                    //engine.eval("quit=function(){throw 'quit() not allowed';};exit=function(){throw 'exit() not allowed';};readFully=function(){throw 'readFully() not allowed';};readLine=function(){throw 'readLine() not allowed';};$ARG=null;$ENV=null;$EXEC=null;$OPTIONS=null;$OUT=null;$ERR=null;$EXIT=null;");
                
                if(disableLoadJSFunctions)
                    engine.eval("load=function(){throw 'load() not allowed';};loadWithNewGlobal=function(){throw 'loadWithNewGlobal() not allowed';};");
                
                //nashorn-polyfill.js
                engine.eval("var global=this;var window=this;var process={env:{}};var console={};console.debug=print;console.log=print;console.warn=print;console.error=print;");
                
                class ScriptMonitor{
                    public Object scriptResult = null;
                    public Throwable  lastException = null;
                    private boolean stop = false;
                    Object lock = new Object();
                    @SuppressWarnings("deprecation")
                    public void startAndWait(Thread threadToMonitor, int secondsToWait) throws Exception {
                        threadToMonitor.start();
                        synchronized (lock) {
                            if(!stop) {
                                try {
                                    if(secondsToWait<1)
                                        lock.wait();
                                    else
                                        lock.wait(1000*secondsToWait);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        if(!stop) {
                            threadToMonitor.interrupt();
                            threadToMonitor.stop();
                            throw new Exception("Javascript forced to termination: Execution time bigger then " + secondsToWait + " seconds");
                        }
                        if(lastException != null)
                            throw new Exception("Error occurred in the Javascript execution: " + lastException.toString());
                    }
                    public void stop() {
                        synchronized (lock) {
                            stop = true;
                            lock.notifyAll();
                        }
                    }
                }
                final ScriptMonitor scriptMonitor = new ScriptMonitor();
                
                scriptMonitor.startAndWait(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            scriptMonitor.scriptResult = engine.eval(algorithm);
                        } catch (Throwable e) {
                            scriptMonitor.lastException = e;
                            //throw new RuntimeException(e);
                        } finally {
                            scriptMonitor.stop();
                        }
                    }
                }), maxAllowedExecTimeInSeconds);
                
                Object ret = scriptMonitor.scriptResult;
                return ret;
            } finally {
                if(enableSecurityManager)
                    System.setSecurityManager(originalSecurityManager);
            }
        } finally {
            if(enableSecurityManager)
                Policy.setPolicy(originalPolicy);
        }
    }
    
    public static String neverNull(String input) {
        if(input == null) return "";
        return input;
    }
    
    public static String neverNullO(Object input) {
        if(input == null) return "";
        return input.toString();
    }
    
    public static String escapeJson(String json) {
        //String quoted = org.codehaus.jettison.json.JSONObject.quote(json);
        //return quoted.substring(1, quoted.length()-1);
        //return json.replace("\"", "\\\"");
        
        if (json == null || json.length() == 0)
            return "";
        
        char c = 0;
        int i;
        int len = json.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        for (i = 0; i < len; i += 1) {
            c = json.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\');
                sb.append(c);
                break;
            case '/':
                sb.append('\\');
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
                sb.append("\\r");
                break;
            default:
                if (c < ' ') {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u" + t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }

        return sb.toString();
    }
    
    public static String encodeRFC3986(String text) throws Exception {
        return URLEncoder.encode(text, "UTF-8").replace("%7E", "~").replace("+", "%20").replace("*", "%2A");
    }
    
    public static String createOAuth10HttpAuthorizationHeader(String apiHttpMethod, String apiEndpoint, String oauth_consumer_key, String oauth_consumer_secret, String oauth_token, String oauth_token_secret, HashMap<String, String> parametersMap) throws Exception {
        String oauth_nonce = "" + (int) (Math.random() * 100000000);
        String oauth_signature_method = "HMAC-SHA1";
        String oauth_timestamp = "" + (System.currentTimeMillis() / 1000);
        String oauth_version = "1.0";
        HashMap<String, String> _parametersMap = new HashMap<>(parametersMap);
        _parametersMap.put("oauth_consumer_key", oauth_consumer_key);
        _parametersMap.put("oauth_nonce", oauth_nonce);
        _parametersMap.put("oauth_signature_method", oauth_signature_method);
        _parametersMap.put("oauth_timestamp", oauth_timestamp);
        _parametersMap.put("oauth_token", oauth_token);
        _parametersMap.put("oauth_version", oauth_version);
        //Signature generation
        ArrayList<String> parametersList = new ArrayList<String>();
        parametersList.addAll(_parametersMap.keySet());
        Collections.sort(parametersList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                try {
                    int res = String.CASE_INSENSITIVE_ORDER.compare(encodeRFC3986(o1), encodeRFC3986(o2));
                    return (res != 0) ? res : o1.compareTo(o2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }}
        );
        String parameters = "";
        for(String parameter : parametersList)
            parameters += encodeRFC3986(parameter) + "=" + encodeRFC3986(_parametersMap.get(parameter)) + "&";
        if(!parameters.isEmpty())
            parameters = parameters.substring(0, parameters.length()-1);
        String base = apiHttpMethod.toUpperCase() + "&" + encodeRFC3986(apiEndpoint) + "&" + encodeRFC3986(parameters);
        String keyS = encodeRFC3986(oauth_consumer_secret) + "&" + encodeRFC3986(oauth_token_secret);
        SecretKey key = new SecretKeySpec(keyS.getBytes("UTF-8"), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(key);
        String oauth_signature = Utils.base64Encode(mac.doFinal(base.getBytes("UTF-8")));
        
        return "OAuth " + encodeRFC3986("oauth_consumer_key") + "=\"" + encodeRFC3986(oauth_consumer_key) + "\", " +
            encodeRFC3986("oauth_nonce") + "=\"" + encodeRFC3986(oauth_nonce) + "\", " +
            encodeRFC3986("oauth_signature") + "=\"" + encodeRFC3986(oauth_signature) + "\", " +
            encodeRFC3986("oauth_signature_method") + "=\"" + encodeRFC3986(oauth_signature_method) + "\", " +
            encodeRFC3986("oauth_timestamp") + "=\"" + encodeRFC3986(oauth_timestamp) + "\", " +
            encodeRFC3986("oauth_token") + "=\"" + encodeRFC3986(oauth_token) + "\", " +
            encodeRFC3986("oauth_version") + "=\"" + encodeRFC3986(oauth_version) + "\"";
    }
    
    public static JsonObject java2Json(Object javaObject) {
        return Json.createReader(new StringReader(JsonbBuilder.create().toJson(javaObject))).readObject();
    }
    
    public static JsonObject echoTest(String input1, String input2, JsonObject input3) throws Exception {
        System.out.println(input1);
        System.out.println(input2);
        System.out.println(input3);
        return input3;
    }
    
    /*
    public static void main(String[] argv){
        try {
            //ArrayList<String[]> parameterList = new ArrayList<String[]>();
            //parameterList.add(new String[] {"script", "CC \"AdoScript\" INFOBOX \"test\""});
            //parameterList.add(new String[] {"resultVar", "result"});
            //System.out.println(Utils.getStringFromXmlDoc(Utils.sendSOAP("http://127.0.0.1", "urn:AdoWS", "execute", null, parameterList, null)));
            Utils.uploadFolder = "D:\\ADOXX.ORG\\MY_MICROSERVICE_FRAMEWORK\\microservices-upload\\";
            String fileIdUploaded = Utils.uploadLocalFile("root", "test.txt", "ciao".getBytes("UTF-8"));
            System.out.println(fileIdUploaded);
            Utils.updateLocalFile(fileIdUploaded, "ciao2".getBytes("UTF-8"));
            //List<String> rootFiles = Utils.listLocalFiles("root");
            //for(String file:rootFiles) System.out.println(file);
            //String encodedData = "Y2lhbyBhIHR1dHRpY2lhbyBhIHR1dHRp";
            //System.out.println(new String(Base64.getDecoder().decode(encodedData), "UTF-8"));
            //System.out.println(new String(DatatypeConverter.parseBase64Binary(encodedData), "UTF-8"));
            //byte [] dataToEncode = "test".getBytes("UTF-8");
            //System.out.println(DatatypeConverter.printBase64Binary(dataToEncode));
            //System.out.println(Base64.getEncoder().encodeToString(dataToEncode));
            //Utils.uploadFolder = "D:\\ADOXX.ORG\\MY_MICROSERVICE_FRAMEWORK\\microservices-upload\\";
            //System.out.println(revealLocalFile("test"));
            //System.out.println(checkLocalFilePathSecurity(new File("D:\\ADOXX.ORG\\MY_MICROSERVICE_FRAMEWORK\\microservices-upload\\..\\test.txt")));
            //System.out.println("{\"key\" : \"val\"}");
            //System.out.println(escapeJson("{\"key\" : \"val\"}"));
            //System.out.println(escapeJson(""));
            //System.out.println(javascriptSafeEval(null, "new java.lang.ProcessBuilder[\"(java.lang.String[])\"]([\"calc.exe\"]).start()"));
            //System.out.println(new java.io.File("D:\\ADOXX.ORG\\0-credential.txt").exists());
            //System.out.println(javascriptSafeEval(null, "new java.io.File(\"D:\\ADOXX.ORG\\0-credential.txt\").exists();"));
            //System.out.println(javascriptSafeEval(null, "for each (var f in new java.io.File(\".\").list()) print(f);"));
            //List<String> exceptionList = new ArrayList<String>();
            //exceptionList.add("java.util.Date");
            //System.out.println(javascriptSafeEval(null, "var Date = Java.type('java.util.Date');new Date();", true, false, exceptionList, 10));
            //System.out.println(javascriptSafeEval(null, "var Date = Java.type('java.util.Date');new Date();"));
            //System.out.println(javascriptSafeEval(null, "while(true);"));
            //System.out.println(javascriptSafeEval(null, "quit();"));
            //System.out.println(javascriptSafeEval(null, "print('ciao');"));
            //System.out.println(javascriptSafeEval(null, "load('https://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.8.3/underscore-min.js'); _.min([10, 5]);"));
            
            
            //HashMap<String, Object> inputs = new HashMap<>();
            //inputs.put("_echoTest", (Function<String, Function<String, Function<String, String>>>) (input1) -> (input2) -> (input3) -> {
            //    try {
            //        return Utils.echoTest(input1, input2, Json.createReader(new StringReader(input3)).readObject()).toString();
            //    } catch (Exception e) {
            //        throw new RuntimeException(e); 
            //    }
            //});
            
            //Object ret = javascriptSafeEval(inputs , "echoTest=function(input1, input2, input3){ return JSON.parse(_echoTest.apply(input1).apply(input2).apply(JSON.stringify(input3)));};      echoTest('1', '2', {test : ''});");
            //System.out.println(ret);
            
            //System.out.println(Utils.outputAdaptation("{\"key\":\"val\"}", "out(callMicroservice('TESTSYNC', 'default', {'Your Name':{'value':output.key}}));", true));
            //System.out.println(javascriptSafeEval(null, "while(true);"));
            //System.out.println(javascriptSafeEval(null, "JSON.stringify({key:'val'});"));
            //System.out.println("TEST");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */
}
