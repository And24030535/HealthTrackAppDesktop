package com.itc.healthtrack.models;

// usuario del sistema puede tener rol patient doctor o admin
public class User {

    private String uid;
    private String email;
    private String firstName;
    private String lastName;
    // rol del usuario patient doctor o admin
    private String role;
    // id de la especialidad medica referencia a la coleccion specialties
    private String specialtyId;
    // nombre de la especialidad desnormalizado para mostrarlo sin consulta extra a firestore
    private String specialtyName;
    // numero de licencia profesional para medicos
    private String numLicencia;

    // campos exclusivos de pacientes
    // fecha de nacimiento guardada como texto para no lidiar con zonas horarias por ejemplo 2000 01 15
    private String birthDate;
    // puede ser M F u Otro
    private String gender;
    // estatura en metros
    private Double height;
    private String assignedDoctorId;
    // nombre del medico desnormalizado para mostrar en la ui sin consulta extra a firestore
    private String assignedDoctorName;

    public User() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(String specialtyId) { this.specialtyId = specialtyId; }

    public String getSpecialtyName() { return specialtyName; }
    public void setSpecialtyName(String specialtyName) { this.specialtyName = specialtyName; }

    public String getNumLicencia() { return numLicencia; }
    public void setNumLicencia(String numLicencia) { this.numLicencia = numLicencia; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public String getAssignedDoctorId() { return assignedDoctorId; }
    public void setAssignedDoctorId(String assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }

    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

    @Override
    public String toString() {
        return this.firstName + " " + this.lastName;
    }
}
