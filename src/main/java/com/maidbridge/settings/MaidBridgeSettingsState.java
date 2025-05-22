package com.maidbridge.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;

@Service
@State(name = "MaidBridgeSettings", storages = @Storage("MaidBridgeSettings.xml"))
public final class MaidBridgeSettingsState implements PersistentStateComponent<MaidBridgeSettingsState.State> {

    private State state = new State();

    public static class State {
        public String elasticsearchURL = "http://localhost:9200/";
        public String username = "";
        public String password = "";
        public String index = "";
        public String kibanaURL = "http://localhost:5601/";

        public String errorTimeRange = "24h"; // "24h", "7d", "30d", "custom"
        public String logTimeRange = "24h";

        public String errorCustomTime = ZonedDateTime.now().minusHours(24).toString();
        public String logCustomTime = ZonedDateTime.now().minusHours(24).toString();
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

    public String getErrorTimeRange() {
        return state.errorTimeRange;
    }

    public void setErrorTimeRange(String value) {
        state.errorTimeRange = value;
    }

    public String getLogTimeRange() {
        return state.logTimeRange;
    }

    public void setLogTimeRange(String value) {
        state.logTimeRange = value;
    }

    public ZonedDateTime getErrorCustomTime() {
        try {
            return ZonedDateTime.parse(state.errorCustomTime);
        } catch (Exception e) {
            return ZonedDateTime.now().minusHours(24);
        }
    }

    public void setErrorCustomTime(ZonedDateTime zdt) {
        state.errorCustomTime = zdt.toString();
    }

    public ZonedDateTime getLogCustomTime() {
        try {
            return ZonedDateTime.parse(state.logCustomTime);
        } catch (Exception e) {
            return ZonedDateTime.now().minusHours(24);
        }
    }

    public void setLogCustomTime(ZonedDateTime zdt) {
        state.logCustomTime = zdt.toString();
    }
}
