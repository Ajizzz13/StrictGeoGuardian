package com.zeroends.strictgeoguardian.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class FindIpData {

    private City city;
    private Continent continent;
    private Country country;
    private Location location;
    private List<Subdivision> subdivisions;
    private Traits traits;
    @SerializedName("error")
    private String error;

    public boolean isSuccess() {
        return error == null;
    }

    public City getCity() { return city; }
    public Continent getContinent() { return continent; }
    public Country getCountry() { return country; }
    public Location getLocation() { return location; }
    public List<Subdivision> getSubdivisions() { return subdivisions; }
    public Traits getTraits() { return traits; }
    public String getError() { return error; }

    public static class City {
        @SerializedName("geoname_id")
        private int geonameId;
        private Map<String, String> names;
        
        public String getName() {
            return names != null ? names.get("en") : null;
        }
    }

    public static class Continent {
        private String code;
        @SerializedName("geoname_id")
        private int geonameId;
        private Map<String, String> names;
        
        public String getCode() {
            return code;
        }
        public String getName() {
            return names != null ? names.get("en") : null;
        }
    }

    public static class Country {
        @SerializedName("geoname_id")
        private int geonameId;
        @SerializedName("is_in_european_union")
        private boolean isInEuropeanUnion;
        @SerializedName("iso_code")
        private String isoCode;
        private Map<String, String> names;
        
        public String getIsoCode() {
            return isoCode;
        }
        public String getName() {
            return names != null ? names.get("en") : null;
        }
    }

    public static class Location {
        private double latitude;
        private double longitude;
        @SerializedName("time_zone")
        private String timeZone;
        
        public double getLatitude() {
            return latitude;
        }
        public double getLongitude() {
            return longitude;
        }
        public String getTimeZone() {
            return timeZone;
        }
    }

    public static class Subdivision {
        @SerializedName("geoname_id")
        private int geonameId;
        @SerializedName("iso_code")
        private String isoCode;
        private Map<String, String> names;
        
        public String getIsoCode() {
            return isoCode;
        }
        public String getName() {
            return names != null ? names.get("en") : null;
        }
    }

    public static class Traits {
        @SerializedName("autonomous_system_number")
        private int autonomousSystemNumber;
        @SerializedName("autonomous_system_organization")
        private String autonomousSystemOrganization;
        @SerializedName("connection_type")
        private String connectionType;
        private String isp;
        @SerializedName("user_type")
        private String userType;
        
        public String getAsn() {
            return "AS" + autonomousSystemNumber;
        }
        public String getOrg() {
            return autonomousSystemOrganization;
        }
        public String getIsp() {
            return isp;
        }
    }
}
