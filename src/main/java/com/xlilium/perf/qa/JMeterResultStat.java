package com.xlilium.perf.qa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JMeterResultStat {
    private static final Logger LOG = LoggerFactory.getLogger(JMeterResultStat.class);

    private String statLabel;
    private long numberOfRequestsSent;
    private long numberOfBytesSent;
    private long numberOfPayloadBytesSent;
    private long numberOfSuccessfulRequests;
    private long numberOfFailedRequests;

    private transient List<Integer> latencies = new ArrayList<>();
    private transient List<Integer> activeThreads = new ArrayList<>();
    private transient List<String> returnCodes = new ArrayList<>();

    private double lt_max = -1;
    private double lt_min = Double.MAX_VALUE;
    private double lt_avg = -1;
    private double lt_p98, lt_p90 = -1;
    private double lt_stddev = -1;

    private int durationInSeconds;
    private double throughputMBps = -1;
    private double payloadThroughputMBps = -1;
    private double reqPerSecondTotal = -1;
    private double reqPerSecondSuccess = -1;
    private double reqPerSecondFailed = -1;

    private double numberOfActiveThreads = -1;

    private Map<String, Integer> returnCodeStats = new HashMap<>();

    private long startTimeTicks = Long.MAX_VALUE;
    private long endTimeTicks = 0;

    private String startTime = "";
    private String endTime = "";

    JMeterResultStat (String label, int durationInSeconds) {
        this.statLabel = label;
        this.durationInSeconds = durationInSeconds;
    }

    public void addBucket(JMeterSampleBucket bucket) {
        numberOfRequestsSent += bucket.getBucketSampleCount();
        numberOfBytesSent += bucket.getTotalBytesSent();
        numberOfPayloadBytesSent += bucket.getTotalPayloadBytesSent();
        numberOfSuccessfulRequests += bucket.getBucketSuccessfulSampleCount();
        numberOfFailedRequests += bucket.getBucketFailedSampleCount();

        latencies.addAll(bucket.getLatencies());
        activeThreads.addAll(bucket.getActiveThreads());

        startTimeTicks = Math.min(startTimeTicks, bucket.getBucketId());
        endTimeTicks = Math.max(endTimeTicks, bucket.getBucketId());

        returnCodes.addAll(bucket.getReturnCodes());
    }

    public void printStat() {
        // updateStats();

        System.out.println(statLabel + " : "
                + numberOfSuccessfulRequests + " : "
                + numberOfFailedRequests + " : "
                + "max " + lt_max + " : "
                + "min " + lt_min + " : "
                + "avg " + lt_avg + " : "
                + "p98 " + lt_p98 + " : "
                + "p90 " + lt_p90 + " : "
                + "std dev " + lt_stddev + " : ");
        System.out.println("Duration in sec: " + durationInSeconds);
        System.out.println("Throughput: " + throughputMBps);
        //System.out.println(latencies);

        System.out.println();
    }

    public JMeterResultStat updateStats() {
        LOG.info("Updating stats. Latencies count: {}", latencies.size());

        if (latencies.size() > 0) {
            latencies.sort(Comparator.comparing(Integer::intValue));

            lt_min = latencies.get(0);
            lt_max = latencies.get(latencies.size() - 1);
            lt_avg = avgOf(latencies);
            lt_p90 = latencies.get(latencies.size() == 1 ? 0 : Math.round(90 * latencies.size() / (float)100) - 1);
            lt_p98 = latencies.get(latencies.size() == 1 ? 0 : Math.round(98 * latencies.size() / (float)100) - 1);
            lt_stddev = calcStdDev(latencies, lt_avg);
        }

        throughputMBps = (((double) numberOfBytesSent) / (double) durationInSeconds) / 1000000.0;
        payloadThroughputMBps = (((double) numberOfPayloadBytesSent) / (double) durationInSeconds) / 1000000.0;

        reqPerSecondTotal = numberOfRequestsSent / (double) durationInSeconds;
        reqPerSecondSuccess = numberOfSuccessfulRequests / (double) durationInSeconds;
        reqPerSecondFailed = numberOfFailedRequests/ (double) durationInSeconds;

        numberOfActiveThreads = avgOf(activeThreads);

        startTime = new Date(startTimeTicks * 1000L).toString();
        endTime = new Date(endTimeTicks * 1000L).toString();

        returnCodeStats = getRcStats(returnCodes);

        return this;
    }

    private double sumOf(List<Integer> values) {
        double sum = 0;

        for (int value : values)  {
            sum += value;
        }

        return sum;
    }

    private double avgOf(List<Integer> values) {
        return values.size() != 0 ? sumOf(values) / values.size() : -1;
    }

    private double calcStdDev(List<Integer> values, double avg) {
        double stddev = 0;

        for (int value : values) {
            stddev += Math.pow(value - avg, 2);
        }

        return Math.sqrt(stddev/values.size());
    }

    private Map<String, Integer> getRcStats(List<String> rcCodes) {
        Map<String, Integer> rcStats = new HashMap<>();

        for (String rc : rcCodes) {
            if (rcStats.containsKey(rc)) {
                rcStats.replace(rc, rcStats.get(rc) + 1);
            } else {
                rcStats.put(rc, 1);
            }
        }

        return rcStats;
    }

    public String getStatLabel() {
        return statLabel;
    }

    public void setStatLabel(String statLabel) {
        this.statLabel = statLabel;
    }

    public long getNumberOfRequestsSent() {
        return numberOfRequestsSent;
    }

    public void setNumberOfRequestsSent(long numberOfRequestsSent) {
        this.numberOfRequestsSent = numberOfRequestsSent;
    }

    public long getNumberOfBytesSent() {
        return numberOfBytesSent;
    }

    public void setNumberOfBytesSent(long numberOfBytesSent) {
        this.numberOfBytesSent = numberOfBytesSent;
    }

    public long getNumberOfSuccessfulRequests() {
        return numberOfSuccessfulRequests;
    }

    public void setNumberOfSuccessfulRequests(long numberOfSuccessfulRequests) {
        this.numberOfSuccessfulRequests = numberOfSuccessfulRequests;
    }

    public long getNumberOfFailedRequests() {
        return numberOfFailedRequests;
    }

    public void setNumberOfFailedRequests(long numberOfFailedRequests) {
        this.numberOfFailedRequests = numberOfFailedRequests;
    }

    public int getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(int durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public double getThroughputMBps() {
        return throughputMBps;
    }

    public void setThroughputMBps(double throughputMBps) {
        this.throughputMBps = throughputMBps;
    }

    public long getStartTimeTicks() {
        return startTimeTicks;
    }

    public void setStartTimeTicks(long startTimeTicks) {
        this.startTimeTicks = startTimeTicks;
    }

    public long getEndTiemTicks() {
        return endTimeTicks;
    }

    public void setEndTimeTicks(long endTimeTicks) {
        this.endTimeTicks = endTimeTicks;
    }
}
