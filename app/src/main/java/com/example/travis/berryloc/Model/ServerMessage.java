package com.example.travis.berryloc.Model;

import java.util.Dictionary;
import java.util.Hashtable;

public class ServerMessage {
    private String Command;
    private Dictionary payload;

    public ServerMessage() {
        this.Command = "code-save";
        payload = new Hashtable();
    }

    public String getCommand() {
        return Command;
    }

    public void setCommand(String command) {
        Command = command;
    }


    public void readyPayload(String name, String code) {
        payload.put("name", name);
        payload.put("code", code);
    }

    public Dictionary getPayload() {
        return payload;
    }

    public void setPayload(Dictionary payload) {
        this.payload = payload;
    }
}
