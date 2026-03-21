package com.example.todo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiPlanner {

    private static final String TAG = "GeminiPlanner";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(String task, int offsetDays, List<String> reminders);
        void onError(String error);
    }

    public static void process(Context ctx, String rawText, Callback cb) {
        Log.d(TAG, ">>> START rawText: " + rawText);

        String apiKey = BuildConfig.GEMINI_API_KEY;
        Log.d(TAG, "apiKey: " + apiKey.substring(0, 8) + "...");

        String prompt =
                "Przeanalizuj polecenie i zwróć TYLKO poprawny JSON, bez markdown, " +
                        "bez komentarzy, bez żadnego tekstu poza JSON:\n" +
                        "{\n" +
                        "  \"task\": \"nazwa zadania po polsku\",\n" +
                        "  \"date_offset_days\": 0,\n" +
                        "  \"reminders\": [\"18:00\", \"19:00\"]\n" +
                        "}\n\n" +
                        "Zasady:\n" +
                        "- date_offset_days: 0=dziś, 1=jutro, 7=za tydzień itd.\n" +
                        "- reminders: lista godzin HH:mm, może być pusta []\n" +
                        "- task: krótka nazwa zadania, maks 60 znaków\n\n" +
                        "Polecenie: \"" + rawText + "\"";

        executor.execute(() -> {
            try {
                Log.d(TAG, "Wysyłam request do Gemini REST API v1beta...");

                URL url = new URL(API_URL + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject requestBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                requestBody.put("contents", contents);

                Log.d(TAG, "Request body: " + requestBody);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                BufferedReader reader;
                if (responseCode == 200) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseStr = sb.toString();
                Log.d(TAG, "Response raw: " + responseStr);

                if (responseCode != 200) {
                    mainHandler.post(() -> cb.onError("HTTP " + responseCode + ": " + responseStr));
                    return;
                }

                JSONObject response = new JSONObject(responseStr);
                String text = response
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                Log.d(TAG, "Odpowiedź text: " + text);

                String json = text.trim()
                        .replaceAll("```json", "")
                        .replaceAll("```", "")
                        .trim();

                Log.d(TAG, "JSON po oczyszczeniu: " + json);

                JSONObject obj = new JSONObject(json);
                String task = obj.getString("task");
                int days = obj.getInt("date_offset_days");
                JSONArray arr = obj.getJSONArray("reminders");
                List<String> reminders = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) reminders.add(arr.getString(i));

                Log.d(TAG, "Sparsowano OK — task: " + task + ", days: " + days + ", reminders: " + reminders);

                mainHandler.post(() -> cb.onResult(task, days, reminders));

            } catch (Exception e) {
                Log.e(TAG, "Błąd: " + e.getMessage(), e);
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }
}