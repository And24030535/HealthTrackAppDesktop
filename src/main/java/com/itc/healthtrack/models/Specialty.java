package com.itc.healthtrack.models;

// especialidad medica del catalogo que administra el admin
// se guarda en la coleccion firestore specialties
public class Specialty {

    // id del documento en firestore tambien guardado como campo del documento
    private String id;
    private String name;
    private String description;

    public Specialty() {}

    public Specialty(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // se muestra en los combobox y celdas de la ui
    @Override
    public String toString() {
        return this.name;
    }
}
