package com.example.mock;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;

import static okhttp3.TlsVersion.TLS_1_0;

public class OpenShiftMockServer extends KubernetesMockServer {

    public OpenShiftMockServer() {
        super();
    }

    public OpenShiftMockServer(boolean useHttps) {
        super(useHttps);
    }

    @Override
    public String[] getRootPaths() {
        return new String[]{"/api","/oapi"};
    }

    public NamespacedOpenShiftClient createOpenShiftClient() {
        Config config = new ConfigBuilder()
                .withMasterUrl(url("/"))
                .withNamespace("test")
                .withTrustCerts(true)
                .withTlsVersions(TLS_1_0)
                .build();
        return new DefaultOpenShiftClient(config);
    }
}
