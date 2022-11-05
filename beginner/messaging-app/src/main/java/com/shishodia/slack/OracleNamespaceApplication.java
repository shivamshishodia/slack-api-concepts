package com.shishodia.slack;

import java.io.IOException;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;

public class OracleNamespaceApplication {

    public static void main(String[] args) throws IOException {

        final String compartmentId = System.getenv("OCI_COMPARTMENT_OCID");

        final ConfigFileReader.ConfigFile configFile = ConfigFileReader.parseDefault();
        final AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

        ObjectStorageClient objectStorageClient = new ObjectStorageClient(provider);
        objectStorageClient.setRegion(Region.AP_MUMBAI_1);

        // Construct GetNamespaceRequest with the given compartmentId.
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder().compartmentId(compartmentId).build();
        String namespace = objectStorageClient.getNamespace(getNamespaceRequest).getValue();

        System.out.println(String.format("Object Storage namespace for compartment [%s] is [%s]", compartmentId, namespace));

        objectStorageClient.close();

    }
    
}
