package com.xlilium.perf.qa;

public class JMeterSample {
    private int loadTime_t;
    private int  latency_lt;
    private long timeStamp_ts;
    private boolean success_s;
    private String label_lb;
    private String responseCode_rc;
    private String responseMessage_rm;
    private String threadName_tn;
    private int receivedBytes_by;
    private int sentBytes_sby;
    private int sampleCount_sc;
    private int errorCount_ec;
    private int ng;
    private int activeThreadCount_na;
    private int payloadSize;
    private int sentPayloadSize;
    private int receivedPayloadSize;
    private String sampleUrl;

    private long bucketId;
    private String bucketTime;

    public int getSentBytes_successful_sby() {
        return success_s ? getSentBytes_sby() : 0;
    }

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    public void setBucketId() {
        this.bucketId = this.timeStamp_ts / 1000;
    }

    public String getBucketTime() {
        return bucketTime;
    }

    public void setBucketTime(String bucketTime) {
        this.bucketTime = bucketTime;
    }

    public void setBucketTime() {
        this.bucketTime = new java.util.Date(timeStamp_ts).toString();
    }

    public int getLoadTime_t() {
        return loadTime_t;
    }

    public void setLoadTime_t(int loadTime_t) {
        this.loadTime_t = loadTime_t;
    }

    public int getLatency_lt() {
        return latency_lt;
    }

    public void setLatency_lt(int latency_lt) {
        this.latency_lt = latency_lt;
    }

    public long getTimeStamp_ts() {
        return timeStamp_ts;
    }

    public void setTimeStamp_ts(long timeStamp_ts) {
        this.timeStamp_ts = timeStamp_ts;
    }

    public boolean isSuccess_s() {
        return success_s;
    }

    public void setSuccess_s(boolean success_s) {
        this.success_s = success_s;
    }

    public String getLabel_lb() {
        return label_lb;
    }

    public void setLabel_lb(String label_lb) {
        this.label_lb = label_lb;
    }

    public String getResponseCode_rc() {
        return responseCode_rc;
    }

    public void setResponseCode_rc(String responseCode_rc) {
        this.responseCode_rc = responseCode_rc;
    }

    public String getResponseMessage_rm() {
        return responseMessage_rm;
    }

    public void setResponseMessage_rm(String responseMessage_rm) {
        this.responseMessage_rm = responseMessage_rm;
    }

    public String getThreadName_tn() {
        return threadName_tn;
    }

    public void setThreadName_tn(String threadName_tn) {
        this.threadName_tn = threadName_tn;
    }

    public int getRecievedBytes_by() {
        return receivedBytes_by;
    }

    public void setRecievedBytes_by(int recievedBytes_by) {
        this.receivedBytes_by = recievedBytes_by;
    }

    public int getSentBytes_sby() {
        return sentBytes_sby;
    }

    public void setSentBytes_sby(int sentBytes_sby) {
        this.sentBytes_sby = sentBytes_sby;
    }

    public int getSampleCount_sc() {
        return sampleCount_sc;
    }

    public void setSampleCount_sc(int sampleCount_sc) {
        this.sampleCount_sc = sampleCount_sc;
    }

    public int getErrorCount_ec() {
        return errorCount_ec;
    }

    public void setErrorCount_ec(int errorCount_ec) {
        this.errorCount_ec = errorCount_ec;
    }

    public int getNg() {
        return ng;
    }

    public void setNg(int ng) {
        this.ng = ng;
    }

    public int getActiveThreadCount_na() {
        return activeThreadCount_na;
    }

    public void setActiveThreadCount_na(int activeThreadCount_na) {
        this.activeThreadCount_na = activeThreadCount_na;
    }

    public String getSampleUrl() {
        return sampleUrl;
    }

    public void setSampleUrl(String sampleUrl) {
        this.sampleUrl = sampleUrl;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    public String getSampleUrlParam(String urlParameter) {
        try {
            String[] urlParams = getSampleUrl().split("\\?");

            if (urlParams.length == 2) {
                String[] params = urlParams[1].split("&");
                for (String param : params) {
                    String[] keyValuePair = param.split("=");
                    if (keyValuePair[0].equals(urlParameter)) {
                        return keyValuePair[1];
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to get URL parameter " + urlParameter + "  from " + getSampleUrl());
        }

        return "";
    }
}
