package com.itc.healthtrack.models;

// alergia asociada a un paciente
public class PatientAllergy {

    private String id;
    private String allergyId;
    private String detectionDate;
    private String notes;

    public PatientAllergy() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAllergyId() { return allergyId; }
    public void setAllergyId(String allergyId) { this.allergyId = allergyId; }

    public String getDetectionDate() { return detectionDate; }
    public void setDetectionDate(String detectionDate) { this.detectionDate = detectionDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
