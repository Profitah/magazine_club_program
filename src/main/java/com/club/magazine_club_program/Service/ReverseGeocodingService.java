package com.club.magazine_club_program.Service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ReverseGeocodingService {

    private final RestTemplate restTemplate;

    public ReverseGeocodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String reverseGeocodeCountryRegion(double latitude, double longitude) {
        try {
            String url = String.format("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%f&lon=%f", latitude, longitude);
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "magazine-club-program/1.0 (contact: example@example.com)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> address = (Map<String, Object>) response.getBody().get("address");
            if (address == null) return null;

            String country = (String) address.getOrDefault("country", "");
            String state = (String) address.getOrDefault("state", "");
            String region = (String) address.getOrDefault("region", "");
            String province = (String) address.getOrDefault("province", "");

            String area = firstNonEmpty(state, region, province);
            if (isEmpty(country) && isEmpty(area)) return null;
            if (isEmpty(area)) return country;
            if (isEmpty(country)) return area;
            return country + " " + area;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) {
            if (!isEmpty(v)) return v;
        }
        return null;
    }
}


