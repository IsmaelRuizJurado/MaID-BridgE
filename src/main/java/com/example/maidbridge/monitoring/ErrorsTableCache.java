package com.example.maidbridge.monitoring;

import com.example.maidbridge.elastic.ElasticConnector;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

import static com.example.maidbridge.monitoring.Auxiliaries.*;

public class ErrorsTableCache {
    private static final List<ErrorsTablePanel.ErrorsTableEntry> cache = new ArrayList<>();

    public static void update(List<ErrorsTablePanel.ErrorsTableEntry> newData) {
        synchronized (cache) {
            cache.clear();
            cache.addAll(newData);
        }
    }

    public static List<ErrorsTablePanel.ErrorsTableEntry> getAll() {
        synchronized (cache) {
            return new ArrayList<>(cache);
        }
    }

    public static List<ErrorsTablePanel.ErrorsTableEntry> computeDetailedErrorData() {
        List<ErrorsTablePanel.ErrorsTableEntry> result = new ArrayList<>();

        String queryJson = """
    {
      "size": 10000,
      "_source": ["message", "level", "@timestamp", "stack_trace", "thread_name"],
      "query": {
        "bool": {
          "should": [
            { "match": { "level": "ERROR" } },
            { "match": { "level": "DEBUG" } }
          ]
        }
      },
      "sort": [{ "@timestamp": "asc" }]
    }
    """;

        try {
            String responseJson = ElasticConnector.performSearch(queryJson);
            JSONArray hits = new JSONObject(responseJson).getJSONObject("hits").getJSONArray("hits");

            Map<String, List<JSONObject>> debugLogsByThread = new HashMap<>();
            List<JSONObject> errorLogs = new ArrayList<>();

            for (int i = 0; i < hits.length(); i++) {
                JSONObject log = hits.getJSONObject(i).getJSONObject("_source");
                String level = log.optString("level", "");
                String thread = log.optString("thread_name", "unknown");

                if (level.equalsIgnoreCase("DEBUG")) {
                    debugLogsByThread.computeIfAbsent(thread, k -> new ArrayList<>()).add(log);
                } else if (level.equalsIgnoreCase("ERROR")) {
                    errorLogs.add(log);
                }
            }

            Map<String, Map<Integer, Map<String, ErrorsTablePanel.ErrorsTableEntry>>> grouped = new HashMap<>();

            for (JSONObject error : errorLogs) {
                String thread = error.optString("thread_name", "unknown");
                String errorTimestamp = error.optString("@timestamp", "");
                ZonedDateTime errorTime;
                try {
                    errorTime = ZonedDateTime.parse(errorTimestamp);
                } catch (Exception e) {
                    continue;
                }

                List<JSONObject> candidates = debugLogsByThread.getOrDefault(thread, List.of());

                JSONObject matchedDebug = null;
                for (int i = candidates.size() - 1; i >= 0; i--) {
                    JSONObject debug = candidates.get(i);
                    try {
                        ZonedDateTime debugTime = ZonedDateTime.parse(debug.optString("@timestamp", ""));
                        if (!debugTime.isAfter(errorTime)) {
                            matchedDebug = debug;
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (matchedDebug == null) continue;

                String stackTrace = error.optString("stack_trace", "");
                String exceptionType = extractExceptionType(stackTrace);
                String fqcn = extractClassNameFromStackTrace(stackTrace);
                int line = extractLineNumberFromStackTrace(stackTrace, matchedDebug.optString("message", ""));

                if (fqcn == null || line < 0) continue;

                String simpleClassName = fqcn.substring(fqcn.lastIndexOf('.') + 1);

                // Extraer motivo del error desde el mensaje de debug
                String debugMessage = matchedDebug.optString("message", "");
                String lineContent;
                int lastColon = debugMessage.lastIndexOf("Exception:");
                if (lastColon != -1 && lastColon + 10 < debugMessage.length()) {
                    lineContent = debugMessage.substring(lastColon + 10).trim();
                } else {
                    lineContent = debugMessage.trim();
                }

                grouped
                        .computeIfAbsent(fqcn, k -> new HashMap<>())
                        .computeIfAbsent(line, k -> new HashMap<>())
                        .compute(exceptionType, (k, v) -> {
                            if (v == null) {
                                return new ErrorsTablePanel.ErrorsTableEntry(simpleClassName, exceptionType, line, lineContent, 1);
                            } else {
                                v.occurrences++;
                                return v;
                            }
                        });
            }

            for (Map<Integer, Map<String, ErrorsTablePanel.ErrorsTableEntry>> perClass : grouped.values()) {
                for (Map<String, ErrorsTablePanel.ErrorsTableEntry> perLine : perClass.values()) {
                    result.addAll(perLine.values());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        update(result);
        ErrorsTable.refreshData(result);

        return result;
    }

}
