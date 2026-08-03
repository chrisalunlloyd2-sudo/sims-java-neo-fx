package com.aigen.sims;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaRouter {
    public String query(String model, String prompt) {
        System.out.println("[OLLAMA] Routing to " + model + " | Prompt: " + prompt);
        try {
            URL url = new URL("http://localhost:11434/api/generate");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(15000); // 15s timeout
            con.setReadTimeout(45000);    // 45s read timeout
            
            // Escape quotes
            String safePrompt = prompt.replace("\"", "\\\"").replace("\n", " ");
            String json = "{\"model\":\"" + model + "\", \"prompt\":\"" + safePrompt + "\", \"stream\":false}";
            
            OutputStream os = con.getOutputStream();
            os.write(json.getBytes("UTF-8"));
            os.flush();
            os.close();
            
            if (con.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while((line = in.readLine()) != null) response.append(line);
                in.close();
                
                String resStr = response.toString();
                // Crude JSON extract for "response":"..."
                int idx = resStr.indexOf("\"response\":\"");
                if (idx != -1) {
                    int start = idx + 12;
                    // Find the unescaped closing quote
                    int end = start;
                    while (end < resStr.length()) {
                        if (resStr.charAt(end) == '"' && resStr.charAt(end - 1) != '\\') {
                            break;
                        }
                        end++;
                    }
                    if (end <= resStr.length()) {
                        String actualText = resStr.substring(start, end).replace("\\n", " ").replace("\\\"", "\"").trim();
                        System.out.println(" -> Response from " + model + " acquired.");
                        return actualText;
                    }
                }
                return resStr;
            } else {
                System.out.println("[OLLAMA ERROR] HTTP " + con.getResponseCode());
            }
        } catch(Exception e) {
            System.out.println("[OLLAMA ERROR] Timeout or offline: " + model + " - " + e.getMessage());
        }
        return "NO_RESPONSE";
    }
}
