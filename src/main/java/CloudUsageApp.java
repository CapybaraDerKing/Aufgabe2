import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CloudUsageApp{

    private static final String GET_URL = "http://localhost:8080/v1/datasetLeistungsnachweis";
    private static final String POST_URL = "http://localhost:8080/v1/result";
    
    public static void main(String[] args) {
        try {
            // Step 1: Fetch dataset
            String jsonResponse = sendGetRequest(GET_URL);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode events = rootNode.get("events");

            // Step 2: Process events to calculate runtimes
            Map<String, Long> runtimes = new HashMap<>();
            Map<String, Long> workloadStartTimes = new HashMap<>();

            for (JsonNode event : events) {
                String customerId = event.get("customerId").asText();
                String workloadId = event.get("workloadId").asText();
                long timestamp = event.get("timestamp").asLong();
                String eventType = event.get("eventType").asText();

                if (eventType.equals("start")) {
                    workloadStartTimes.put(workloadId, timestamp);
                } else if (eventType.equals("stop") && workloadStartTimes.containsKey(workloadId)) {
                    long startTime = workloadStartTimes.remove(workloadId);
                    long runtime = timestamp - startTime;
                    runtimes.put(customerId, runtimes.getOrDefault(customerId, 0L) + runtime);
                }
            }

            // Step 3: Prepare result JSON
            String resultJson = createResultJson(runtimes);

            // Step 4: Post the result
            int postResponseCode = sendPostRequest(POST_URL, resultJson);
            System.out.println("Result posted with response code: " + postResponseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Send GET request to fetch the dataset
    private static String sendGetRequest(String getUrl) throws IOException {
        URL url = new URL(getUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } else {
            throw new RuntimeException("Failed to get data from API, response code: " + responseCode);
        }
    }

    // Send POST request with the result
    private static int sendPostRequest(String postUrl, String resultJson) throws IOException {
        URL url = new URL(postUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
    
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = resultJson.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed to post data to API, response code: " + responseCode);
        }
        
        return responseCode;
    }

    // Create JSON for the result
    private static String createResultJson(Map<String, Long> runtimes) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> result = new HashMap<>();
        result.put("result", runtimes.entrySet().stream().map(entry -> {
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("customerId", entry.getKey());
            customerData.put("consumption", entry.getValue());
            return customerData;
        }).toArray());

        return objectMapper.writeValueAsString(result);
    }
}
