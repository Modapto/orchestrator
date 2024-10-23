package org.adoxx.microservice.api.connectors.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class LDAPConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "LDAP Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Module for connection to LDAP")
            .add("de", "Module for connection to LDAP")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("host", Json.createObjectBuilder()
                .add("name", "Host")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Server host")
                    .add("de", "Server host"))
                .add("value", ""))
            .add("port", Json.createObjectBuilder()
                .add("name", "Port")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Server port")
                    .add("de", "Server port"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("username", Json.createObjectBuilder()
                .add("name", "Username")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Username")
                    .add("de", "Username"))
                .add("value", ""))
            .add("password", Json.createObjectBuilder()
                .add("name", "Password")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Password")
                    .add("de", "Password"))
                .add("value", ""))
            .add("searchBaseDN", Json.createObjectBuilder()
                .add("name", "SearchBaseDN")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Search Base DN")
                    .add("de", "Search Base DN"))
                .add("value", ""))
            .add("searchFilter", Json.createObjectBuilder()
                .add("name", "SearchFilter")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Search Filter")
                    .add("de", "Search Filter"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  resultDNs : {" + "\n"
                + "    _cn_full_name_ : {" + "\n"
                + "      _attribute_name_ : [_attribute_values_]" + "\n"
                + "    }," + "\n"
                + "  }," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                ;
    }
    
    private String host = "";
    private int port = 0;

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        host = startConfiguration.getJsonObject("host")==null?"":startConfiguration.getJsonObject("host").getString("value", "");
        if(host.isEmpty()) throw new Exception("LDAP host not provided");
        
        try {
            if(startConfiguration.getJsonObject("port")!=null && startConfiguration.getJsonObject("port").getString("value", ""+port).length()!=0)
                port = Integer.parseInt(startConfiguration.getJsonObject("port").getString("value", ""+port));
        }catch(Exception ex) {
            throw new Exception("The LDAP port number is incorrect: '"+startConfiguration.getJsonObject("port").getString("value")+"'");
        }
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String username = configuration.getJsonObject("username")==null?"":configuration.getJsonObject("username").getString("value", "");
        String password = configuration.getJsonObject("password")==null?"":configuration.getJsonObject("password").getString("value", "");
        String searchBaseDN = configuration.getJsonObject("searchBaseDN")==null?"":configuration.getJsonObject("searchBaseDN").getString("value", "");
        String searchFilter = configuration.getJsonObject("searchFilter")==null?"":configuration.getJsonObject("searchFilter").getString("value", "");
        if(searchFilter.isEmpty()) searchFilter = "(objectclass=*)";

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port);
        props.put(Context.SECURITY_AUTHENTICATION, "simple");
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, password);

        InitialDirContext context = new InitialDirContext(props);
        JsonObjectBuilder resultDNsJSON = Json.createObjectBuilder();
        try {
            DirContext rootContext = (DirContext)context.lookup("");
            
            SearchControls ctrls = new SearchControls();
            ctrls.setReturningAttributes(null);
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> searchResults = rootContext.search(searchBaseDN, searchFilter, ctrls);
            while (searchResults.hasMoreElements()) {
                SearchResult searchResult = searchResults.nextElement();
                String dnName = searchResult.getNameInNamespace();
                JsonObjectBuilder resultDNJSON = Json.createObjectBuilder();
                NamingEnumeration<? extends Attribute> attributes = searchResult.getAttributes().getAll();
                while (attributes.hasMoreElements()) {
                    Attribute attribute = attributes.nextElement();
                    String attributeID = attribute.getID();
                    JsonArrayBuilder resultDNAttributeListJSON = Json.createArrayBuilder();
                    NamingEnumeration<?> attributeValues = attribute.getAll();
                    while (attributeValues.hasMoreElements()) {
                        Object attrValue = attributeValues.nextElement();
                        if (attrValue instanceof String)
                            resultDNAttributeListJSON.add((String)attrValue);
                        else if (attrValue instanceof byte[])
                            resultDNAttributeListJSON.add(new String((byte[])attrValue, "UTF-8"));
                        else
                            throw new Exception("Unexpected attribute value: " + attrValue.getClass().getName());
                    }
                    resultDNJSON.add(attributeID, resultDNAttributeListJSON);
                }
                
                resultDNsJSON.add(dnName, resultDNJSON);
            }
        } finally {
            context.close();
        }
        return Json.createObjectBuilder()
            .add("resultDNs", resultDNsJSON)
            .add("moreInfo", Json.createObjectBuilder().add("retrievalTime", Utils.getCurrentTime()))
            .build();
    }
    
    @Override
    public void stop() throws Exception {
        host = "";
        port = 0;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        LDAPConnector connector = new LDAPConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("host", Json.createObjectBuilder().add("value", "127.0.0.1"))
                .add("port", Json.createObjectBuilder().add("value", "10389"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("username", Json.createObjectBuilder().add("value", "uid=admin,ou=system"))
                .add("password", Json.createObjectBuilder().add("value", "secret"))
                .add("searchBaseDN", Json.createObjectBuilder().add("value", "ou=people,dc=example,dc=com"))
                .add("searchFilter", Json.createObjectBuilder().add("value", "cn=user1"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
