package com.itc.healthtrack.models;

// FEATURE 4 — Alergia de un paciente
// se guarda en la subcoleccion firestore users/{patientId}/allergies
public class Allergy {

    // id del documento en firestore tambien guardado como campo
    private String id;
    // id del paciente dueno de la alergia (desnormalizado, vive bajo su subcoleccion)
    private String patientId;
    // nombre del alergeno ej Penicilina, Maní, Polen
    private String allergen;
    // tipo de alergia ej Medicamento, Alimento, Ambiental, Otro
    private String type;
    // gravedad ej Leve, Moderada, Severa
    private String severity;
    // reaccion que provoca ej Urticaria, Anafilaxia
    private String reaction;

    public Allergy() {}

    public Allergy(String id, String patientId, String allergen, String type,
                   String severity, String reaction) {
        this.id = id;
        this.patientId = patientId;
        this.allergen = allergen;
        this.type = type;
        this.severity = severity;
        this.reaction = reaction;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAllergen() { return allergen; }
    public void setAllergen(String allergen) { this.allergen = allergen; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    @Override
    public String toString() {
        return this.allergen + (this.severity != null ? " (" + this.severity + ")" : "");
    }
}
