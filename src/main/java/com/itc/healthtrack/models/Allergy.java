package com.itc.healthtrack.models;

// alergia registrada en el catalogo general
public class Allergy {

    private String id;
    private String name;
    // mild / moderate / severe
    private String severity;
    // true si la alergia sigue activa en el catalogo
    private Boolean active;

    public Allergy() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
