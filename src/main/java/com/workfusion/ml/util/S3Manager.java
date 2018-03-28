package com.workfusion.ml.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

public class S3Manager {

    private static final String S3_ENDPOINT = "http://wfapp-s3.hamiltonusa.corp";
    private static final String ACCESS_KEY = "9TYDMJHX27FWJDSUQJUP";
    private static final String SECRET_KEY = "Lo9U0ynEC6WOqB0pPJY-ISdBjgqV5a-0Dgycog==";
    private static final String S3_SIGNER_TYPE = "S3SignerType";

    private AmazonS3 s3;

    private void init() {
        if (s3 == null) {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setSignerOverride(S3_SIGNER_TYPE);
            s3 = new AmazonS3Client(
                    new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY),
                    clientConfiguration);
            s3.setEndpoint(S3_ENDPOINT);
            S3ClientOptions s3ClientOptions = S3ClientOptions.builder().setPathStyleAccess(true).build();
            s3.setS3ClientOptions(s3ClientOptions);
        }
    }

    public String put(String bucket, String key, InputStream is, String contentType) throws IOException {
        init();
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(contentType);
        objectMetadata.setContentLength(is.available());
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, is, objectMetadata);
        putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        s3.putObject(putObjectRequest);
        return S3_ENDPOINT + "/" + bucket + "/" + key;
    }
}
