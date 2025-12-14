package com.zeroends.strictgeoguardian.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.model.FindIpData;
import com.zeroends.strictgeoguardian.model.GeoData;
import com.zeroends.strictgeoguardian.model.IpApiData;
import com.zeroends.strictgeoguardian.model.IpWhoData;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoService {

    private final StrictGeoGuardian plugin;
    private final String api1Url, api1Token, api2Url, api3Url;
    private final Gson gson = new Gson();
    private final Pattern asnPattern = Pattern.compile("^AS(\\d+).*");

    public GeoService(StrictGeoGuardian plugin, String api1Url, String api1Token, String api2Url, String api3Url) {
        this.plugin = plugin;
        this.api1Url = api1Url;
        this.api1Token = api1Token;
        this.api2Url = api2Url;
        this.api3Url = api3Url;
    }

    private boolean isLocal(String ip) {
        return ip.equals("127.0.0.1") || ip.equals("localhost") || ip.startsWith("192.168.");
    }

    public GeoData createLocalHostData() {
        return new GeoData.Builder("127.0.0.1")
                .success(true).type("IPv4").countryCode("LO").region("Local")
                .city("Localhost").asn("AS0").org("Local Network").isp("Local ISP")
                .build();
    }

    public CompletableFuture<GeoData> fetchRegistrationData(String ipAddress) {
        if (isLocal(ipAddress)) return CompletableFuture.completedFuture(createLocalHostData());

        return CompletableFuture.supplyAsync(() -> {
            GeoData data = fetchApi1(ipAddress).join();
            if (data != null) return data;
            
            plugin.getLogger().warning("Registration: API 1 failed. Trying API 2...");
            data = fetchApi2(ipAddress).join();
            if (data != null) return data;

            plugin.getLogger().warning("Registration: API 2 failed. Trying API 3...");
            return fetchApi3(ipAddress).join();
        });
    }
    
    public CompletableFuture<GeoData> fetchApi1(String ipAddress) {
        if (isLocal(ipAddress)) return CompletableFuture.completedFuture(createLocalHostData());
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format(api1Url, ipAddress) + "?token=" + api1Token;
                FindIpData data = httpGet(url, null, FindIpData.class);
                if (data != null && data.isSuccess()) {
                    return normalize(data, ipAddress);
                }
                plugin.getLogger().warning("API 1 (findip.net) failed for " + ipAddress + ". Error: " + (data != null ? data.getError() : "HTTP Error"));
                return null;
            } catch (Exception e) {
                plugin.getLogger().severe("Error fetching from API 1: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<GeoData> fetchApi2(String ipAddress) {
        if (isLocal(ipAddress)) return CompletableFuture.completedFuture(createLocalHostData());
        return CompletableFuture.supplyAsync(() -> {
            try {
                IpApiData data = httpGet(String.format(api2Url, ipAddress), null, IpApiData.class);
                if (data != null && data.isSuccess()) {
                    return normalize(data);
                }
                plugin.getLogger().warning("API 2 (ip-api.com) failed for " + ipAddress + ".");
                return null;
            } catch (Exception e) {
                plugin.getLogger().severe("Error fetching from API 2: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<GeoData> fetchApi3(String ipAddress) {
        if (isLocal(ipAddress)) return CompletableFuture.completedFuture(createLocalHostData());
        return CompletableFuture.supplyAsync(() -> {
            try {
                IpWhoData data = httpGet(String.format(api3Url, ipAddress), null, IpWhoData.class);
                if (data != null && data.success()) {
                    return normalize(data);
                }
                plugin.getLogger().warning("API 3 (ipwho.is) failed for " + ipAddress + ".");
                return null;
            } catch (Exception e) {
                plugin.getLogger().severe("Error fetching from API 3: " + e.getMessage());
                return null;
            }
        });
    }

    private <T> T httpGet(String urlString, String token, Class<T> classOfT) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "StrictGeoGuardian/1.0");
            if (token != null && !token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    return gson.fromJson(reader, classOfT);
                }
            } else {
                plugin.getLogger().warning("GeoAPI request failed for " + urlString + ". Response code: " + responseCode);
                return null;
            }
        } catch (JsonSyntaxException e) {
            plugin.getLogger().severe("Failed to parse GeoAPI JSON response from " + urlString + ": " + e.getMessage());
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String normalizeAsn(String input) {
        if (input == null || input.isEmpty()) return null;
        Matcher matcher = asnPattern.matcher(input);
        if (matcher.matches()) {
            return "AS" + matcher.group(1);
        }
        if (input.matches("^\\d+$")) {
            return "AS" + input;
        }
        return input;
    }

    private GeoData normalize(FindIpData data, String ip) {
        return new GeoData.Builder(ip)
                .success(true)
                .type(ip.contains(":") ? "IPv6" : "IPv4")
                .continentCode(data.getContinent() != null ? data.getContinent().getCode() : null)
                .countryCode(data.getCountry() != null ? data.getCountry().getIsoCode() : null)
                .region(data.getSubdivisions() != null && !data.getSubdivisions().isEmpty() ? data.getSubdivisions().get(0).getName() : null)
                .city(data.getCity() != null ? data.getCity().getName() : null)
                .latitude(data.getLocation() != null ? data.getLocation().getLatitude() : 0)
                .longitude(data.getLocation() != null ? data.getLocation().getLongitude() : 0)
                .asn(data.getTraits() != null ? normalizeAsn(data.getTraits().getAsn()) : null)
                .org(data.getTraits() != null ? data.getTraits().getOrg() : null)
                .isp(data.getTraits() != null ? data.getTraits().getIsp() : null)
                .timezone(data.getLocation() != null ? data.getLocation().getTimeZone() : null)
                .build();
    }
    
    private GeoData normalize(IpApiData data) {
        return new GeoData.Builder(data.query())
                .success(true)
                .type(data.query().contains(":") ? "IPv6" : "IPv4")
                .countryCode(data.countryCode())
                .region(data.regionName())
                .city(data.city())
                .latitude(data.lat())
                .longitude(data.lon())
                .postal(data.zip())
                .asn(normalizeAsn(data.asNumber()))
                .org(data.org())
                .isp(data.isp())
                .timezone(data.timezone())
                .build();
    }

    private GeoData normalize(IpWhoData data) {
        return new GeoData.Builder(data.ip())
                .success(true)
                .type(data.type())
                .continentCode(data.continentCode())
                .countryCode(data.countryCode())
                .region(data.region())
                .city(data.city())
                .latitude(data.latitude())
                .longitude(data.longitude())
                .postal(data.postal())
                .callingCode(data.callingCode())
                .asn(normalizeAsn(data.connection() != null ? String.valueOf(data.connection().asn()) : null))
                .org(data.connection() != null ? data.connection().org() : null)
                .isp(data.connection() != null ? data.connection().isp() : null)
                .domain(data.connection() != null ? data.connection().domain() : null)
                .timezone(data.timezone() != null ? data.timezone().id() : null)
                .build();
    }
}
