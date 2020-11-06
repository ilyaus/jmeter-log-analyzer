package com.xlilium.perf.qa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Stack;

public class JMeterXMLLogParser extends DefaultHandler {
    private static final Logger log = LoggerFactory.getLogger(JMeterXMLLogParser.class);

    private JMeterResults jMeterResults = new JMeterResults();
    private JMeterSample jMeterSample;

    private Stack<String> currentElement = new Stack<>();
    private StringBuilder xmlText;

    private final String JMETER_SAMPLE_TAG = "httpSample";
    private final String JMETER_REQUEST_HEADER_TAG = "requestHeader";
    private final String JMETER_RESPONSE_HEADER_TAG = "responseHeader";
    private final String JMETER_ULR_TAG = "java.net.URL";
    private final String JMETER_TEST_RESULT_TAG = "testResults";

    private final int MAX_PRINT_COUNT = 0;
    private int global_print_count = 0;

    JMeterResults readDataFromXML(String fileName, String successCodes)
            throws ParserConfigurationException, SAXException, IOException {

        jMeterResults.setSuccessCodes(successCodes);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        try {
            parser.parse(new File(fileName), this);
        } catch (SAXParseException ex) {
            System.err.println("Failed to parse JMeter log file. ");
            System.err.println(ex.getMessage());

            ex.printStackTrace();
        }

        return this.jMeterResults;
    }

    @Override
    public void startDocument() {}

    @Override
    public void endDocument() {}

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        currentElement.push(qName);

        switch (qName) {
            case JMETER_SAMPLE_TAG:
                jMeterSample = new JMeterSample();

                // TODO: need to add load time and latency as a separate matrix
                jMeterSample.setLoadTime_t(getAttributeInt(attributes, "t", -1));
                jMeterSample.setLatency_lt(getAttributeInt(attributes, "t", -1));
                jMeterSample.setTimeStamp_ts(getAttributeLong(attributes, "ts", -1L));
                jMeterSample.setSuccess_s(getAttributeBoolean(attributes, "s", false));
                jMeterSample.setLabel_lb(getAttributeString(attributes, "lb", ""));
                jMeterSample.setResponseCode_rc(getAttributeString(attributes,"rc", ""));
                jMeterSample.setResponseMessage_rm(getAttributeString(attributes, "rm", ""));
                jMeterSample.setThreadName_tn(getAttributeString(attributes, "tn", ""));
                jMeterSample.setRecievedBytes_by(getAttributeInt(attributes, "by", -1));
                jMeterSample.setSentBytes_sby(getAttributeInt(attributes,"sby", -1));
                jMeterSample.setSampleCount_sc(getAttributeInt(attributes,"sc", -1));
                jMeterSample.setErrorCount_ec(getAttributeInt(attributes,"ec", -1));
                jMeterSample.setNg(getAttributeInt(attributes, "ng", -1));
                jMeterSample.setActiveThreadCount_na(Integer.parseInt(attributes.getValue("na")));

                jMeterSample.setBucketId();
                jMeterSample.setBucketTime();

                break;

            case JMETER_REQUEST_HEADER_TAG:
            case JMETER_RESPONSE_HEADER_TAG:
            case JMETER_ULR_TAG:
                xmlText = new StringBuilder();
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String lastElement = "";

        try {
            lastElement = currentElement.pop();
        } catch (EmptyStackException e) {
            e.printStackTrace();
        }

        if (JMETER_TEST_RESULT_TAG.equals(lastElement)) {
            return;
        }

        switch (lastElement) {
            case JMETER_SAMPLE_TAG:
                jMeterResults.addSample(jMeterSample);

                if (global_print_count < MAX_PRINT_COUNT) {
                    System.out.println("Read: "
                            + jMeterSample.getLabel_lb()
                            + " : " + jMeterSample.getBucketId()
                            + " : " + jMeterSample.getLatency_lt());
                    global_print_count++;
                }

                break;

            case JMETER_ULR_TAG:
                jMeterSample.setSampleUrl(xmlText.toString());
                break;

            case JMETER_REQUEST_HEADER_TAG:
                if (jMeterSample != null) {
                    jMeterSample.setPayloadSize(getContentLength(xmlText.toString()));
                }

            case JMETER_RESPONSE_HEADER_TAG:
                if (jMeterSample != null) {
                    jMeterSample.setPayloadSize(getContentLength(xmlText.toString()));
                }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (xmlText != null) {
            xmlText.append(ch, start, length);
        }
    }

    private Long getAttributeLong(Attributes attributes, String name, Long defaultValue) {
        Long retVal = defaultValue;
        try {
            retVal = Long.parseLong(attributes.getValue(name));
        } catch (NumberFormatException e) {
            // Do nothing
        }

        return retVal;
    }

    private int getAttributeInt(Attributes attributes, String name, int defaultValue) {
        int retVal = defaultValue;
        try {
            retVal = Integer.parseInt(attributes.getValue(name));
        } catch (NumberFormatException e) {
            // Do nothing
        }

        return retVal;
    }

    private String getAttributeString(Attributes attributes, String name, String defaultValue) {
        String retVal = defaultValue;
        try {
            retVal = attributes.getValue(name);
        } catch (Exception e) {
            // Do nothing
        }

        return retVal;
    }

    private boolean getAttributeBoolean(Attributes attributes, String name, Boolean defaultValue) {
        Boolean retVal = defaultValue;
        try {
            retVal = attributes.getValue(name).toLowerCase().equals("true");
        } catch (Exception e) {
            // Do nothing
        }

        return retVal;
    }

    private int getContentLength(String responseHeader) {
        return Integer.parseInt(
            Arrays.stream(responseHeader.split("\n"))
                .filter(s -> s.startsWith("Content-Length"))
                .findFirst().orElse("Content-Length: -1")
                .split(":")[1].trim()
        );
    }
}
