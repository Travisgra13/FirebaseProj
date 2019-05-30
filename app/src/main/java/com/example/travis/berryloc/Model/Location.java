package com.example.travis.berryloc.Model;

public class Location {
    private double Longitude;
    private double Latitude;

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public Location() {

    }


    public Location(double longitude, double latitude) {

        Longitude = longitude;
        Latitude = latitude;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o.getClass() != this.getClass()) {
            return false;
        }
        Location compareObject = (Location) o;
        if (((Location) o).getLatitude() != getLatitude()) {
            return false;
        }
        if (((Location) o).getLongitude() != getLatitude()) {
            return false;
        }
        return true;
    }
}
