/** LoggingLevel.java
 *
 * Copyright May 2018 Tideworks Technology
 * Author: Roger D. Voss
 * MIT License
 */
package com.tideworks.data_load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Supplier;

import static com.tideworks.data_load.DataLoad.getProgDirPath;

public enum LoggingLevel {
  TRACE("trace"), DEBUG("debug"), INFO("info"), WARN("warn"), ERROR("error");

  private static final Object lockObj = new Object();
  private static LoggingLevel defaultLogLevel = INFO;
  @SuppressWarnings("unused")
  private String level;
  private static ch.qos.logback.classic.Level originalRootLevel;

  LoggingLevel(String level) {
    this.level = level;
  }

  public static void setLoggingVerbosity(LoggingLevel level) {
    // Must be one of ("trace", "debug", "info", "warn", or "error")
    synchronized (lockObj) {
      defaultLogLevel = level;
    }
  }

  private static LoggingLevel getLoggingVerbosity() {
    synchronized (lockObj) {
      return defaultLogLevel;
    }
  }

  public static ch.qos.logback.classic.Logger getRootLogger() {
    return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
  }

  public static ch.qos.logback.classic.Level getOriginalRootLevel() {
    return originalRootLevel;
  }

  public static Logger effectLoggingLevel(final Supplier<Logger> createLogger) {
    String logbackCfgFileName = "logback.xml";
    File logbackCfgFile = new File(getProgDirPath(), logbackCfgFileName);
    if (!logbackCfgFile.exists()) {
      System.err.printf("LogBack config file \"%s\" not detected - defaulting to console logging", logbackCfgFileName);
      System.err.printf("Expected LogBack config file full pathname:%n\t\"%s\"", logbackCfgFile);
    }
    System.setProperty("logback.configurationFile", logbackCfgFile.toString());
    System.setProperty("program.directoryPath", getProgDirPath().toString());
    final ch.qos.logback.classic.Logger root = getRootLogger();
    originalRootLevel = root.getLevel();
    root.setLevel(ch.qos.logback.classic.Level.toLevel(getLoggingVerbosity().toString()));
    return createLogger.get();
  }
}