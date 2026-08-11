package com.agileoracles.leave_portal_app.service;

import com.agileoracles.leave_portal_app.dto.OciUploadResult;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class OciStorageService {

    @Value("${oci.namespace}")
    private String namespace;

    @Value("${oci.bucket-name}")
    private String bucketName;

    @Value("${oci.config-file}")
    private String configFile;

    @Value("${oci.profile}")
    private String profile;

    @Value("${oci.object-prefix:eta-maryam}")
    private String objectPrefix;

    private ObjectStorageClient client;

    private synchronized ObjectStorageClient getClient() throws Exception {
        if (client == null) {
            ConfigFileAuthenticationDetailsProvider provider =
                    new ConfigFileAuthenticationDetailsProvider(configFile, profile);
            client = ObjectStorageClient.builder().build(provider);
        }
        return client;
    }

    public String getBucketName() {
        return bucketName;
    }

    public OciUploadResult uploadFile(String originalFileName, byte[] fileBytes) throws Exception {

        String objectName = objectPrefix + "/" + UUID.randomUUID() + "-" + originalFileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .putObjectBody(new ByteArrayInputStream(fileBytes))
                .contentLength((long) fileBytes.length)
                .build();

        PutObjectResponse response = getClient().putObject(request);

        return new OciUploadResult(objectName, response.getETag());
    }

    public List<String> listFiles() throws Exception {

        ListObjectsRequest request = ListObjectsRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .build();

        ListObjectsResponse response = getClient().listObjects(request);

        return response.getListObjects().getObjects().stream()
                .map(ObjectSummary::getName)
                .toList();
    }

    public byte[] downloadFile(String objectName) throws Exception {
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .build();

        GetObjectResponse response = getClient().getObject(request);

        try (InputStream in = response.getInputStream()) {
            return in.readAllBytes();
        }
    }
}