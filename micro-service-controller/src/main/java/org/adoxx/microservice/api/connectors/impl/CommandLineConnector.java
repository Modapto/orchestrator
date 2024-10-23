package org.adoxx.microservice.api.connectors.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.json.Json;
import javax.json.JsonObject;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

public class CommandLineConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "CommandLine Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Module for connection to a command line executable")
            .add("de", "Module for connection to a command line executable")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("executable", Json.createObjectBuilder()
                .add("name", "Executable")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Name of the command line executable"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("arguments", Json.createObjectBuilder()
                .add("name", "Arguments")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The arguments to pass to the executable"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  stdout : \"\"," + "\n"
                + "  stderr : \"\"," + "\n"
                + "  exitCode : 0" + "\n"
                + "}" + "\n"
                ;
    }
    
    private String executable = "";
    
    public static int commandLineMaxExecTimeInMinutes = 1;
    public static String commandLineExecPath = "";

    private String revealExecutable(String executable) throws Exception {
        String filePath = commandLineExecPath + ((commandLineExecPath.endsWith("\\") || commandLineExecPath.endsWith("/"))?"":"/") + executable;
        File file = new File(filePath);
        if(!commandLineExecPath.isEmpty() && !file.getCanonicalPath().startsWith(new File(commandLineExecPath).getCanonicalPath()))
            throw new Exception("Security Exception: Is not allowed to execute the file " + file.getCanonicalPath());
        if(!file.isFile())
            throw new Exception("The required executable don't exist: " + executable);
        return filePath;
    }

    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        executable = startConfiguration.getJsonObject("executable")==null?"":startConfiguration.getJsonObject("executable").getString("value", "");
        if(executable.isEmpty()) throw new Exception("Executable not provided");
        executable = revealExecutable(executable);
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String arguments = configuration.getJsonObject("arguments")==null?"":configuration.getJsonObject("arguments").getString("value", "");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        CommandLine cmdLine = new CommandLine(executable);
        if(!arguments.isEmpty())
            cmdLine.addArguments(arguments);

        DefaultExecutor exec = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(1000*60*commandLineMaxExecTimeInMinutes);
        
        exec.setWatchdog(watchdog);
        exec.setStreamHandler(streamHandler);
        exec.setExitValues(null);
        int exitCode = exec.execute(cmdLine);
        
        String stdout = outputStream.toString();
        String stderr = errorStream.toString();

        if(watchdog.killedProcess())
            throw new Exception("ERROR: Timeout occurred. '"+executable+"'' has reached the execution time limit of "+commandLineMaxExecTimeInMinutes+" minutes, so it has been aborted.\nPartial stdout:\n"+stdout+"\nPartial stderr:\n"+stderr);
        
        return Json.createObjectBuilder()
            .add("stdout", stdout)
            .add("stderr", stderr)
            .add("exitCode", exitCode)
            .build();
    }
    
    @Override
    public void stop() throws Exception {
        executable = "";
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        CommandLineConnector connector = new CommandLineConnector();
        commandLineExecPath = "";
        try{
            connector.threadStart(Json.createObjectBuilder()
                //.add("executable", Json.createObjectBuilder().add("value", "C:\\Windows\\System32\\calc.exe"))
                .add("executable", Json.createObjectBuilder().add("value", "D:\\COGITO\\UEDIN Integration\\gams.bat"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("arguments", Json.createObjectBuilder().add("value", "COGITO1_PWL_AccessGDX_v2.gms"))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
