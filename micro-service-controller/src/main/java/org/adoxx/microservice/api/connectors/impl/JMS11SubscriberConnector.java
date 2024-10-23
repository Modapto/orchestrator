package org.adoxx.microservice.api.connectors.impl;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

import org.adoxx.microservice.api.connectors.AsyncConnectorA;
import org.adoxx.microservice.api.connectors.AsyncResponseHandlerI;
import org.adoxx.microservice.api.log.LogManager;
import org.adoxx.microservice.api.log.LogI.LogLevel;
import org.adoxx.microservice.utils.Utils;

public class JMS11SubscriberConnector extends AsyncConnectorA {
    //use JNDI to obtain JMS objects
    private static HashMap<String, String> availableContexFactoryClasses = new HashMap<String, String>();
    static {
        availableContexFactoryClasses.put("Nirvana", "com.pcbsys.nirvana.nSpace.NirvanaContextFactory");
        availableContexFactoryClasses.put("ActiveMQ", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        availableContexFactoryClasses.put("Solace", "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        availableContexFactoryClasses.put("OpenJMS", "org.exolab.jms.jndi.InitialContextFactory");
        availableContexFactoryClasses.put("WebLogic", "weblogic.jndi.WLInitialContextFactory");
    }
    
    private AsyncResponseHandlerI responseHandler = null;
    private MessageConsumer messageConsumer = null;
    private JsonObject lastMessage = null;
    
    @Override
    public String getName() {
        return "JMS 1.1 Subscriber Connector";
    }
    
    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Module for subscription to a JMS 1.1 topic")
            .add("de", "Module for subscription to a JMS 1.1 topic")
            .build();
    }

    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder().build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        JsonArrayBuilder contextNamesJsonArrayBuilder = Json.createArrayBuilder();
        String contextNames = "|";
        for(String context : availableContexFactoryClasses.keySet()) {
            contextNames += context + "|";
            contextNamesJsonArrayBuilder.add(context);
        }
        
        return Json.createObjectBuilder()
            .add("url", Json.createObjectBuilder()
                .add("name", "URL")
                .add("description", Json.createObjectBuilder()
                    .add("en", "JMS provider URL")
                    .add("de", "JMS provider URL"))
                .add("value", ""))
            .add("contextName", Json.createObjectBuilder()
                .add("name", "Context Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The context to use. Available are: "+contextNames)
                    .add("de", "The context to use. Available are: "+contextNames))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", contextNamesJsonArrayBuilder)))
            .add("connectionFactoryLookupName", Json.createObjectBuilder()
                .add("name", "ConnectionFactory Lookup Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Name of the ConnectionFactory object to lookup in the JMS provider")
                    .add("de", "Name of the ConnectionFactory object to lookup in the JMS provider"))
                .add("value", ""))
            .add("topicLookupName", Json.createObjectBuilder()
                .add("name", "Topic Lookup Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Name of the Topic object to lookup in the JMS provider")
                    .add("de", "Name of the Topic object to lookup in the JMS provider"))
                .add("value", ""))
            .add("idleAutoMsgIntervalTime", Json.createObjectBuilder()
                .add("name", "IDLE Auto Msg Interval Time")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The interval time in minutes to send the anti-timeout idle message. The value of 0 will deactivate the feature (default=0).")
                    .add("de", "The interval time in minutes to send the anti-timeout idle message. The value of 0 will deactivate the feature (default=0)."))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in one of the following formats:\n"
                + "- When the received message is a StreamMessage:\n"
                + "{" + "\n"
                + "  dataMIME : 'application/json'," + "\n"
                + "  dataJson : {" + "\n"
                + "    streamList : [{" + "\n"
                + "      dataMIME : 'application/octet-stream _or_ text/* _or_ application/json'," + "\n"
                + "      dataBase64/dataText/dataJson : '_ContentBase64_'/'_PlainText_'/{_JsonObject_}," + "\n"
                + "    }, {" + "\n"
                + "      ..." + "\n"
                + "    }]" + "\n"
                + "  }," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "    messageHeaders : {" + "\n"
                + "      JMSCorrelationID : ''," + "\n"
                + "      JMSDeliveryMode : ''," + "\n"
                + "      JMSExpiration : ''," + "\n"
                + "      JMSMessageID : ''," + "\n"
                + "      JMSPriority : ''," + "\n"
                + "      JMSRedelivered : ''," + "\n"
                + "      JMSTimestamp : ''," + "\n"
                + "      JMSType : ''," + "\n"
                + "      JMSDestination : ''," + "\n"
                + "      JMSReplyTo : ''" + "\n"
                + "    }," + "\n"
                + "    messageProperties : {" + "\n"
                + "      _propertyName_ : '_propertyValue_'," + "\n"
                + "      ..." + "\n"
                + "    }" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                + "\n\n"
                
                + "- When the received message is a MapMessage:\n"
                
                + "{" + "\n"
                + "  dataMIME : 'application/json'," + "\n"
                + "  dataJson : {" + "\n"
                + "    _mapKey_1_ : {" + "\n"
                + "      dataMIME : 'application/octet-stream _or_ text/plain'," + "\n"
                + "      dataBase64/dataText : '_ContentBase64_'/'_PlainText_'," + "\n"
                + "    }," + "\n"
                + "    _mapKey_2_ : {" + "\n"
                + "      ..." + "\n"
                + "    }," + "\n"
                + "    ..." + "\n"
                + "  }," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "    messageHeaders : {" + "\n"
                + "      JMSCorrelationID : ''," + "\n"
                + "      JMSDeliveryMode : ''," + "\n"
                + "      JMSExpiration : ''," + "\n"
                + "      JMSMessageID : ''," + "\n"
                + "      JMSPriority : ''," + "\n"
                + "      JMSRedelivered : ''," + "\n"
                + "      JMSTimestamp : ''," + "\n"
                + "      JMSType : ''," + "\n"
                + "      JMSDestination : ''," + "\n"
                + "      JMSReplyTo : ''" + "\n"
                + "    }," + "\n"
                + "    messageProperties : {" + "\n"
                + "      _propertyName_ : '_propertyValue_'," + "\n"
                + "      ..." + "\n"
                + "    }" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                + "\n\n"
                
                + "- When the received message is a TextMessage:\n"

                + "{" + "\n"
                + "  dataMIME : 'text/plain'," + "\n"
                + "  dataText : ''" + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "    messageHeaders : {" + "\n"
                + "      JMSCorrelationID : ''," + "\n"
                + "      JMSDeliveryMode : ''," + "\n"
                + "      JMSExpiration : ''," + "\n"
                + "      JMSMessageID : ''," + "\n"
                + "      JMSPriority : ''," + "\n"
                + "      JMSRedelivered : ''," + "\n"
                + "      JMSTimestamp : ''," + "\n"
                + "      JMSType : ''," + "\n"
                + "      JMSDestination : ''," + "\n"
                + "      JMSReplyTo : ''" + "\n"
                + "    }," + "\n"
                + "    messageProperties : {" + "\n"
                + "      _propertyName_ : '_propertyValue_'," + "\n"
                + "      ..." + "\n"
                + "    }" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                + "\n\n"
                
                + "- When the received message is a ObjectMessage:\n"

                + "{" + "\n"
                + "  dataMIME : 'application/json'," + "\n"
                + "  dataJson : {" + "\n"
                + "    ... _object_dipendent_json_content_as_generated_by_JsonB_ ..." + "\n"
                + "  }," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "    messageHeaders : {" + "\n"
                + "      JMSCorrelationID : ''," + "\n"
                + "      JMSDeliveryMode : ''," + "\n"
                + "      JMSExpiration : ''," + "\n"
                + "      JMSMessageID : ''," + "\n"
                + "      JMSPriority : ''," + "\n"
                + "      JMSRedelivered : ''," + "\n"
                + "      JMSTimestamp : ''," + "\n"
                + "      JMSType : ''," + "\n"
                + "      JMSDestination : ''," + "\n"
                + "      JMSReplyTo : ''" + "\n"
                + "    }," + "\n"
                + "    messageProperties : {" + "\n"
                + "      _propertyName_ : '_propertyValue_'," + "\n"
                + "      ..." + "\n"
                + "    }" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                + "\n\n"
                
                + "- When the received message is a BytesMessage:\n"

                + "{" + "\n"
                + "  dataMIME : 'application/octet-stream'," + "\n"
                + "  dataBase64 : '_contentBase64_'," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "    messageHeaders : {" + "\n"
                + "      JMSCorrelationID : ''," + "\n"
                + "      JMSDeliveryMode : ''," + "\n"
                + "      JMSExpiration : ''," + "\n"
                + "      JMSMessageID : ''," + "\n"
                + "      JMSPriority : ''," + "\n"
                + "      JMSRedelivered : ''," + "\n"
                + "      JMSTimestamp : ''," + "\n"
                + "      JMSType : ''," + "\n"
                + "      JMSDestination : ''," + "\n"
                + "      JMSReplyTo : ''" + "\n"
                + "    }," + "\n"
                + "    messageProperties : {" + "\n"
                + "      _propertyName_ : '_propertyValue_'," + "\n"
                + "      ..." + "\n"
                + "    }" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                + "\n\n"
                
                + "- When the received message do not contains a body:\n"

                + "{" + "\n"
                + "  dataMIME : 'text/plain'," + "\n"
                + "  dataText : ''," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "    messageHeaders : {" + "\n"
                + "      JMSCorrelationID : ''," + "\n"
                + "      JMSDeliveryMode : ''," + "\n"
                + "      JMSExpiration : ''," + "\n"
                + "      JMSMessageID : ''," + "\n"
                + "      JMSPriority : ''," + "\n"
                + "      JMSRedelivered : ''," + "\n"
                + "      JMSTimestamp : ''," + "\n"
                + "      JMSType : ''," + "\n"
                + "      JMSDestination : ''," + "\n"
                + "      JMSReplyTo : ''" + "\n"
                + "    }," + "\n"
                + "    messageProperties : {" + "\n"
                + "      _propertyName_ : '_propertyValue_'," + "\n"
                + "      ..." + "\n"
                + "    }" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                + "\n\n"
            ;
    }
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        if(messageConsumer != null) return; //Means: if there is already an active connection, it will do nothing
        if(responseHandler == null) throw new Exception("responseHandler must be provided before calling this method");
        
        String url = startConfiguration.getJsonObject("url")==null?"":startConfiguration.getJsonObject("url").getString("value", "");
        if(url.isEmpty()) throw new Exception("url not provided");
        String contextName = startConfiguration.getJsonObject("contextName")==null?"":startConfiguration.getJsonObject("contextName").getString("value", "");
        if(contextName.isEmpty()) throw new Exception("contextName not provided");
        String connectionFactoryLookupName = startConfiguration.getJsonObject("connectionFactoryLookupName")==null?"":startConfiguration.getJsonObject("connectionFactoryLookupName").getString("value", "");
        if(connectionFactoryLookupName.isEmpty()) throw new Exception("connectionFactoryLookupName not provided");
        String topicLookupName = startConfiguration.getJsonObject("topicLookupName")==null?"":startConfiguration.getJsonObject("topicLookupName").getString("value", "");
        if(topicLookupName.isEmpty()) throw new Exception("topicLookupName not provided");
        String idleAutoMsgIntervalTime = startConfiguration.getJsonObject("idleAutoMsgIntervalTime")==null?"":startConfiguration.getJsonObject("idleAutoMsgIntervalTime").getString("value", "");
        int idleAutoMsgIntervalTimeMinutes = idleAutoMsgIntervalTime.isEmpty()?0:Integer.parseInt(idleAutoMsgIntervalTime);
        
        String contextClass = availableContexFactoryClasses.get(contextName);
        if(contextClass == null) throw new Exception("The contexName " + contextName + " is not valid");
        
        Properties initialProperties = new Properties();
        initialProperties.put(Context.INITIAL_CONTEXT_FACTORY, contextClass);
        initialProperties.put(Context.PROVIDER_URL, url);
        Context context = new InitialContext(initialProperties);
        
        ConnectionFactory factory = null;
        Destination topic = null;
        try {
            factory = (ConnectionFactory) context.lookup(connectionFactoryLookupName);
            if(contextName.equals("Nirvana")) { //nirvana PATCH for incorrect setted RNAME!
                //((com.pcbsys.nirvana.nJMS.ConnectionFactoryImpl) factory).setRNAME(url);
                Class.forName("com.pcbsys.nirvana.nJMS.ConnectionFactoryImpl").getDeclaredMethod("setRNAME", new Class[]{url.getClass()}).invoke(factory, new Object[]{url});
            }
            topic = (Destination) context.lookup(topicLookupName);
        } finally {
            context.close();
        }
        
        try(Connection conn = factory.createConnection()) {
            conn.setExceptionListener(new ExceptionListener() {
                @Override
                public void onException(JMSException exception) {
                    LogManager.unique().log(LogLevel.ERROR, "Exception in the JMS Connection", exception);
                    throw new RuntimeException(exception);
                }
            });
            conn.start();
            try(Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                try(MessageConsumer consumer = sess.createConsumer(topic)) {
                    messageConsumer = consumer;
                    antiTimeout(conn, idleAutoMsgIntervalTimeMinutes);
                    while(true) {
                        //System.out.println("waiting for a message");
                        setPreStarted();
                        Message receivedMessage = messageConsumer.receive();
                        
                        if(receivedMessage != null) {
                            lastMessage = processJMS11Response(receivedMessage);
                            responseHandler.handler(lastMessage);
                        } else
                            break;
                    }
                } finally {
                    messageConsumer = null;
                }
            }
        }
    }
    
    private void antiTimeout(Connection conn, int idleAutoMsgIntervalTimeMinutes) throws Exception {
        if(idleAutoMsgIntervalTimeMinutes<=0) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(messageConsumer!=null) {
                        conn.stop();
                        conn.start();
                        Thread.sleep(idleAutoMsgIntervalTimeMinutes*60*1000);
                    }
                }catch(Exception ex) {
                    LogManager.unique().log(LogLevel.ERROR, "Exception in the JMS Connection Anti Timout", ex);
                    throw new RuntimeException("Exception in the JMS Connection Anti Timout", ex);
                }
            }
        }).start();
    }
    
    @Override
    public JsonObject performCall(JsonObject callConfiguration) throws Exception {
        if(lastMessage == null)
            throw new Exception("No messages yet received");
        return lastMessage;
        //System.out.println("performCall method called with parameter " + callConfiguration.toString());
        //return callConfiguration;
    }
    
    @Override
    public void stop() throws Exception {
        try {
            if(messageConsumer != null)
                messageConsumer.close();
        } catch(Exception ex) {}
    }

    @Override
    public void setAsyncResponsesHandler(AsyncResponseHandlerI asyncResponseHandler) {
        responseHandler = asyncResponseHandler;
    }
    
    private static JsonObject processJMS11Response(Message receivedMessage) throws Exception {
        JsonObjectBuilder moreInfoBuilder = Json.createObjectBuilder()
        .add("retrievalTime", Utils.getCurrentTime())
        .add("messageHeaders", Json.createObjectBuilder()
            .add("JMSCorrelationID", Utils.neverNull(receivedMessage.getJMSCorrelationID()))
            .add("JMSDeliveryMode", receivedMessage.getJMSDeliveryMode())
            .add("JMSExpiration", receivedMessage.getJMSExpiration())
            .add("JMSMessageID", Utils.neverNull(receivedMessage.getJMSMessageID()))
            .add("JMSPriority", receivedMessage.getJMSPriority())
            .add("JMSRedelivered", receivedMessage.getJMSRedelivered())
            .add("JMSTimestamp", receivedMessage.getJMSTimestamp())
            .add("JMSType", Utils.neverNull(receivedMessage.getJMSType()))
            .add("JMSDestination", Utils.neverNullO(receivedMessage.getJMSDestination()))
            .add("JMSReplyTo", Utils.neverNullO(receivedMessage.getJMSReplyTo()))
        );
        
        JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
        Enumeration<?> propertyNamesEnum = receivedMessage.getPropertyNames();
        while(propertyNamesEnum.hasMoreElements()) {
            String propertyName = (String) propertyNamesEnum.nextElement();
            Object propertyValueObj = receivedMessage.getObjectProperty(propertyName);
            if(propertyValueObj instanceof Boolean)
                propertiesBuilder.add(propertyName, (Boolean)propertyValueObj);
            else if(propertyValueObj instanceof Byte)
                propertiesBuilder.add(propertyName, (Byte)propertyValueObj);
            else if(propertyValueObj instanceof Double)
                propertiesBuilder.add(propertyName, (Double)propertyValueObj);
            else if(propertyValueObj instanceof Float)
                propertiesBuilder.add(propertyName, (Float)propertyValueObj);
            else if(propertyValueObj instanceof Integer)
                propertiesBuilder.add(propertyName, (Integer)propertyValueObj);
            else if(propertyValueObj instanceof Long)
                propertiesBuilder.add(propertyName, (Long)propertyValueObj);
            else if(propertyValueObj instanceof Short)
                propertiesBuilder.add(propertyName, (Short)propertyValueObj);
            else if(propertyValueObj instanceof String)
                propertiesBuilder.add(propertyName, (String)propertyValueObj);
        }
        moreInfoBuilder.add("messageProperties", propertiesBuilder);
        
        if(receivedMessage instanceof StreamMessage) {
            StreamMessage msg = (StreamMessage) receivedMessage;
            JsonArrayBuilder outputArrayBuilder = Json.createArrayBuilder();
            while(true) {
                try {
                    Object streamObject = msg.readObject();
                    if(streamObject instanceof byte[])
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "application/octet-stream").add("dataBase64", Utils.base64Encode((byte[])streamObject)));
                    else if(streamObject instanceof Boolean)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/boolean").add("dataText", ""+(Boolean)streamObject));
                    else if(streamObject instanceof Byte)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/byte").add("dataText", ""+(Byte)streamObject));
                    else if(streamObject instanceof Short)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/short").add("dataText", ""+(Short)streamObject));
                    else if(streamObject instanceof Character)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/character").add("dataText", ""+(Character)streamObject));
                    else if(streamObject instanceof Integer)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/integer").add("dataText", ""+(Integer)streamObject));
                    else if(streamObject instanceof Long)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/long").add("dataText", ""+(Long)streamObject));
                    else if(streamObject instanceof Float)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/float").add("dataText", ""+(Float)streamObject));
                    else if(streamObject instanceof Double)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/double").add("dataText", ""+(Double)streamObject));
                    else if(streamObject instanceof String)
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "text/plain").add("dataText", ""+(String)streamObject));
                    else
                        outputArrayBuilder.add(Json.createObjectBuilder().add("dataMIME", "application/json").add("dataJson", Utils.java2Json(streamObject)));
                } catch(Exception ex) {
                    break;
                }
            }
            
            return Json.createObjectBuilder()
                .add("dataMIME", "application/json")
                .add("dataJson", Json.createObjectBuilder()
                    .add("streamList", outputArrayBuilder))
                .add("moreInfo", moreInfoBuilder)
            .build();
            
        } else if(receivedMessage instanceof MapMessage) {
            MapMessage msg = (MapMessage) receivedMessage;
            JsonObjectBuilder outputBuilder = Json.createObjectBuilder();
            Enumeration<?> mapNamesEnum = msg.getMapNames();
            while(mapNamesEnum.hasMoreElements()) {
                String name = (String) mapNamesEnum.nextElement();
                if(msg.getObject(name) instanceof byte[])
                    outputBuilder.add(name, Json.createObjectBuilder().add("dataMIME", "application/octet-stream").add("dataBase64", Utils.base64Encode(msg.getBytes(name))));
                else
                    outputBuilder.add(name, Json.createObjectBuilder().add("dataMIME", "text/plain").add("dataText", msg.getString(name)));
            }
            
            return Json.createObjectBuilder()
                .add("dataMIME", "application/json")
                .add("dataJson", outputBuilder)
                .add("moreInfo", moreInfoBuilder)
            .build();
            
        } else if(receivedMessage instanceof TextMessage) {
            TextMessage msg = (TextMessage)receivedMessage;
            String text = msg.getText();
            
            return Json.createObjectBuilder()
                .add("dataMIME", "text/plain")
                .add("dataText", text)
                .add("moreInfo", moreInfoBuilder)
            .build();
            
        } else if(receivedMessage instanceof ObjectMessage) {
            ObjectMessage msg = (ObjectMessage) receivedMessage;
            Serializable obj = msg.getObject();

            return Json.createObjectBuilder()
                .add("dataMIME", "application/json")
                .add("dataJson", Utils.java2Json(obj))
                .add("moreInfo", moreInfoBuilder)
            .build();
            
        } else if(receivedMessage instanceof BytesMessage) {
            BytesMessage msg = (BytesMessage) receivedMessage;
            long msgLength = msg.getBodyLength();
            if (((long)Integer.MAX_VALUE) < msgLength) throw new Exception("The ByteMessage is to big to be managed: " + msgLength);
            byte[] out = new byte[(int)msgLength];
            msg.readBytes(out, (int)msgLength);
            
            return Json.createObjectBuilder()
                .add("dataMIME", "application/octet-stream")
                .add("dataBase64", Utils.base64Encode(out))
                .add("moreInfo", moreInfoBuilder)
            .build();
            
        } else {
            return Json.createObjectBuilder()
                .add("dataMIME", "text/plain")
                .add("dataText", "")
                .add("moreInfo", moreInfoBuilder)
            .build();
        }
    }
    
    public JsonObject lookupAll(String contextName, String url) throws Exception {
        if(contextName==null || contextName.isEmpty()) throw new Exception("contextName not provided");
        if(url==null || url.isEmpty()) throw new Exception("url not provided");
        
        String contextClass = availableContexFactoryClasses.get(contextName);
        if(contextClass == null) throw new Exception("The contexName " + contextName + " is not valid");
        
        JsonObjectBuilder retBuilder = Json.createObjectBuilder();
        Properties initialProperties = new Properties();
        initialProperties.put(Context.INITIAL_CONTEXT_FACTORY, contextClass);
        initialProperties.put(Context.PROVIDER_URL, url);
        Context context = new InitialContext(initialProperties);
        try {
            NamingEnumeration<NameClassPair> contextList = context.list("");
            while(contextList.hasMoreElements()) {
                NameClassPair nameClassPair = contextList.nextElement();
                retBuilder.add(nameClassPair.getName(), nameClassPair.getClassName());
            }
        } finally {
            context.close();
        }
        
        return retBuilder.build();
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        //System.getProperties().put("proxySet", "true");
        //System.getProperties().put("socksProxyHost", "127.0.0.1");
        //System.getProperties().put("socksProxyPort", "9999");
        
        JMS11SubscriberConnector connector = new JMS11SubscriberConnector();
        try{
            System.out.println("Available topics/queues: " + connector.lookupAll("Nirvana", "nsp://daehglobal23134.hycloud.softwareag.com:9000"));
            
            connector.setAsyncResponsesHandler(new AsyncResponseHandlerI() {
                @Override
                public void handler(JsonObject asyncResponse) {
                    System.out.println("Received a message : " + asyncResponse.toString());
                }
            });
            connector.threadStart(Json.createObjectBuilder()
                .add("url", Json.createObjectBuilder().add("value", "nsp://daehglobal23134.hycloud.softwareag.com:9000"))
                .add("contextName", Json.createObjectBuilder().add("value", "Nirvana"))
                .add("connectionFactoryLookupName", Json.createObjectBuilder().add("value", "jms/optimize/connection_factory"))
                .add("topicLookupName", Json.createObjectBuilder().add("value", "Event::DISRUPT::MyTest"))
                .add("idleAutoMsgIntervalTime", Json.createObjectBuilder().add("value", "1"))
                .build()
            );
            connector.waitThreadStart();
            Thread.sleep(30*1000);
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .build()
            );
            System.out.println("Last received message after 30s from start: " + callOutputJson);
            //connector.threadStop();
        } finally {
            //connector.threadStop();
        }
    }
    */
}

