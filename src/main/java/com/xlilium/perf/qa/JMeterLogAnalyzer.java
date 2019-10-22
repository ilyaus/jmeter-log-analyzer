package com.xlilium.perf.qa;

import com.xlilium.perf.aws.AwsS3;
import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.xlilium.perf.qa.CommandLineOptions.JMETER_LOG_FILE_XML;
import static com.xlilium.perf.qa.CommandLineOptions.JMETER_RAMP_DURATION;
import static java.lang.System.exit;

public class JMeterLogAnalyzer {
  private String jMeterXMLLog;
  private int jMeterRampDuration;
  private String baseFolder = "";
  private String currentTime;

  private static final Logger LOG = LoggerFactory.getLogger(JMeterLogAnalyzer.class);

  private JMeterResults jMeterResults = new JMeterResults();
  private JMeterResultsConfig jMeterResultsConfig = new JMeterResultsConfig("application.conf");

  /*
  If JMeter log file is passed as a parameter, that file should be processed.
  If no parameters are passed, JMeter logs are checked and processed from S3 location
   */
  public void run(String[] args) {
    CommandLine commandLine = CommandLineOptions.getCommandLineArguments(args);

    if (commandLine.hasOption(JMETER_LOG_FILE_XML)) {
      processLocalLog(commandLine);
    } else {
      processS3Logs();
    }
  }

  private String processLocalLog(CommandLine commandLine) {
    jMeterXMLLog = commandLine.hasOption(JMETER_LOG_FILE_XML) ?
            commandLine.getOptionValue(JMETER_LOG_FILE_XML) :
            CommandLineOptions.getDefault(JMETER_LOG_FILE_XML);

    jMeterRampDuration =
            Integer.parseInt(commandLine.hasOption(JMETER_RAMP_DURATION) ?
                    commandLine.getOptionValue(JMETER_RAMP_DURATION) :
                    CommandLineOptions.getDefault(JMETER_RAMP_DURATION));

    return processLocalLog(jMeterXMLLog, jMeterRampDuration);
  }

  private String processLocalLog(String logFolder) {
    String logFileName = jMeterResultsConfig.getConfig("jmeter-logs.result-file");
    String infoFileName = jMeterResultsConfig.getConfig("jmeter-logs.info-file");

    LOG.info("Processing results. File: {}", logFolder + File.separator + logFileName);
    LOG.info("Processing results. Ramp Duration: {}",
            getJMeterRampDuration(logFolder + File.separator + infoFileName));

    return processLocalLog(logFolder + File.separator + logFileName,
            getJMeterRampDuration(logFolder + File.separator + infoFileName));
  }

  private String processLocalLog(String jMeterXMLLog, int jMeterRampDuration) {
    baseFolder = Paths.get(jMeterXMLLog).getParent().toString();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd_HHmmss");
    currentTime = sdf.format(new Date());
    String reportFileName = baseFolder + File.separator + "report-" + currentTime + ".json";

    JMeterXMLLogParser jMeterXMLLogParser = new JMeterXMLLogParser();

    try {
      jMeterResults = jMeterXMLLogParser.readDataFromXML(validateAndFixXML(jMeterXMLLog));

      // System.out.println("Finished reading log file: " + jMeterResults.getSampleCount());

      jMeterResults.addStats("HTTP_Baseload_Request");
      jMeterResults.addStats("HTTP_Ramp_Request", jMeterRampDuration);

      jMeterResults.writeJsonReport(reportFileName);

    } catch (FileNotFoundException ex) {
      LOG.error("Needed file not found in logs.");
      ex.printStackTrace();

      // Try to create report file anyway
      LOG.info("Creating empty report file.");
      jMeterResults.writeJsonReport(reportFileName);

    } catch (Exception e) {
      LOG.error("Error parsing JMeter log file. " + e.getMessage());
      e.printStackTrace();

      // Don't exit, let the program complete.
      // exit(1);
    }

    return reportFileName;
  }

  private void processS3Logs() {
        /*
        Basic algorithm:
            For all log files in the input folder:
                Download and Unzip log file
                Process jmeter log file (result should be saved in the same folder)
                Upload report and metadata to the results folder
                Zip everything and upload to processed folder
                Delete log from local temp folder
                Delete log from input folder
         */

    LOG.info("S3: " + jMeterResultsConfig.getConfig("s3.name"));
    LOG.info("S3 input: " + jMeterResultsConfig.getConfig("s3-input.key"));
    LOG.info("S3 processed: " + jMeterResultsConfig.getConfig("s3-processed.key"));
    LOG.info("S3 results: " + jMeterResultsConfig.getConfig("s3-results.key"));
    LOG.info("Tmp folder: " + jMeterResultsConfig.getConfig("tmp-folder.name"));
    LOG.info("JMeter files to upload: " + jMeterResultsConfig.getStringList("jmeter-files-to-upload"));

    final String s3Name = jMeterResultsConfig.getConfig("s3.name");
    final String s3Input = jMeterResultsConfig.getConfig("s3-input.key");
    final String s3Processed = jMeterResultsConfig.getConfig("s3-processed.key");
    final String s3Results = jMeterResultsConfig.getConfig("s3-results.key");
    final String tmpFolder = jMeterResultsConfig.getConfig("tmp-folder.name");
    final List<String> resultFilesToUpload = jMeterResultsConfig.getStringList("jmeter-files-to-upload");


    AwsS3 awsS3 = new AwsS3(s3Name);

    List<String> logFileList = awsS3.downloadLogFiles(".zip", s3Input, tmpFolder);

    LOG.info("Found files: " + logFileList.toString());

    for (String file : logFileList) {
      if (unzipLogFile(file)) {
        String unzippedFolder = file.substring(0, file.lastIndexOf("."));
        String folderName = unzippedFolder.substring(
                unzippedFolder.lastIndexOf("/") + 1, unzippedFolder.length());
        List<String> filesToUpload = new ArrayList<>();

        String reportFile = processLocalLog(unzippedFolder);

        filesToUpload.add(reportFile);

        for (String n : resultFilesToUpload) {
          String fullFileName = unzippedFolder + File.separator + n;
          File f = new File(fullFileName);
          if (f.exists() && !f.isDirectory()) {
            filesToUpload.add(fullFileName);
          }
        }

        try {
          addToZippedFile(file, folderName, reportFile);
        } catch (ZipError ex) {
          LOG.error("Could not add " + reportFile + " to " + file);
          LOG.error("File will be archived without report");
          LOG.error(ex.getMessage());
          ex.printStackTrace();
        }

        awsS3.upload(s3Processed, file);
        awsS3.upload(s3Results + "/" + folderName, filesToUpload);
        awsS3.upload(s3Results + "/" + folderName, file);

        awsS3.delete(s3Input, Paths.get(file).getFileName().toString());

        delete(new File(file));
        delete(new File(unzippedFolder));
      } else {
        LOG.error("Failed to unzip the log file");
        exit(1);
      }
    }
  }

  private boolean unzipLogFile(String zipFileName) {
    String destFolder = Paths.get(zipFileName).getParent().toString();

    LOG.info("Unzipping {} to folder: {}", zipFileName, destFolder);

    byte[] read_buf = new byte[1024];

    try {
      File folder = new File(destFolder);

      if (!folder.exists()) {
        LOG.info("Creating folder {}", folder.getName());
        boolean result = folder.mkdir();
        LOG.info("Folder created: {}", result);
      }

      ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFileName));
      ZipEntry ze = zis.getNextEntry();

      while (ze != null) {
        String fileName = ze.getName();

        if (!fileName.endsWith("/") && !fileName.endsWith("\\")) {
          LOG.info("Unzipping file {}", fileName);
          File newFile = new File(destFolder + File.separator + fileName);

          new File(newFile.getParent()).mkdirs();
          FileOutputStream fos = new FileOutputStream(newFile);

          int read_len;
          while ((read_len = zis.read(read_buf)) > 0) {
            fos.write(read_buf, 0, read_len);
          }

          fos.close();
        } else {
          LOG.info("Skipping, entry is folder {}", fileName);
        }


        zis.closeEntry();
        ze = zis.getNextEntry();
      }

      zis.closeEntry();
      zis.close();

    } catch (FileNotFoundException e) {
      LOG.error("Cannot find file while unzipping {} ({})", zipFileName, e.getMessage());
      return false;
    } catch (IOException e) {
      LOG.error(e.getMessage());
      return false;
    }

    return true;
  }

  private boolean addToZippedFile(String zippedFile, String pathInZippedFile, String fileToAdd) {
    LOG.info("Adding to zip, zip file: {}", zippedFile);
    LOG.info("Adding to zip, path in zipped file: {}", pathInZippedFile);
    LOG.info("Adding to zip, file to add: {}", fileToAdd);

    Map<String, String> env = new HashMap<>();
    env.put("create", "true");

    URI uri = URI.create("jar:file:" + zippedFile);

    try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
      Path externalFile = Paths.get(fileToAdd);
      Path pathInZipFile = zipfs.getPath(
              File.separator +
                      pathInZippedFile +
                      File.separator +
                      Paths.get(fileToAdd).getFileName().toString());

      Files.copy(externalFile, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);

      return true;
    } catch (IOException e) {
      LOG.error("Error adding to zip file ({})", e.getMessage());
      e.printStackTrace();

      return false;
    }
  }

  private void delete(File file) {
    File[] files = file.listFiles();

    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          delete(f);
        } else {
          LOG.info("Deleting {}", f.getAbsolutePath());
          if (!f.delete()) {
            LOG.error("Failed to delete " + f.getAbsolutePath());
          }
        }
      }
    }
    LOG.info("Deleting {}", file.getAbsolutePath());
    if (!file.delete()) {
      LOG.error("Failed to delete " + file.getAbsolutePath());
    }
  }

  private int getJMeterRampDuration(String infoFileName) {
    int retVal = 0;

    try {
      retVal = Files.lines(Paths.get(infoFileName))
              .filter(line -> line.startsWith(jMeterResultsConfig.getConfig("info-keys.step-duration")))
              .findAny()
              .map(line -> Integer.parseInt(filterNumbers(line.substring(line.indexOf(":") + 1, line.length()))))
              .orElse(0);

    } catch (IOException e) {
      LOG.error("Error reading info file {} ({})", infoFileName, e.getMessage());
      exit(1);
    }

    return retVal;
  }

  private String filterNumbers(String s) {

        /*
        return s.chars()
                .filter(Character::isDigit)
                .mapToObj(c -> (char)c)
                .collect(Collectors.toList())
                .toString();
        */

    StringBuilder b = new StringBuilder(s.length());

    s.chars().filter(Character::isDigit).forEach(c -> b.append((char) c));

    return b.toString();
  }

  private String validateAndFixXML(String xmlFileName) {
    Pattern open = Pattern.compile("^<([^/|?][a-zA-Z.]*).*>.*");
    Pattern close = Pattern.compile("</(.*)>");

        /*
        Path path = Paths.get(xmlFileName);

        try(Stream<String> lines = Files.lines(path)) {
            lines.map(open::matcher).filter(Matcher::matches)
                    .forEach(matcher -> System.out.println(matcher.group(1)));
            //lines.map(close::matcher).filter(Matcher::matches).forEach(matcher -> System.out.println(matcher.group(1)));
        }
        */

    String tmpFile = xmlFileName + ".tmp";
    int errorCount = 0;

    try (BufferedReader br = new BufferedReader(new FileReader(xmlFileName))) {
      String line = "";
      Stack<String> st = new Stack<>();

      try (BufferedWriter outFileBw = new BufferedWriter(new FileWriter(tmpFile))) {

        while ((line = br.readLine()) != null) {
          Matcher open_m = open.matcher(line.trim());
          Matcher close_m = close.matcher(line.trim());

          if (open_m.find()) {
            LOG.info("Found opening tag: {}", open_m.group(1));
            st.push(open_m.group(1));

            outFileBw.write(line + "\n");

          }

          if (close_m.find()) {
            if (st.peek().equals(close_m.group(1))) {
              LOG.info("Found matching closing tag: {}", close_m.group(1));
              st.pop();

              outFileBw.write(line + "\n");
            } else {
              LOG.warn("Error: Cannot find closing tag for " + open_m.group(1) + ". Adding it");
              outFileBw.write("</" + open_m.group(1) + ">\n");

              errorCount++;
            }
          } else {
            outFileBw.write(line + "\n");
          }
        }

        if (!st.empty()) {
          LOG.warn("Error: Missing closing tags:");
          while (!st.empty()) {
            String missingTag = st.pop();
            LOG.warn("Adding " + missingTag);
            outFileBw.write("</" + missingTag + ">\n");

            errorCount++;
          }
        }

      } catch (IOException ex) {
        ex.printStackTrace();
      } catch (IllegalStateException ex) {
        LOG.warn("Failed to verify the file. Will assume it is valid because it is most " +
            "likely problem with validation (which should be addressed.");
        LOG.warn("Current line: {}", line);

        File tmpFileObject = new File(tmpFile);

        if (tmpFileObject.delete()) {
          LOG.warn("Deleted temp file {}", tmpFile);
        }

        return xmlFileName;
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    if (errorCount > 0) {
      File oldFile = new File(xmlFileName);
      File newFile = new File(tmpFile);

      if (oldFile.delete()) {
        if (newFile.renameTo(oldFile)) {
          LOG.warn("Renamed " + oldFile.getName() + " to " + newFile.getName());
        }
      }
    }

    return xmlFileName;
  }
}
