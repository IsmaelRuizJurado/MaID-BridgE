package com.example.maidbridge.elastic;

import com.example.maidbridge.settings.ElasticSettingsState;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;

public class ElasticConnector {

    private static RestClient client;

    public static RestClient getClient() {
        if (client == null) {
            ElasticSettingsState settings = ElasticSettingsState.getInstance();

            String host = settings.getHost();
            int port = settings.getPort();
            String scheme = settings.getScheme();
            String username = settings.getUsername();
            String password = settings.getPassword();

            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
            );

            RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme))
                    .setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                    );

            client = builder.build();
        }

        return client;
    }

    public static String performSearch(String queryJson) throws IOException {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        String indexName = settings.getIndex();

        Request request = new Request("GET", "/" + indexName + "/_search");
        request.setJsonEntity(queryJson);

        Response response = getClient().performRequest(request);
        return new String(response.getEntity().getContent().readAllBytes());
    }


    public static void close() {
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
