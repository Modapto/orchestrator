package org.adoxx.microservice.api.log;

public interface LogI {
    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }
    
    public void log(LogLevel logLevel, String message, Throwable exception);
}
