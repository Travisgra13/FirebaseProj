package com.example.travis.berryloc;

import java.util.ArrayList;
import java.util.Scanner;

public class AttributeParser {
    private String file;
    private ArrayList <String> attributes;

    //FIXME if def is used in a comment it will incorrectly believe the next word is a func

    public AttributeParser(String file) {
        this.file = file;
        this.attributes = new ArrayList<>();
    }

    public ArrayList<String> parse() {
        if (file == null) {
            return null;
        }
        //StringBuilder sb = new StringBuilder(file);
        Scanner scanner = new Scanner(file);
        while (scanner.hasNext()) {
            String nextWord = scanner.next();
            if (nextWord.equals("def")) {
                attributes.add(ignoreFuncParams(scanner.next()));
            }
        }
        return attributes;
    }

    private String ignoreFuncParams(String string) {
        StringBuilder sb = new StringBuilder(string);
        int startChopIndex = sb.indexOf("(");
        int endChopIndex = sb.length();
        String deliminatedString = sb.delete(startChopIndex, endChopIndex).toString();
        return deliminatedString;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public ArrayList<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<String> attributes) {
        this.attributes = attributes;
    }
}
