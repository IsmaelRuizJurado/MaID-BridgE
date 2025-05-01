package com.example.maidbridge.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service
@State(name = "MaidBridgeSettings", storages = @Storage("MaidBridgeSettings.xml"))
public final class MaidBridgeSettingsState implements PersistentStateComponent<MaidBridgeSettingsState.State> {

    private State state = new State();

    public static class State {
        public String elasticsearchURL = "http://localhost:9200/";
        public String username = "elastic";
        public String password = "aBewwyxIDlbHOF79YcpH";
        public String index = "spring-petclinic-logs";
        public String kibanaURL = "http://localhost:5601/";
        public int refreshInterval = 15;
    }

    public static MaidBridgeSettingsState getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(MaidBridgeSettingsState.class);
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


    public String getElasticsearchURL() {
        return state.elasticsearchURL;
    }

    public void setElasticsearchURL(String elasticsearchURL) {
        state.elasticsearchURL = elasticsearchURL;
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

    public String getIndex() {
        return state.index;
    }

    public void setIndex(String index) {
        state.index = index;
    }

    public String getKibanaURL() {
        return state.kibanaURL;
    }

    public void setKibanaURL(String kibanaURL) {
        state.kibanaURL = kibanaURL;
    }

    public int getRefreshInterval() {
        return state.refreshInterval;
    }

    public void setRefreshInterval(int interval) {
        state.refreshInterval = interval;
    }

}
