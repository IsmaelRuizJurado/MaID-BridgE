package com.example.mpv_maidbridge;

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

    private static final String HOST = "localhost";
    private static final int PORT = 9200;
    private static final String SCHEME = "http";
    private static final String USERNAME = "elastic";
    private static final String PASSWORD = "aBewwyxIDlbHOF79YcpH";

    private static RestClient client;

    public static RestClient getClient() {
        if (client == null) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(USERNAME, PASSWORD)
            );

            RestClientBuilder builder = RestClient.builder(new HttpHost(HOST, PORT, SCHEME))
                    .setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                    );

            client = builder.build();
        }

        return client;
    }

    public static String performSearch(String indexName, String queryJson) throws IOException {
        Request request = new Request("GET", "/" + indexName + "/_search");
        request.setJsonEntity(queryJson);

        Response response = getClient().performRequest(request);
        return new String(response.getEntity().getContent().readAllBytes());
    }

    public static void close() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

