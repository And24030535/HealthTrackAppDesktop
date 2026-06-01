package com.itc.healthtrack.models;

// contacto de emergencia de un paciente guardado en la subcoleccion firestore users/{patientId}/emergencyContacts
public class EmergencyContact {

    // id del documento en firestore tambien guardado como campo del documento
    private String id;
    // id del paciente dueno del contacto desnormalizado porque el contacto vive bajo su subcoleccion
    private String patientId;
    private String name;
    private String phone;
    // parentesco o relacion con el paciente por ejemplo Madre Hermano o Amigo
    private String relationship;
    // correo al que se le avisa cuando hay una alerta critica via JavaMail
    private String email;
    // indica si es el contacto principal al que se notifica en emergencias
    private boolean primary;

    public EmergencyContact() {}

    public EmergencyContact(String id, String patientId, String name, String phone,
                            String relationship, String email, boolean primary) {
        this.id = id;
        this.patientId = patientId;
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
        this.email = email;
        this.primary = primary;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    @Override
    public String toString() {
        return this.name + (this.relationship != null ? " (" + this.relationship + ")" : "");
    }
}
