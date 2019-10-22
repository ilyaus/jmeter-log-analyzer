package com.xlilium.perf.qa;

import java.util.ArrayList;
import java.util.List;

public class JMeterSampleBucket {
    private long bucketId;
    private String bucketTime;
    private String label;

    private long totalBytesSent;
    private long totalPayloadBytesSent;

    private int bucketSampleCount;
    private int bucketSuccessfulSampleCount;
    private int bucketFailedSampleCount;

    private List<Integer> latencies = new ArrayList<>();
    private List<Integer> activeThreads = new ArrayList<>();

    private List<String> returnCodes = new ArrayList<>();

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    public String getBucketTime() {
        return bucketTime;
    }

    public void setBucketTime(String bucketTime) {
        this.bucketTime = bucketTime;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getBucketSampleCount() {
        return bucketSampleCount;
    }

    public void setBucketSampleCount(int bucketSampleCount) {
        this.bucketSampleCount = bucketSampleCount;
    }

    public long getTotalBytesSent() {
        return totalBytesSent;
    }

    public void setTotalBytesSent(long totalBytesSent) {
        this.totalBytesSent = totalBytesSent;
    }

    public long getTotalPayloadBytesSent() {
        return totalPayloadBytesSent;
    }

    public void setTotalPayloadBytesSent(long totalPayloadBytesSent) {
        this.totalPayloadBytesSent = totalPayloadBytesSent;
    }

    public int getBucketSuccessfulSampleCount() {
        return bucketSuccessfulSampleCount;
    }

    public void setBucketSuccessfulSampleCount(int bucketSuccessfulSampleCount) {
        this.bucketSuccessfulSampleCount = bucketSuccessfulSampleCount;
    }

    public int getBucketFailedSampleCount() {
        return bucketFailedSampleCount;
    }

    public void setBucketFailedSampleCount(int bucketFailedSampleCount) {
        this.bucketFailedSampleCount = bucketFailedSampleCount;
    }

    public List<Integer> getLatencies() {
        return latencies;
    }

    public void setLatencies(List<Integer> latencies) {
        this.latencies = latencies;
    }

    public List<Integer> getActiveThreads() {
        return activeThreads;
    }

    public void addLatency(int latency) {
        this.latencies.add(latency);
    }

    public void addActiveThreads(int na) {
        this.activeThreads.add(na);
    }

    public void addReturnCode(String returnCode) {
        this.returnCodes.add(returnCode);
    }

    public List<String> getReturnCodes() {
        return returnCodes;
    }
}
