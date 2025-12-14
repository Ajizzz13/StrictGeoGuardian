package com.zeroends.strictgeoguardian.model;

public class GeoData {
    private String ip;
    private boolean success;
    private String type;
    private String continentCode;
    private String countryCode;
    private String region;
    private String city;
    private double latitude;
    private double longitude;
    private String postal;
    private String callingCode;
    private String asn;
    private String org;
    private String isp;
    private String domain;
    private String timezone;

    private GeoData() {}

    public String getIp() { return ip; }
    public boolean isSuccess() { return success; }
    public String getType() { return type; }
    public String getContinentCode() { return continentCode; }
    public String getCountryCode() { return countryCode; }
    public String getRegion() { return region; }
    public String getCity() { return city; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getPostal() { return postal; }
    public String getCallingCode() { return callingCode; }
    public String getAsn() { return asn; }
    public String getOrg() { return org; }
    public String getIsp() { return isp; }
    public String getDomain() { return domain; }
    public String getTimezone() { return timezone; }

    public static class Builder {
        private String ip;
        private boolean success = false;
        private String type;
        private String continentCode;
        private String countryCode;
        private String region;
        private String city;
        private double latitude;
        private double longitude;
        private String postal;
        private String callingCode;
        private String asn;
        private String org;
        private String isp;
        private String domain;
        private String timezone;

        public Builder(String ip) {
            this.ip = ip;
        }

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder continentCode(String continentCode) { this.continentCode = continentCode; return this; }
        public Builder countryCode(String countryCode) { this.countryCode = countryCode; return this; }
        public Builder region(String region) { this.region = region; return this; }
        public Builder city(String city) { this.city = city; return this; }
        public Builder latitude(double latitude) { this.latitude = latitude; return this; }
        public Builder longitude(double longitude) { this.longitude = longitude; return this; }
        public Builder postal(String postal) { this.postal = postal; return this; }
        public Builder callingCode(String callingCode) { this.callingCode = callingCode; return this; }
        public Builder asn(String asn) { this.asn = asn; return this; }
        public Builder org(String org) { this.org = org; return this; }
        public Builder isp(String isp) { this.isp = isp; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder timezone(String timezone) { this.timezone = timezone; return this; }

        public GeoData build() {
            GeoData data = new GeoData();
            data.ip = this.ip;
            data.success = this.success;
            data.type = this.type;
            data.continentCode = this.continentCode;
            data.countryCode = this.countryCode;
            data.region = this.region;
            data.city = this.city;
            data.latitude = this.latitude;
            data.longitude = this.longitude;
            data.postal = this.postal;
            data.callingCode = this.callingCode;
            data.asn = this.asn;
            data.org = this.org;
            data.isp = this.isp;
            data.domain = this.domain;
            data.timezone = this.timezone;
            return data;
        }
    }
}
