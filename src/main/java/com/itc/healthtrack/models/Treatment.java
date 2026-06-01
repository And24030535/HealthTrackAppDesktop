package com.itc.healthtrack.models;

// tratamiento medico del paciente guardado en la coleccion treatments
// el medico crea el tratamiento y luego le agrega detalles con medicamentos en la subcoleccion
// cuando closed es true el tratamiento queda en modo solo lectura
public class Treatment {

    private String id;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String diagnosis;
    private String startDate;   // formato texto aaaa mm dd
    private String endDate;     // formato texto aaaa mm dd puede quedar vacio
    private boolean closed;

    // constructor vacio requerido por el sdk de firestore
    public Treatment() {}

    public Treatment(String id, String patientId, String patientName,
                     String doctorId, String doctorName,
                     String diagnosis, String startDate, String endDate,
                     boolean closed) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.diagnosis = diagnosis;
        this.startDate = startDate;
        this.endDate = endDate;
        this.closed = closed;
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

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    @Override
    public String toString() {
        return diagnosis + " (" + startDate + ")" + (closed ? " [Cerrado]" : "");
    }
}
