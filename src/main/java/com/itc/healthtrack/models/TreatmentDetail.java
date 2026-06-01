package com.itc.healthtrack.models;

// FEATURE 6 — detalle de un tratamiento: un medicamento con su dosis y frecuencia
// vive en la subcoleccion "treatments/{treatmentId}/details"
// FEATURE 7: medicineId referencia al catalogo; medicineName se desnormaliza para no perder
//            el nombre si el medicamento fuera desactivado en el futuro
public class TreatmentDetail {

    private String id;
    private String treatmentId;
    private String medicineId;    // id del documento en la coleccion "medicines"
    private String medicineName;  // nombre desnormalizado para display historico
    private String dose;
    private String frequency;

    // constructor vacio requerido por el sdk de firestore
    public TreatmentDetail() {}

    public TreatmentDetail(String id, String treatmentId,
                           String medicineId, String medicineName,
                           String dose, String frequency) {
        this.id = id;
        this.treatmentId = treatmentId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.dose = dose;
        this.frequency = frequency;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTreatmentId() { return treatmentId; }
    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
}
