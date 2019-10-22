package com.xlilium.perf.qa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.GsonBuilder;

public class JMeterResults {
    private List<JMeterSample> jMeterSamples = new ArrayList<>();
    private List<JMeterSampleBucket> jMeterSampleBuckets = new ArrayList<>();
    private List<JMeterResultStat> rampStats = new ArrayList<>();

    private int jMeterSampleCount = 0;
    private final int MAX_PRINT_COUNT = 0;
    private int global_print_count = 0;

    public void addSample(JMeterSample sample) {
        jMeterSampleCount++;
        updateBuckets(sample);
    }

    public void addStats(String label, int durationInSeconds) {
        //System.out.println("Adding stats for " + label);

        int statCount = 1;

        jMeterSampleBuckets.sort(Comparator.comparing(JMeterSampleBucket::getBucketId));
        List<JMeterSampleBucket> buckets = filterBucketsByLabel(jMeterSampleBuckets, label);

        //System.out.println("Found: " + buckets.size() + " buckets");

        JMeterResultStat jMeterResultStat = new JMeterResultStat(label + "-" + statCount, durationInSeconds);

        if (buckets.size() > 0) {
            long endRampTime = buckets.get(0).getBucketId() + durationInSeconds;

            for (JMeterSampleBucket bucket : buckets) {
                if (bucket.getLabel().equals(label)) {

                    if (bucket.getBucketId() >= endRampTime) {
                        rampStats.add(jMeterResultStat.updateStats());

                        endRampTime = bucket.getBucketId() + durationInSeconds;

                        jMeterResultStat = new JMeterResultStat(label + "-" + (++statCount), durationInSeconds);
                    }

                    jMeterResultStat.addBucket(bucket);
                }
            }
        }

        rampStats.add(jMeterResultStat.updateStats());
    }

    public void addStats(String label) {
        //System.out.println("Adding stats for " + label);

        int statCount = 1;

        jMeterSampleBuckets.sort(Comparator.comparing(JMeterSampleBucket::getBucketId));
        List<JMeterSampleBucket> buckets = filterBucketsByLabel(jMeterSampleBuckets, label);

        //System.out.println("Found: " + buckets.size() + " buckets");
        //JMeterResultStat jMeterResultStat = new JMeterResultStat(label + "-" + statCount, -1);

        JMeterResultStat jMeterResultStat = new JMeterResultStat(label + "-" + statCount, getStatDuration(buckets));

        jMeterResultStat.setStartTimeTicks(getStartTimeTick(buckets));

        for (JMeterSampleBucket bucket : buckets) {
            if (bucket.getLabel().equals(label)) {
                jMeterResultStat.addBucket(bucket);
            }
        }

        rampStats.add(jMeterResultStat.updateStats());
    }

    public int getSampleCount() {
        return jMeterSampleCount;
    }

    private int getStatDuration(List<JMeterSampleBucket> buckets) {
        int ret_val;

        // If bucket is empty, duration is -1
        // If bucket have only one sample, duration is latency of that sample
        // If bucket has multiple samples, duration is difference between time stamp of the samples
        if (buckets.size() > 1) {
            ret_val = (int) buckets.get(buckets.size() - 1).getBucketId() - (int) buckets.get(0).getBucketId();
        } else if (buckets.size() == 1 && buckets.get(0).getLatencies().size() > 0) {
            ret_val = buckets.get(0).getLatencies().get(0);
        } else {
            ret_val = -1;
        }

        return ret_val;
    }

    private Long getStartTimeTick(List<JMeterSampleBucket> buckets) {
        if (buckets.size() == 0) {
            return Long.MAX_VALUE;
        }

        return buckets.get(0).getBucketId();
    }

    private List<JMeterSampleBucket> filterBucketsByLabel(List<JMeterSampleBucket> sampleBuckets, String label) {
        List<JMeterSampleBucket> buckets = new ArrayList<>();

        for (JMeterSampleBucket bucket : sampleBuckets) {
            if (bucket.getLabel().equals(label)) {
                buckets.add(bucket);
            }
        }

        return buckets;
    }

    private void updateBuckets(JMeterSample sample) {
        boolean bucketFound = false;

        if (global_print_count < MAX_PRINT_COUNT) {
            System.out.println("Adding sample " + sample.getLabel_lb() + " : "
                    + sample.getLatency_lt() + " : "
                    + sample.isSuccess_s());
        }

        for (JMeterSampleBucket bucket : jMeterSampleBuckets) {
            if (bucket.getBucketId() == sample.getBucketId() && bucket.getLabel().equals(sample.getLabel_lb())) {
                bucket.setBucketSampleCount(bucket.getBucketSampleCount() + 1);
                bucket.setTotalBytesSent(bucket.getTotalBytesSent() + sample.getSentBytes_successful_sby());
                bucket.setTotalPayloadBytesSent(bucket.getTotalPayloadBytesSent() + sample.getPayloadSize());

                bucket.setBucketSuccessfulSampleCount(bucket.getBucketSuccessfulSampleCount() +
                        (sample.isSuccess_s() ? 1 : 0));
                bucket.setBucketFailedSampleCount(bucket.getBucketFailedSampleCount() +
                        (!sample.isSuccess_s() ? 1 : 0));

                bucket.addActiveThreads(sample.getActiveThreadCount_na());

                if (sample.isSuccess_s()) {
                    bucket.addLatency(sample.getLatency_lt());
                }

                bucket.addReturnCode(sample.getResponseCode_rc());

                bucketFound = true;
            }
        }

        if (!bucketFound) {
            JMeterSampleBucket jMeterSampleBucket = new JMeterSampleBucket();

            jMeterSampleBucket.setBucketId(sample.getBucketId());
            jMeterSampleBucket.setBucketTime(sample.getBucketTime());
            jMeterSampleBucket.setLabel(sample.getLabel_lb());
            jMeterSampleBucket.setBucketSampleCount(1);
            jMeterSampleBucket.setTotalBytesSent(sample.getSentBytes_successful_sby());
            jMeterSampleBucket.setTotalPayloadBytesSent(sample.getPayloadSize());

            jMeterSampleBucket.setBucketSuccessfulSampleCount((sample.isSuccess_s() ? 1 : 0));
            jMeterSampleBucket.setBucketFailedSampleCount((!sample.isSuccess_s() ? 1 : 0));

            jMeterSampleBucket.addActiveThreads(sample.getActiveThreadCount_na());

            if (sample.isSuccess_s()) {
                jMeterSampleBucket.addLatency(sample.getLatency_lt());
            }

            jMeterSampleBucket.addReturnCode(sample.getResponseCode_rc());

            jMeterSampleBuckets.add(jMeterSampleBucket);
        }

        global_print_count++;
    }

    public void printSummary() {
        //System.out.println("Got: " + jMeterSamples.size() + " samples.");

        /*
        for (JMeterSample sample : jMeterSamples) {
            System.out.print(sample.getLabel_lb() + " ");
            System.out.print(sample.getBucketId() + " - " + sample.getBucketTime());
            System.out.println(" : " + sample.getResponseCode_rc() + " - " + sample.getThreadName_tn());
        }
        */

        //System.out.println();

        /*
        jMeterSampleBuckets.sort(Comparator.comparing(JMeterSampleBucket::getBucketId));

        for (JMeterSampleBucket bucket : jMeterSampleBuckets) {
            System.out.print(bucket.getLabel() + ":");
            System.out.print(bucket.getBucketTime() + ":");
            System.out.print(bucket.getBucketId() + ":");
            System.out.print(bucket.getBucketSampleCount() + ":");
            System.out.print(bucket.getBucketFailedSampleCount() + ":");
            System.out.print(bucket.getTotalBytesSent());
            System.out.println();
        }
        */

        //for (JMeterResultStat stat : rampStats) {
        //    stat.printStat();
        //}

    }

    public void writeJsonReport(String fileName) {
        Path file = Paths.get(fileName);

        String jsonSummary = new GsonBuilder()
                .serializeSpecialFloatingPointValues()
                .setPrettyPrinting()
                .create()
                .toJson(rampStats);

        try {
            Files.write(file, jsonSummary.getBytes("utf8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Cannot encode report string");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Cannot create file " + fileName);
            e.printStackTrace();
        }
    }
}
