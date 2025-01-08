package org.adoxx.microservice.api.connectors.impl;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class JMS11PublisherConnector extends SyncConnectorA {
    //use JNDI to obtain JMS objects
    private static HashMap<String, String> availableContexFactoryClasses = new HashMap<String, String>();
    static {
        availableContexFactoryClasses.put("Nirvana", "com.pcbsys.nirvana.nSpace.NirvanaContextFactory");
        availableContexFactoryClasses.put("ActiveMQ", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        availableContexFactoryClasses.put("Solace", "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        availableContexFactoryClasses.put("OpenJMS", "org.exolab.jms.jndi.InitialContextFactory");
        availableContexFactoryClasses.put("WebLogic", "weblogic.jndi.WLInitialContextFactory");
        availableContexFactoryClasses.put("Kafka", "io.confluent.kafka.jms.KafkaInitialContextFactory");
    }
    
    private static HashMap<String, String> managedMessageTypes = new HashMap<String, String>();
    static {
        managedMessageTypes.put("Text", "TextMessage");
        managedMessageTypes.put("Bytes", "BytesMessage");
    }
    
    private Context context = null;
    private Connection connection = null;
    private Session session = null;
    
    @Override
    public String getName() {
        return "JMS 1.1 Publisher Connector";
    }
    
    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Module for publishing to a JMS 1.1 topic")
            .add("de", "Module for publishing to a JMS 1.1 topic")
            .build();
    }

    @Override
    public JsonObject getCallConfigurationTemplate() {
        JsonArrayBuilder messageTypesJsonArrayBuilder = Json.createArrayBuilder();
        String messageTypes = "|";
        for(String messageType : managedMessageTypes.keySet()) {
            messageTypes += messageType + "|";
            messageTypesJsonArrayBuilder.add(messageType);
        }
        
        return Json.createObjectBuilder()
            .add("topicName", Json.createObjectBuilder()
                .add("name", "Topic Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Name of the Topic object where to publish in the JMS provider")
                    .add("de", "Name of the Topic object where to publish in the JMS provider"))
                .add("value", ""))
            .add("properties", Json.createObjectBuilder()
                .add("name", "Message Properties")
                .add("description", Json.createObjectBuilder()
                    .add("en", "JSON of properties to attach in the message")
                    .add("de", "JSON of properties to attach in the message"))
                .add("value", ""))
            .add("message", Json.createObjectBuilder()
                .add("name", "Message")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The message to publish (for Bytes messages the message have to be Base64 encoded)")
                    .add("de", "The message to publish (for Bytes messages the message have to be Base64 encoded)"))
                .add("value", ""))
            .add("messageType", Json.createObjectBuilder()
                .add("name", "Message Type")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The type of message to send. Available are: "+messageTypes)
                    .add("de", "The type of message to send. Available are: "+messageTypes))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", messageTypesJsonArrayBuilder)))
            .add("persistenceMode", Json.createObjectBuilder()
                .add("name", "Persistence Mode")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The persistence mode to use. Available are PERSISTENT (default) and NON_PERSISTENT")
                    .add("de", "Name of the ConnectionFactory object to lookup in the JMS provider"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder()
                        .add("PERSISTENT")
                        .add("NON_PERSISTENT"))))
            .build();
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
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  dataMIME : 'text/plain'," + "\n"
                + "  dataText : 'OK'," + "\n"
                + "  moreInfo : {" + "\n"
                + "    executionTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {        
        String url = startConfiguration.getJsonObject("url")==null?"":startConfiguration.getJsonObject("url").getString("value", "");
        if(url.isEmpty()) throw new Exception("url not provided");
        String contextName = startConfiguration.getJsonObject("contextName")==null?"":startConfiguration.getJsonObject("contextName").getString("value", "");
        if(contextName.isEmpty()) throw new Exception("contextName not provided");
        String connectionFactoryLookupName = startConfiguration.getJsonObject("connectionFactoryLookupName")==null?"":startConfiguration.getJsonObject("connectionFactoryLookupName").getString("value", "");
        if(connectionFactoryLookupName.isEmpty()) throw new Exception("connectionFactoryLookupName not provided");
        
        String contextClass = availableContexFactoryClasses.get(contextName);
        if(contextClass == null) throw new Exception("The contexName " + contextName + " is not valid");
        
        Properties initialProperties = new Properties();
        initialProperties.put(Context.INITIAL_CONTEXT_FACTORY, contextClass);
        initialProperties.put(Context.PROVIDER_URL, url);
        context = new InitialContext(initialProperties);
        
        ConnectionFactory factory = (ConnectionFactory) context.lookup(connectionFactoryLookupName);
        if(contextName.equals("Nirvana")) { //nirvana PATCH for incorrect setted RNAME!
            //((com.pcbsys.nirvana.nJMS.ConnectionFactoryImpl) factory).setRNAME(url);
            Class.forName("com.pcbsys.nirvana.nJMS.ConnectionFactoryImpl").getDeclaredMethod("setRNAME", new Class[]{url.getClass()}).invoke(factory, new Object[]{url});
        }
        
        connection = factory.createConnection();
        connection.setExceptionListener(new ExceptionListener() {
            @Override
            public void onException(JMSException exception) {
                throw new RuntimeException(exception);
            }
        });
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    public JsonObject performCall(JsonObject callConfiguration) throws Exception {
        String topicName = callConfiguration.getJsonObject("topicName")==null?"":callConfiguration.getJsonObject("topicName").getString("value", "");
        if(topicName.isEmpty()) throw new Exception("topicName not provided");
        String propertiesJsonS = callConfiguration.getJsonObject("properties")==null?"{}":callConfiguration.getJsonObject("properties").getString("value", "{}");
        if(propertiesJsonS.isEmpty()) throw new Exception("properties not provided");
        JsonObject properties = null;
        try {
            properties = Json.createReader(new StringReader(propertiesJsonS)).readObject();
        }catch(Exception e) {
            throw new Exception("The provided properties is not a valid Json Object: " + e.getMessage());
        }
        String message = callConfiguration.getJsonObject("message")==null?"":callConfiguration.getJsonObject("message").getString("value", "");
        if(message.isEmpty()) throw new Exception("message not provided");
        String messagetypeS = callConfiguration.getJsonObject("messageType")==null?"":callConfiguration.getJsonObject("messageType").getString("value", "");
        if(messagetypeS.isEmpty()) throw new Exception("messageType not provided");
        String messagetype = managedMessageTypes.get(messagetypeS);
        if(messagetype == null) throw new Exception("The messageType " + messagetypeS + " is not valid");
        
        String persistenceMode = callConfiguration.getJsonObject("persistenceMode")==null?"":callConfiguration.getJsonObject("persistenceMode").getString("value", "");
        if(persistenceMode.isEmpty()) persistenceMode = "PERSISTENT";
        if(!(persistenceMode.equals("PERSISTENT") || persistenceMode.equals("NON_PERSISTENT"))) throw new Exception("The persistenceMode " + persistenceMode + " is not valid");
        
        Destination topic = (Destination) context.lookup(topicName);
        try(MessageProducer messageProducer = session.createProducer(topic)) {
            Message msg = null;
            
            if(messagetype.equals("TextMessage")) {
                msg = session.createTextMessage(message);
            } else if(messagetype.equals("BytesMessage")) {
                BytesMessage bytesMsg = session.createBytesMessage();
                bytesMsg.writeBytes(Utils.base64Decode(message));
                msg = bytesMsg;
            } else {
                throw new Exception("The messagetype " + messagetype + " is not available");
            }
            
            addProperties(msg, properties);
            if(persistenceMode.equals("NON_PERSISTENT"))
                messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            messageProducer.send(msg);
        }
        return Json.createObjectBuilder()
            .add("dataMIME", "text/plain")
            .add("dataText", "OK")
            .add("moreInfo", Json.createObjectBuilder()
                .add("executionTime", Utils.getCurrentTime())
            ).build();
    }
    
    private void addProperties(Message msg, JsonObject properties) throws Exception {
        for(Entry<String, JsonValue> entry : properties.entrySet()) {
            if(entry.getValue().getValueType().compareTo(ValueType.STRING) == 0) {
                String valueJsonString = entry.getValue().toString();
                msg.setStringProperty(entry.getKey(), valueJsonString.substring(1, valueJsonString.length()-1));
            } else if(entry.getValue().getValueType().compareTo(ValueType.NUMBER) == 0) {
                JsonNumber number = (JsonNumber)entry.getValue();
                if(number.isIntegral())
                    msg.setLongProperty(entry.getKey(), number.longValueExact());
                else
                    msg.setDoubleProperty(entry.getKey(), number.doubleValue());
            } else if(entry.getValue().getValueType().compareTo(ValueType.NULL) == 0)
                msg.setStringProperty(entry.getKey(), null);
            else if(entry.getValue().getValueType().compareTo(ValueType.FALSE) == 0)
                msg.setBooleanProperty(entry.getKey(), false);
            else if(entry.getValue().getValueType().compareTo(ValueType.TRUE) == 0)
                msg.setBooleanProperty(entry.getKey(), true);
            else
                throw new Exception("The variable " + entry.getKey() + "is in an incorrect format: " + entry.getValue().getValueType());
        }
    }
    
    @Override
    public void stop() throws Exception {
        try {
            if(session!=null)
                session.close();
            if(connection!=null)
                connection.close();
            if(context!=null)
                context.close();
        } catch(Exception ex) {}
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        //System.getProperties().put("proxySet", "true");
        //System.getProperties().put("socksProxyHost", "127.0.0.1");
        //System.getProperties().put("socksProxyPort", "9999");
        
        JMS11PublisherConnector connector = new JMS11PublisherConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("url", Json.createObjectBuilder().add("value", "nsp://daehglobal23134.hycloud.softwareag.com:9000"))
                .add("contextName", Json.createObjectBuilder().add("value", "Nirvana"))
                .add("connectionFactoryLookupName", Json.createObjectBuilder().add("value", "jms/optimize/connection_factory"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("topicName", Json.createObjectBuilder().add("value", "Event::DISRUPT::MyTest")) //Event::DISRUPT::MyTest kpiDashboard
                .add("properties", Json.createObjectBuilder().add("value", "{\"id\":\"1\"}"))
                .add("message", Json.createObjectBuilder().add("value", Utils.base64Encode("hello world!".getBytes("UTF-8"))))
                .add("messageType", Json.createObjectBuilder().add("value", "Bytes"))
                .add("persistenceMode", Json.createObjectBuilder().add("value", "NON_PERSISTENT"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {
            connector.threadStop();
        }
    }
    */
}

