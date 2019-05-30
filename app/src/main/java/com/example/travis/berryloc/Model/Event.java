package com.example.travis.berryloc.Model;

public class Event {
    private String Name;
    private String Code;
    private Double Latitude;
    private Double Longitude;
    private String Type;

    public Event() {

    }

    public Event(String code, Double latitude, Double longitude, String type) {
        Code = code;
        Latitude = latitude;
        Longitude = longitude;
        Type = type;
    }

    public String getCode() {
        return Code;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public void setCode(String code) {
        Code = code;
    }

    public Double getLatitude() {
        return Latitude;
    }

    public void setLatitude(Double latitude) {
        Latitude = latitude;
    }

    public Double getLongitude() {
        return Longitude;
    }

    public void setLongitude(Double longitude) {
        Longitude = longitude;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }
}
