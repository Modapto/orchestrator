package org.adoxx.microservice.api.connectors.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

import net.sourceforge.jtds.jdbcx.JtdsDataSource;

public class MSSQLServerConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "MSSQLServer Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Module for connection to MSSQLServer")
            .add("de", "Module for connection to MSSQLServer")
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
            .add("instance", Json.createObjectBuilder()
                .add("name", "Instance Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Instance name")
                    .add("de", "Instance name"))
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
            .add("database", Json.createObjectBuilder()
                .add("name", "Database name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Database name")
                    .add("de", "Database name"))
                .add("value", ""))
            .add("query", Json.createObjectBuilder()
                .add("name", "Query")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Query")
                    .add("de", "Query"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  columns : ['_query_column_name_1', '_query_column_name_2', ...]," + "\n"
                + "  data : [{" + "\n"
                + "    _query_column_name_1 : 'query_result_row_1_column_1'," + "\n"
                + "    _query_column_name_1 : 'query_result_row_1_column_2'," + "\n"
                + "    ..." + "\n"
                + "  }, {" + "\n"
                + "    _query_column_name_1 : 'query_result_row_2_column_1'," + "\n"
                + "    _query_column_name_2 : 'query_result_row_2_column_2'," + "\n"
                + "    ..." + "\n"
                + "  }]," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                ;
    }

    private String host, instance = "";
    private int port = 0;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        host = startConfiguration.getJsonObject("host")==null?"":startConfiguration.getJsonObject("host").getString("value", "");
        if(host.isEmpty()) throw new Exception("MSSQLServer host not provided");
        
        try {
            if(startConfiguration.getJsonObject("port")!=null && startConfiguration.getJsonObject("port").getString("value", ""+port).length()!=0)
                port = Integer.parseInt(startConfiguration.getJsonObject("port").getString("value", ""+port));
        }catch(Exception ex) {
            throw new Exception("The SQLServer port number is incorrect: '"+startConfiguration.getJsonObject("port").getString("value")+"'");
        }
        
        instance = startConfiguration.getJsonObject("instance")==null?"":startConfiguration.getJsonObject("instance").getString("value", "");
        if(instance.isEmpty()) throw new Exception("MSSQLServer instance name not provided");
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String username = configuration.getJsonObject("username")==null?"":configuration.getJsonObject("username").getString("value", "");
        if(username.isEmpty()) throw new Exception("MSSQLServer username not provided");
        String password = configuration.getJsonObject("password")==null?"":configuration.getJsonObject("password").getString("value", "");
        if(password.isEmpty()) throw new Exception("MSSQLServer password not provided");
        String database = configuration.getJsonObject("database")==null?"":configuration.getJsonObject("database").getString("value", "");
        String query = configuration.getJsonObject("query")==null?"":configuration.getJsonObject("query").getString("value", "");
        if(query.isEmpty()) throw new Exception("MSSQLServer query not provided");
        
        /*
        SQLServerDataSource dataSource = new SQLServerDataSource();
        dataSource.setServerName(host);
        if(port!=0) dataSource.setPortNumber(port);
        dataSource.setInstance(dbInstance);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        */
        
        JtdsDataSource dataSource = new JtdsDataSource();
        dataSource.setServerName(host);
        if(port!=0) dataSource.setPortNumber(port);
        dataSource.setDatabaseName(database);
        dataSource.setInstance(instance);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        
        JsonArrayBuilder columnList = Json.createArrayBuilder();
        JsonArrayBuilder dataList = Json.createArrayBuilder();
        
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                if(statement.execute()) {
                    try (ResultSet resultSet = statement.getResultSet()) {
                        ResultSetMetaData metadata = resultSet.getMetaData();
                        for(int i=1;i<=metadata.getColumnCount();i++)
                            columnList.add(metadata.getColumnName(i));
                        while (resultSet.next()) {
                            JsonObjectBuilder dataObject = Json.createObjectBuilder();
                            for(int i=1;i<=metadata.getColumnCount();i++)
                                if(resultSet.getString(i)==null)
                                    dataObject.add(metadata.getColumnName(i), JsonValue.NULL);
                                else
                                    dataObject.add(metadata.getColumnName(i), resultSet.getString(i));
                            dataList.add(dataObject);
                        }
                    }
                }
            }
        }
        
        return Json.createObjectBuilder()
            .add("columns", columnList)
            .add("data", dataList)
            .add("moreInfo", Json.createObjectBuilder().add("retrievalTime", Utils.getCurrentTime()))
            .build();
    }
    
    @Override
    public void stop() throws Exception {
        host = "";
        instance = "";
        port = 0;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        MSSQLServerConnector connector = new MSSQLServerConnector();
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("host", Json.createObjectBuilder().add("value", "127.0.0.1"))
                .add("port", Json.createObjectBuilder().add("value", ""))
                .add("instance", Json.createObjectBuilder().add("value", "ADOXX15EN"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("username", Json.createObjectBuilder().add("value", "sa"))
                .add("password", Json.createObjectBuilder().add("value", "12+*ADOxx*+34"))
                .add("database", Json.createObjectBuilder().add("value", "adoMY"))
                .add("query", Json.createObjectBuilder().add("value", "select * from ADONIS.class;"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
