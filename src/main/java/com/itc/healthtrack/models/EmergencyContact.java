package com.itc.healthtrack.models;

// contacto de emergencia asociado a un paciente
public class EmergencyContact {

    private String id;
    private String fullName;
    private String phone;
    private String relationship;

    public EmergencyContact() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
}
