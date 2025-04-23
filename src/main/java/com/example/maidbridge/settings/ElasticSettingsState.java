package com.example.maidbridge.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service
@State(name = "ElasticSettings", storages = @Storage("ElasticSettings.xml"))
public final class ElasticSettingsState implements PersistentStateComponent<ElasticSettingsState.State> {

    private State state = new State();

    public static class State {
        public String host = "localhost";
        public int port = 9200;
        public String scheme = "http";
        public String username = "elastic";
        public String password = "aBewwyxIDlbHOF79YcpH";
    }

    public static ElasticSettingsState getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(ElasticSettingsState.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    // Métodos de acceso para ElasticConnector

    public String getHost() {
        return state.host;
    }

    public void setHost(String host) {
        state.host = host;
    }

    public int getPort() {
        return state.port;
    }

    public void setPort(int port) {
        state.port = port;
    }

    public String getScheme() {
        return state.scheme;
    }

    public void setScheme(String scheme) {
        state.scheme = scheme;
    }

    public String getUsername() {
        return state.username;
    }

    public void setUsername(String username) {
        state.username = username;
    }

    public String getPassword() {
        return state.password;
    }

    public void setPassword(String password) {
        state.password = password;
    }
}
