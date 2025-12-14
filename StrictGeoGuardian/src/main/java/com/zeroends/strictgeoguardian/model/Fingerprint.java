package com.zeroends.strictgeoguardian.model;

import java.time.Instant;
import java.util.UUID;

public record Fingerprint(
    long id,
    String playerName,
    Instant createdAt,
    UUID javaUuid,
    String edition,
    String profilePropertyDigest,
    String ipVersion,
    String hashedPrefix,
    String hashedPseudoAsn,
    String hashedPtr,
    int tcpTtl,
    int tcpMss,
    String clientBrand,
    String modListHash,
    String resourcePackHash,
    String viewportSettings,
    String locale,
    String skinParts,
    String countryCode,
    String continentCode,
    String region,
    String city,
    double latitude,
    double longitude,
    String timezone,
    String postal,
    String calling_code,
    String asn,
    String org,
    String isp,
    String domain
) {
    public static class Builder {
        private long id = -1;
        private String playerName;
        private Instant createdAt = Instant.now();
        private UUID javaUuid;
        private String edition = "JAVA";
        private String profilePropertyDigest;
        private String ipVersion;
        private String hashedPrefix;
        private String hashedPseudoAsn;
        private String hashedPtr;
        private int tcpTtl;
        private int tcpMss;
        private String clientBrand;
        private String modListHash;
        private String resourcePackHash;
        private String viewportSettings;
        private String locale;
        private String skinParts;
        private String countryCode;
        private String continentCode;
        private String region;
        private String city;
        private double latitude;
        private double longitude;
        private String timezone;
        private String postal;
        private String calling_code;
        private String asn;
        private String org;
        private String isp;
        private String domain;

        public Builder(String playerName, UUID javaUuid) {
            this.playerName = playerName;
            this.javaUuid = javaUuid;
        }

        public Builder id(long id) { this.id = id; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder edition(String edition) { this.edition = edition; return this; }
        public Builder profilePropertyDigest(String profilePropertyDigest) { this.profilePropertyDigest = profilePropertyDigest; return this; }
        public Builder ipVersion(String ipVersion) { this.ipVersion = ipVersion; return this; }
        public Builder hashedPrefix(String hashedPrefix) { this.hashedPrefix = hashedPrefix; return this; }
        public Builder hashedPseudoAsn(String hashedPseudoAsn) { this.hashedPseudoAsn = hashedPseudoAsn; return this; }
        public Builder hashedPtr(String hashedPtr) { this.hashedPtr = hashedPtr; return this; }
        public Builder tcpTtl(int tcpTtl) { this.tcpTtl = tcpTtl; return this; }
        public Builder tcpMss(int tcpMss) { this.tcpMss = tcpMss; return this; }
        public Builder clientBrand(String clientBrand) { this.clientBrand = clientBrand; return this; }
        public Builder modListHash(String modListHash) { this.modListHash = modListHash; return this; }
        public Builder resourcePackHash(String resourcePackHash) { this.resourcePackHash = resourcePackHash; return this; }
        public Builder viewportSettings(String viewportSettings) { this.viewportSettings = viewportSettings; return this; }
        public Builder locale(String locale) { this.locale = locale; return this; }
        public Builder skinParts(String skinParts) { this.skinParts = skinParts; return this; }
        
        public Builder geoData(GeoData geo) {
            if (geo == null) return this;
            this.countryCode = geo.getCountryCode();
            this.continentCode = geo.getContinentCode();
            this.region = geo.getRegion();
            this.city = geo.getCity();
            this.latitude = geo.getLatitude();
            this.longitude = geo.getLongitude();
            this.timezone = geo.getTimezone();
            this.postal = geo.getPostal();
            this.calling_code = geo.getCallingCode();
            this.asn = geo.getAsn();
            this.org = geo.getOrg();
            this.isp = geo.getIsp();
            this.domain = geo.getDomain();
            this.ipVersion = geo.getType();
            return this;
        }

        public Fingerprint build() {
            return new Fingerprint(
                id, playerName, createdAt, javaUuid, edition, profilePropertyDigest,
                ipVersion, hashedPrefix, hashedPseudoAsn, hashedPtr, tcpTtl, tcpMss,
                clientBrand, modListHash, resourcePackHash, viewportSettings, locale, skinParts,
                countryCode, continentCode, region, city, latitude, longitude, timezone,
                postal, calling_code, asn, org, isp, domain
            );
        }
    }
}
