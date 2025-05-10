package com.example.maidbridge.monitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.example.maidbridge.elastic.ElasticConnector;
import com.intellij.psi.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import static com.example.maidbridge.monitoring.Auxiliaries.*;

public class TotalErrorsTableCache {
    private static final Map<String, Integer> errorCountByClass = new ConcurrentHashMap<>();

    public static void update(Map<String, Integer> newData) {
        errorCountByClass.clear();
        errorCountByClass.putAll(newData);
    }

    public static Map<String, Integer> getAll() {
        return new HashMap<>(errorCountByClass);
    }

    public static Map<String, Integer> countErrorOccurrencesByClass(PsiFile file) {
        Map<String, Integer> result = new HashMap<>();
        String classQualifiedName = getQualifiedClassName(file);
        if (classQualifiedName == null) return result;

        String queryJson = """
        {
          "size": 10000,
          "_source": ["stack_trace"],
          "query": {
            "bool": {
              "should": [
                { "match": { "level": "ERROR" } }
              ]
            }
          }
        }
        """;

        try {
            String responseJson = ElasticConnector.performSearch(queryJson);
            JSONArray hits = new JSONObject(responseJson).getJSONObject("hits").getJSONArray("hits");

            int totalErrors = 0;

            for (int i = 0; i < hits.length(); i++) {
                JSONObject error = hits.getJSONObject(i).getJSONObject("_source");
                String stackTrace = error.optString("stack_trace", "");

                String fqcn = extractClassNameFromStackTrace(stackTrace);

                if (fqcn != null && fqcn.equals(classQualifiedName)) {
                    totalErrors++;
                }
            }

            if (totalErrors > 0) {
                result.put(classQualifiedName, totalErrors);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

}

