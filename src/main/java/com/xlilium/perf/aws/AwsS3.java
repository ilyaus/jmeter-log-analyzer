package com.xlilium.perf.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


import static java.lang.System.exit;

public class AwsS3 {
    private static final Logger LOG = LoggerFactory.getLogger(AwsS3.class);

    private String s3Name;
    final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

    public AwsS3(String s3Name) {
        this.s3Name = s3Name;
    }

    public List<String> downloadLogFiles(String ext, String keyPrefix, String destination) {
        List<String> fileList = new ArrayList<>();

        ListObjectsV2Result result = s3.listObjectsV2(s3Name, keyPrefix);

        LOG.info("ListObjectsV2Results count: " + result.getKeyCount());

        List<S3ObjectSummary> objects = result.getObjectSummaries();

        LOG.info("Downloading files from: " + s3Name + " Prefix: " + keyPrefix + " Ext: " + ext);

        for (S3ObjectSummary os : objects) {

            LOG.debug("S3 Key: {}", os.getKey());

            if (os.getKey().endsWith(ext) && os.getKey().startsWith(keyPrefix + "/")) {

                String destFileName = destination + File.separator +
                        os.getKey().substring(os.getKey().lastIndexOf("/") + 1, os.getKey().length());

                try {
                    LOG.info("Downloading {} ({})", os.getKey(), os.getSize());

                    S3Object o = s3.getObject(os.getBucketName(), os.getKey());
                    S3ObjectInputStream s3is = o.getObjectContent();

                    FileOutputStream fos = new FileOutputStream(new File(destFileName));

                    byte[] read_buf = new byte[1024];

                    int read_len;

                    while ((read_len = s3is.read(read_buf)) > 0) {
                        fos.write(read_buf, 0, read_len);
                    }

                    fileList.add(destFileName);

                    s3is.close();
                    fos.close();
                } catch (AmazonServiceException e) {
                    LOG.error("AmazonServiceException {}", e.getMessage());
                    exit(1);
                } catch (FileNotFoundException e) {
                    LOG.error("FileNotFoundException {}", e.getMessage());
                    exit(1);
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                    exit(1);
                }
            }
        }

        return fileList;
    }

    public void upload(String key, List<String>fileList) {
        for (String file : fileList) {
            upload(key, file);
        }
    }

    public void upload(String key, String file) {
        LOG.info("Uploading to {}: {}", key, file);

        try {
            s3.putObject(s3Name, key + "/" + Paths.get(file).getFileName().toString(), new File(file));
        } catch (AmazonServiceException e) {
            LOG.error("Failed to upload file to S3 ({})", e.getMessage());
            System.exit(1);
        }
    }

    public void delete(String key, List<String>fileList) {
        for (String file : fileList) {
            delete(key, file);
        }
    }

    public void delete(String key, String file) {
        LOG.info("Deleting from {}: {}", key, file);

        try {
            s3.deleteObject(s3Name, key + "/" + file);
        } catch (AmazonServiceException e) {
            LOG.error("Failed to upload file to S3 ({})", e.getMessage());
            System.exit(1);
        }
    }
}
