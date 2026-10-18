package com.college.speaker.domain;

public enum Track {

    DEVOPS("DevOps"),
    CLOUD("Cloud"),
    AI_ML("AI / ML"),
    SECURITY("Security"),
    WEB("Web"),
    OTHER("Other");

    private final String label;

    Track(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
