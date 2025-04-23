package com.mcp.weatherserver;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {
    private final RestClient restClient;

    public WeatherService(){
        this.restClient = RestClient.builder()
                            .baseUrl("https://api.weather.gov")
                            .defaultHeader("Accept", "application/geo+json")
                            .defaultHeader("User-Agent", "WeatherApiClient/1.0 (youremail@email.com)")
                            .build();
    }

    @Tool(description = "Get weather forecast for a specific latitude/longitude")
    public String getWeatherForecastByLocation(double latitude, double longitude){
        try {
            // step 1: Get metadata (includes forecast url)
            String metadataUrl = String.format("/points/%s,%s", latitude, longitude);
            Map<String, Object> metadataResponse = restClient.get()
                                                    .uri(metadataUrl)
                                                    .retrieve()
                                                    .body(Map.class);
            //Extract forecast URL from metadata
            Map<String, Object> properties = (Map<String, Object>) metadataResponse.get("properties");
            String forecastUrl = (String) properties.get("forecast");

            //step 2: call forecast url
            Map<String, Object> forecastResponse = restClient.get()
                                                    .uri(forecastUrl)
                                                    .retrieve()
                                                    .body(Map.class);
            
            // Extract periods array
            Map<String, Object> forecastProperties = (Map<String, Object>) forecastResponse.get("properties");
            List<Map<String, Object>> periods = (List<Map<String, Object>>) forecastProperties.get("periods");

            //step 3: Format and return forecast
            StringBuilder sb = new StringBuilder();
            for(Map<String, Object> period: periods)
            {
                sb.append("\n---\n");
                sb.append("Name: ").append(period.get("name")).append("\n");
                sb.append("Temperature: ").append(period.get("Temperature")).append("F\n");
                sb.append("Wind: ").append(period.get("windSpeed")).append(" ")
                  .append(period.get("windDirection")).append("\n");
                sb.append("Forecast: ").append(period.get("detailedForecast")).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error fetching forecast: " + e.getMessage();
        }
    }

    @Tool(description = "Get weather alerts for a US state")
    public String getAlerts(@ToolParam(description = "Two-letter US state code (e.g. CA, NY)") String state){
       try {
         //step 1: call the alerts API for the specified state
         String url = String.format("/alerts/active/area/%s", state.toUpperCase());
         Map<String, Object> response = restClient.get()
                                        .uri(url)
                                        .retrieve()
                                        .body(Map.class);
        
        // step 2: extract the features array
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) response.get("features");

        if(alerts == null || alerts.isEmpty()){
            return "No active alerts for this state";
        }

        //step 3: Parse and format each alert
        StringBuilder sb = new StringBuilder();
        for(Map<String, Object> alert: alerts)
        {
            Map<String, Object> properties = (Map<String, Object>) alert.get("properties");

            sb.append("\n---\n")
              .append("Event: ").append(properties.get("event")).append("\n")
              .append("Area: ").append(properties.get("areaDesc")).append("\n")
              .append("Severity: ").append(properties.get("severity")).append("\n")
              .append("Description: ").append(properties.get("description")).append("\n")
              .append("Instruction: ").append(properties.get("instruction")).append("\n");
        }
        return sb.toString();
       } catch (Exception e) {
            return "Failed to fetch alerts: "+ e.getMessage();
       }
    }
}
