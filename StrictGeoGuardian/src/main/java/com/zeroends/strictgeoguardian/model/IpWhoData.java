package com.zeroends.strictgeoguardian.model;

import com.google.gson.annotations.SerializedName;

public record IpWhoData(
    String ip,
    boolean success,
    String type,
    String continent,
    @SerializedName("continent_code")
    String continentCode,
    String country,
    @SerializedName("country_code")
    String countryCode,
    String region,
    @SerializedName("region_code")
    String regionCode,
    String city,
    double latitude,
    double longitude,
    @SerializedName("is_eu")
    boolean isEu,
    String postal,
    @SerializedName("calling_code")
    String callingCode,
    String capital,
    Connection connection,
    Timezone timezone
) {
    public record Connection(
        int asn,
        String org,
        String isp,
        String domain
    ) {}

    public record Timezone(
        String id,
        String abbr,
        @SerializedName("is_dst")
        boolean isDst,
        int offset,
        String utc,
        @SerializedName("current_time")
        String currentTime
    ) {}
}
