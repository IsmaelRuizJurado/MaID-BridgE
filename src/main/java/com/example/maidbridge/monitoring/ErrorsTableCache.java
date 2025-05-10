package com.example.maidbridge.monitoring;

import com.example.maidbridge.elastic.ElasticConnector;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public static void computeDetailedErrorData() {
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
            JSONArray hits = new JSONObject(ElasticConnector.performSearch(queryJson))
                    .getJSONObject("hits").getJSONArray("hits");

            Map<String, List<JSONObject>> debugLogsByThread = new HashMap<>();
            List<JSONObject> errorLogs = new ArrayList<>();

            for (int i = 0; i < hits.length(); i++) {
                JSONObject source = hits.getJSONObject(i).getJSONObject("_source");
                String level = source.optString("level", "");
                String thread = source.optString("thread_name", "unknown");

                if ("DEBUG".equalsIgnoreCase(level)) {
                    debugLogsByThread.computeIfAbsent(thread, __ -> new ArrayList<>()).add(source);
                } else if ("ERROR".equalsIgnoreCase(level)) {
                    errorLogs.add(source);
                }
            }

            Map<String, Map<Integer, Map<String, ErrorsTablePanel.ErrorsTableEntry>>> grouped = new HashMap<>();

            for (JSONObject error : errorLogs) {
                String thread = error.optString("thread_name", "unknown");
                String timestampStr = error.optString("@timestamp", "");
                ZonedDateTime errorTime;
                try {
                    errorTime = ZonedDateTime.parse(timestampStr);
                } catch (Exception ignored) {
                    continue;
                }

                List<JSONObject> debugCandidates = debugLogsByThread.get(thread);
                if (debugCandidates == null || debugCandidates.isEmpty()) continue;

                JSONObject matchedDebug = null;
                for (int i = debugCandidates.size() - 1; i >= 0; i--) {
                    try {
                        JSONObject debug = debugCandidates.get(i);
                        ZonedDateTime debugTime = ZonedDateTime.parse(debug.optString("@timestamp", ""));
                        if (!debugTime.isAfter(errorTime)) {
                            matchedDebug = debug;
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (matchedDebug == null) continue;

                String stackTrace = error.optString("stack_trace", "");
                if (stackTrace.isBlank()) continue;

                String exceptionType = extractExceptionType(stackTrace);
                String fqcn = extractClassNameFromStackTrace(stackTrace);
                if (fqcn == null) continue;

                int line = extractLineNumberFromStackTrace(stackTrace, matchedDebug.optString("message", ""));
                if (line < 0) continue;

                // Extraer clase simple
                int lastDot = fqcn.lastIndexOf('.');
                String simpleClassName = (lastDot != -1) ? fqcn.substring(lastDot + 1) : fqcn;

                // Motivo del error desde el mensaje DEBUG
                String debugMsg = matchedDebug.optString("message", "").trim();
                String lineContent;
                int idx = debugMsg.lastIndexOf("Exception:");
                if (idx != -1 && idx + 10 < debugMsg.length()) {
                    lineContent = debugMsg.substring(idx + 10).trim();
                } else {
                    lineContent = debugMsg;
                }

                grouped
                        .computeIfAbsent(fqcn, __ -> new HashMap<>())
                        .computeIfAbsent(line, __ -> new HashMap<>())
                        .compute(exceptionType, (__, existing) -> {
                            if (existing == null) {
                                return new ErrorsTablePanel.ErrorsTableEntry(simpleClassName, exceptionType, line, lineContent, 1);
                            } else {
                                existing.occurrences++;
                                return existing;
                            }
                        });
            }

            grouped.values().forEach(perClass -> perClass.values().forEach(perLine -> result.addAll(perLine.values())));

        } catch (Exception e) {
            e.printStackTrace();
        }

        update(result);
        ErrorsTable.refreshData(result);
    }


}
