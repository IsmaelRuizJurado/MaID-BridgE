package com.example.maidbridge.elastic;

import com.example.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class ElasticConnector {

    private static RestClient client;

    public static RestClient getClient() throws MalformedURLException {
        if (client == null) {
            MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();

            String username = settings.getUsername();
            String password = settings.getPassword();

            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
            );

            String elasticURL = settings.getElasticsearchURL();
            URL url = new URL(elasticURL);

            String scheme = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();

            RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme))
                    .setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                    );

            client = builder.build();
        }

        return client;
    }

    public static String performSearch(String queryJson) throws IOException {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
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

    public static boolean emptyNotificationShown = false;

    public static void notifyEmptyResponse() {
        if (emptyNotificationShown) return;

        NotificationGroupManager.getInstance()
                .getNotificationGroup("MaID-BridgE Notification Group")
                .createNotification("No results were given by ElasticSearch, please check your configurations.", NotificationType.WARNING)
                .notify(null);

        emptyNotificationShown = true;
    }
}
