package com.example.travis.berryloc.Model;

public class Handshake {
    private String command;
    private String source;
    private String code;
    private BerryBody berryBody;

    public Handshake(String command, String source, String code, BerryBody berryBody) {
        this.command = command;
        this.source = source;
        this.code = code;
        this.berryBody = berryBody;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BerryBody getBerryBody() {
        return berryBody;
    }

    public void setBerryBody(BerryBody berryBody) {
        this.berryBody = berryBody;
    }
}
