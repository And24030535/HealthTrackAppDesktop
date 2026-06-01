package com.itc.healthtrack.models;

// alergia de un paciente guardada en la subcoleccion firestore users/{patientId}/allergies
public class Allergy {

    // id del documento en firestore tambien guardado como campo del documento
    private String id;
    // id del paciente dueno de la alergia desnormalizado porque vive bajo su subcoleccion
    private String patientId;
    // nombre del alergeno por ejemplo Penicilina Mani o Polen
    private String allergen;
    // tipo de alergia por ejemplo Medicamento Alimento Ambiental u Otro
    private String type;
    // nivel de gravedad por ejemplo Leve Moderada o Severa
    private String severity;
    // reaccion que provoca por ejemplo Urticaria o Anafilaxia
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
