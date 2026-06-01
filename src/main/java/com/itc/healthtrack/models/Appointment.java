package com.itc.healthtrack.models;

// FEATURE 5 — Cita medica entre un paciente y su medico
// se guarda en la coleccion top-level "appointments" para que paciente y medico la consulten por su id
public class Appointment {

    // id del documento en firestore tambien guardado como campo
    private String id;

    // paciente que solicita la cita (ids desnormalizados con su nombre para mostrar sin consulta extra)
    private String patientId;
    private String patientName;

    // medico que atendera la cita
    private String doctorId;
    private String doctorName;

    // fecha como texto yyyy-MM-dd y hora como texto HH:mm para ordenar y mostrar sin lios de zona horaria
    private String date;
    private String time;

    // motivo de la consulta
    private String reason;
    // estado de la cita Pendiente, Confirmada, Cancelada o Completada
    private String status;
    // notas del medico (opcional)
    private String notes;

    public Appointment() {}

    public Appointment(String id, String patientId, String patientName, String doctorId, String doctorName,
                       String date, String time, String reason, String status, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
        this.reason = reason;
        this.status = status;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return this.date + " " + this.time + " - " + this.patientName;
    }
}
