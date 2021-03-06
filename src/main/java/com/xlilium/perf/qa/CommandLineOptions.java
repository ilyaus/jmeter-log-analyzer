package com.xlilium.perf.qa;

import org.apache.commons.cli.*;

public class CommandLineOptions {
    static final String JMETER_LOG_FILE_XML = "jmeter-log-file";
    static final String JMETER_RAMP_DURATION = "jmeter-ramp-duration";
    static final String JMETER_SUCCESS_CODES = "jmeter-success-codes";

    private static CommandLine parseArguments(String[] args, Options options) {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Command line argument parsing failed. Reason: " + e.getMessage());
        }

        return null;
    }

    public static CommandLine getCommandLineArguments(final String[] args) {
        final Options options = new Options();

        final Option jmeterLogFileXml = new Option("l", JMETER_LOG_FILE_XML,
                true, "JMeter XML log file");
        jmeterLogFileXml.setRequired(false);
        options.addOption(jmeterLogFileXml);

        final Option jmeterRampDuration = new Option("d", JMETER_RAMP_DURATION,
                true, "JMeter ramp duration");
        jmeterRampDuration.setRequired(false);
        options.addOption(jmeterRampDuration);

        final Option jmeterSuccessCodes = new Option("c", JMETER_SUCCESS_CODES,
            true, "JMeter success codes");
        jmeterSuccessCodes.setRequired(false);
        options.addOption(jmeterSuccessCodes);


        return parseArguments(args, options);
    }

    public static String getDefault(String optionName) {
        String retValue = null;

        switch (optionName) {
            case JMETER_LOG_FILE_XML:
                retValue = "";
                break;
            case JMETER_RAMP_DURATION:
                retValue = "0";
                break;
            case JMETER_SUCCESS_CODES:
                retValue = "429";
                break;
        }

        return retValue;
    }
}
