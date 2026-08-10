package com.agileoracles.leave_portal_app;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LitedeskBucketDemo {

    private static final String CONFIG_FILE = "C:/Users/mahmo/Desktop/.oci/config";
    private static final String PROFILE = "DEFAULT";
    private static final String NAMESPACE = "idqag2xakgns";
    private static final String BUCKET_NAME = "litedesk-oci-bucket";

    public static void main(String[] args) throws Exception {

        ConfigFileAuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(CONFIG_FILE, PROFILE);

        try (ObjectStorageClient client = ObjectStorageClient.builder().build(provider)) {

            System.out.println("=== 1) Listing objects in " + BUCKET_NAME + " ===");
            listObjects(client);

            System.out.println("\n=== 2) Uploading a test file ===");
            String objectName = "local-test.txt";
            byte[] content = "Hello from the litedesk bucket demo".getBytes();
            uploadObject(client, objectName, content);

            System.out.println("\n=== Listing again to confirm the upload ===");
            listObjects(client);

            System.out.println("\n=== 3) Downloading the file back ===");
            downloadObject(client, objectName, "downloaded-local-test.txt");
        }
    }

    private static void listObjects(ObjectStorageClient client) {
        ListObjectsRequest request = ListObjectsRequest.builder()
                .namespaceName(NAMESPACE)
                .bucketName(BUCKET_NAME)
                .build();

        ListObjectsResponse response = client.listObjects(request);

        for (ObjectSummary summary : response.getListObjects().getObjects()) {
            System.out.println(" - " + summary.getName());
        }
    }

    private static void uploadObject(ObjectStorageClient client, String objectName, byte[] content) {
        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(NAMESPACE)
                .bucketName(BUCKET_NAME)
                .objectName(objectName)
                .putObjectBody(new ByteArrayInputStream(content))
                .contentLength((long) content.length)
                .build();

        client.putObject(request);
        System.out.println("Uploaded: " + objectName);
    }

    private static void downloadObject(ObjectStorageClient client, String objectName, String saveAsFileName) throws Exception {
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(NAMESPACE)
                .bucketName(BUCKET_NAME)
                .objectName(objectName)
                .build();

        GetObjectResponse response = client.getObject(request);

        try (InputStream in = response.getInputStream()) {
            Files.copy(in, Path.of(saveAsFileName), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Downloaded to: " + Path.of(saveAsFileName).toAbsolutePath());
    }
}