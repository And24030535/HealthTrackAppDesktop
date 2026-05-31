package com.itc.healthtrack.models;

// especialidad medica que puede seleccionar un doctor
public class Specialty {

    private String id;
    private String name;
    private String description;

    public Specialty() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
