package org.adoxx.microservice.api.log;

import org.adoxx.microservice.api.log.impl.FileBasedLogSaver;

public class LogManager implements LogI {
    private static LogManager uniqueLogManager = null;
    
    static {
        uniqueLogManager = new LogManager();
        uniqueLogManager.setLogHandler(new FileBasedLogSaver());
    }
    
    public static LogManager unique() {
        return uniqueLogManager;
    }
    
    private LogI logHandler = null;
    
    public void setLogHandler(LogI logHandler) {
        this.logHandler = logHandler;
    }

    @Override
    public void log(LogLevel logLevel, String message, Throwable exception) {
        if(logHandler == null) return;
        logHandler.log(logLevel, message, exception);
    }
}
