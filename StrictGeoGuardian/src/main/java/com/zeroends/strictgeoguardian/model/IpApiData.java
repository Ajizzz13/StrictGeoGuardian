package com.zeroends.strictgeoguardian.model;

import com.google.gson.annotations.SerializedName;

public record IpApiData(
    String status,
    String message,
    String country,
    String countryCode,
    String region,
    @SerializedName("regionName")
    String regionName,
    String city,
    String zip,
    double lat,
    double lon,
    String timezone,
    String isp,
    String org,
    @SerializedName("as")
    String asNumber,
    String query
) {
    public boolean isSuccess() {
        return "success".equals(status);
    }
}
