package org.adoxx.microservice.api.log.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.adoxx.microservice.api.log.LogI;
import org.adoxx.microservice.utils.Utils;

public class FileBasedLogSaver implements LogI{

    private String defaultLogFile = null;
    
    public FileBasedLogSaver(String logFile) {
        if(logFile!=null) {
            this.defaultLogFile = logFile;
        }
    }
    
    public FileBasedLogSaver() {
        this(System.getProperty("java.io.tmpdir")+"/ADOxx_micro_logs/microservice.log");
    }
    
    @Override
    public void log(LogLevel logLevel, String message, Throwable exception) {
        StringWriter exceptionStringWriter = new StringWriter();
        if(exception != null) {
            exception.printStackTrace(new PrintWriter(exceptionStringWriter));
            exceptionStringWriter.append('\n');
            exception.printStackTrace();
        }
        String toWrite = Utils.getCurrentTime() + " - [" + logLevel + "] - " + message + "\n" + exceptionStringWriter.toString();
        try {
            Utils.writeFile(toWrite.getBytes("UTF-8"), defaultLogFile, true);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
