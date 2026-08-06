package com.agileoracles.leave_portal_app.service;

import com.agileoracles.leave_portal_app.dto.OciUploadResult;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
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

    private ObjectStorageClient client;

    private synchronized ObjectStorageClient getClient() throws Exception {
        if (client == null) {
            ConfigFileAuthenticationDetailsProvider provider =
                    new ConfigFileAuthenticationDetailsProvider(configFile, profile);
            client = ObjectStorageClient.builder().build(provider);
        }
        return client;
    }

    public OciUploadResult uploadFile(String originalFileName, byte[] fileBytes) throws Exception {

        String objectName = UUID.randomUUID() + "-" + originalFileName;

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
}