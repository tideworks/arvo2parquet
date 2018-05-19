/** DataLoad.java
 *
 * Based on example code snippet ParquetReaderWriterWithAvro.java located on github at:
 *
 * https://github.com/MaxNevermind/Hadoop-snippets/blob/master/src/main/java/org/maxkons/hadoop_snippets/parquet/ParquetReaderWriterWithAvro.java
 *
 * Original example code author: Max Konstantinov https://github.com/MaxNevermind
 *
 * Extensively refactored by: Roger D. Voss, https://github.com/roger-dv, Tideworks Technology, May 2018
 *
 * NOTES:
 * Original example wrote 2 Avro dummy test data items to a Parquet file.
 * The refactored implementation uses an iteration loop to write a default of 10
 * Avro dummy test day items and will accept a count as passed as a command line
 * argument.
 * The test data strings are now generated by RandomString class to a size of 64
 * characters.
 * Still uses the original avroToParquet.avsc schema by which to describe the Avro
 * dummy test data.
 * The most significant enhancements is where the code now calls these two methods:
 *
 * nioPathToOutputFile() and nioPathToInputFile()
 *
 * nioPathToOutputFile() accepts a Java nio Path to a standard file system file path
 * and returns an org.apache.parquet.io.OutputFile (which is accepted by the
 * AvroParquetWriter builder).
 *
 * nioPathToInputFile() accepts a Java nio Path to a standard file system file path
 * and returns an org.apache.parquet.io.InputFile (which is accepted by the
 * AvroParquetReader builder).
 *
 * These methods provide implementations of these two OutputFile and InputFile adaptors
 * that make it possible to write Avro data to Parquet formatted file residing in the
 * conventional file system (i.e., a plain file system instead of the Hadoop
 * hdfs file system) and then read it back.
 *
 * Usecase: Dremio can be incrementally loaded with data provided in Parquet format files.
 *
 * It is an easy matter to adapt this approach to work with JSON input data - just
 * synthesize an appropriate Avro schema to describe the JSON data, put the JSON data
 * into an Avro GenericData.Record and write it out.
 *
 * NOTES ON RUNNING PROGRAM:
 *
 * HADOOP_HOME environment variable should be defined to prevent an exception from being
 * thrown - code will continue to execute properly but defining this squelches it. This is
 * down in the bowels of Hadoop/Parquet library implementation - not behavior from the
 * application code.
 *
 * HOME environment variable may defined. The program will look for logback.xml there
 * and will write the Parquet file it generates to there. Otherwise the program will
 * use the current working directory.
 *
 * In logback.xml, the filters on the ConsoleAppender and RollingFileAppender should be
 * adjusted to modify verbosity level of logging. The defaults are set to INFO level. The
 * intent is to allow, say, setting file appender to DEBUG while console is set to INFO.
 *
 * The only command line argument accepted is the specification of how many iterations
 * of writing Avro records; the default is 10.
 *
 * Can use the shell script run.sh to invoke the program from the Maven target/ directory.
 * Logging will go into a logs/ directory as the file avro2parquet.log.
 */
package com.tideworks.data_load;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.tideworks.data_load.io.InputFile.nioPathToInputFile;
import static com.tideworks.data_load.io.OutputFile.nioPathToOutputFile;

/*
    Example of reading writing Parquet in java without BigData tools.
*/
public class DataLoad {
  private static final Logger LOGGER;
  private static final String SCHEMA_FILE_NAME = "avroToParquet.avsc";
  private static final String loadSchemaRsrcErrMsgFmt = "Can't read SCHEMA file from: \"{}\"";
  private static final File progDirPathFile;

  static File getProgDirPath() { return progDirPathFile; }

  static {
    final Predicate<String> existsAndIsDir = dirPath -> {
      final File dirPathFile = new File(dirPath);
      return dirPathFile.exists() && dirPathFile.isDirectory();
    };
    String homeDirPath = System.getenv("HOME"); // user home directory
    homeDirPath = homeDirPath != null && !homeDirPath.isEmpty() && existsAndIsDir.test(homeDirPath) ? homeDirPath : ".";
    progDirPathFile = FileSystems.getDefault().getPath(homeDirPath).toFile();
    LoggingLevel.setLoggingVerbosity(LoggingLevel.DEBUG);
    LOGGER = LoggingLevel.effectLoggingLevel(() -> LoggerFactory.getLogger(DataLoad.class.getSimpleName()));
  }

  private static Schema getSchema(String schemaRsrcPath) {

    final Function<String, String> get_schema_rsrc = rsrcPath -> {
      try (final InputStream is = ClassLoader.getSystemResourceAsStream(rsrcPath)) {
        assert is != null;
        try (@SuppressWarnings("resource") final Scanner s = new Scanner(is).useDelimiter("\\A")) {
          return s.hasNext() ? s.next() : "";
        }
      } catch (Throwable e) {
        LOGGER.error(loadSchemaRsrcErrMsgFmt, rsrcPath);
        LOGGER.error("exiting program:", e);
        System.exit(1);
      }
      return null; // will never get here - hushes compiler
    };

    return new Schema.Parser().parse(get_schema_rsrc.apply(schemaRsrcPath));
  }

  public static void main(String[] args) {
    try {
      final int maxRecords = args.length > 0 ? Integer.parseUnsignedInt(args[0]) : 10;
      final Schema schema = getSchema(SCHEMA_FILE_NAME);
      final Path parquetFilePath = FileSystems.getDefault().getPath("sample.parquet");
      Files.deleteIfExists(parquetFilePath);
      doTestParquet(schema, maxRecords, parquetFilePath);
    } catch (Throwable e) {
      LOGGER.error("program terminated due to exception:", e);
      System.exit(1); // return non-zero status to indicate program failure
    }
  }

  private static void doTestParquet(final Schema schema, final int maxRecords, final Path parquetFilePath)
          throws IOException
  {
    final List<GenericData.Record> sampleData = new ArrayList<>();
    final RandomString session = new RandomString(64);

    for(int i = 1; i <= maxRecords; i++) {
      GenericData.Record record = new GenericData.Record(schema);
      record.put("c1", i);
      record.put("c2", session.nextString());
      sampleData.add(record);
    }

    writeToParquet(schema, sampleData, parquetFilePath);
    readFromParquet(parquetFilePath);
  }

  private static void writeToParquet(@Nonnull Schema schema,
                                     @Nonnull List<GenericData.Record> recordsToWrite,
                                     @Nonnull Path fileToWrite) throws IOException
  {
    try (final ParquetWriter<GenericData.Record> writer = AvroParquetWriter
            .<GenericData.Record>builder(nioPathToOutputFile(fileToWrite))
            .withRowGroupSize(256 * 1024 * 1024)
            .withPageSize(128 * 1024)
            .withSchema(schema)
            .withConf(new Configuration())
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withValidation(false)
            .withDictionaryEncoding(false)
            .build())
    {
      for (GenericData.Record record : recordsToWrite) {
        writer.write(record);
      }
    }
  }

  private static void readFromParquet(@Nonnull Path filePathToRead) throws IOException {
    try (final ParquetReader<GenericData.Record> reader = AvroParquetReader
            .<GenericData.Record>builder(nioPathToInputFile(filePathToRead))
            .withConf(new Configuration())
            .build())
    {
      GenericData.Record record;
      while ((record = reader.read()) != null) {
        System.out.println(record);
      }
    }
  }
}