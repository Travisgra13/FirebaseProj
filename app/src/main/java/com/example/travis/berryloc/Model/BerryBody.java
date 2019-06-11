package com.example.travis.berryloc.Model;

public class BerryBody {
    private String guid;
    private String name;
    private String type;
    private String ip;
    private String port;

    public BerryBody(String guid, String name, String type, String ip, String port) {
        this.guid = guid;
        this.name = name;
        this.type = type;
        this.ip = ip;
        this.port = port;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
