package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Allergy;
import com.itc.healthtrack.models.PatientAllergy;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// controlador para asociar alergias del catalogo a un paciente especifico
// el catalogo global vive en la coleccion allergies y la asociacion en users/{uid}/patientAllergies
public class PatientAllergyController {

    @FXML private Label lblPatientName;
    @FXML private ComboBox<Allergy> comboAllergies;
    @FXML private DatePicker dpDetectionDate;
    @FXML private TextArea txtNotes;
    @FXML private TableView<PatientAllergy> tableAllergies;
    @FXML private TableColumn<PatientAllergy, String> colAllergyName;
    @FXML private TableColumn<PatientAllergy, String> colSeverity;
    @FXML private TableColumn<PatientAllergy, String> colDetectionDate;
    @FXML private TableColumn<PatientAllergy, String> colNotes;
    @FXML private Label lblStatus;

    // dao del catalogo global de alergias
    private final GenericDAO<Allergy> allergyDao = new GenericDAO<>(Allergy.class, "allergies");
    // dao de las alergias asociadas al paciente se inicializa cuando se conoce su uid
    private GenericDAO<PatientAllergy> patientAllergyDao;

    private final ObservableList<PatientAllergy> patientAllergyList = FXCollections.observableArrayList();
    private final ObservableList<Allergy> allergiesObservableList = FXCollections.observableArrayList();
    // mapa id->alergia para resolver nombres rapidamente sin volver a consultar firestore
    private final Map<String, Allergy> allergyMap = new HashMap<>();

    private User selectedPatient;
    private PatientAllergy selectedPatientAllergy;

    public void initData(User doctor, User patient) {
        this.selectedPatient = patient;
        if (patient != null && patient.getUid() != null) {
            this.patientAllergyDao = new GenericDAO<>(PatientAllergy.class,
                    "users/" + patient.getUid() + "/patientAllergies");
        }
        setupTable();
        setupCombo();
        updatePatientHeader();
        loadAllergyCatalog();
        loadPatientAllergies();
    }

    private void updatePatientHeader() {
        if (lblPatientName == null) return;
        if (selectedPatient == null) {
            lblPatientName.setText("Paciente no seleccionado");
        } else {
            lblPatientName.setText(selectedPatient.getFirstName() + " " + selectedPatient.getLastName());
        }
    }

    // configura el combobox del catalogo con celdas personalizadas que muestran nombre y severidad
    private void setupCombo() {
        comboAllergies.setItems(allergiesObservableList);
        comboAllergies.setCellFactory(lv -> new ListCell<Allergy>() {
            @Override
            protected void updateItem(Allergy allergy, boolean empty) {
                super.updateItem(allergy, empty);
                setText(empty || allergy == null ? null : formatAllergyLabel(allergy));
            }
        });
        comboAllergies.setButtonCell(new ListCell<Allergy>() {
            @Override
            protected void updateItem(Allergy allergy, boolean empty) {
                super.updateItem(allergy, empty);
                setText(empty || allergy == null ? null : formatAllergyLabel(allergy));
            }
        });
    }

    private void setupTable() {
        // resolvemos el nombre de la alergia usando el mapa en memoria para no ir a firestore por cada fila
        colAllergyName.setCellValueFactory(data ->
                new SimpleStringProperty(getAllergyName(data.getValue().getAllergyId())));
        colSeverity.setCellValueFactory(data ->
                new SimpleStringProperty(getSeverityLabel(data.getValue().getAllergyId())));
        colDetectionDate.setCellValueFactory(new PropertyValueFactory<>("detectionDate"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        tableAllergies.setItems(patientAllergyList);
        tableAllergies.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedPatientAllergy = newVal;

                fillForm(newVal);
            }
        });
    }

    // carga el catalogo global de alergias y construye el mapa id->alergia
    private void loadAllergyCatalog() {
        new Thread(() -> {
            try {
                List<Allergy> all = allergyDao.getAll();
                Platform.runLater(() -> {
                    allergyMap.clear();
                    allergiesObservableList.clear();
                    allergiesObservableList.addAll(all);
                    for (Allergy allergy : all) {
                        if (allergy != null && allergy.getId() != null) {
                            allergyMap.put(allergy.getId(), allergy);
                        }
                    }
                    tableAllergies.refresh();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar el catálogo de alergias.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // carga las alergias ya asociadas al paciente desde su subcoleccion
    private void loadPatientAllergies() {
        if (patientAllergyDao == null) return;
        new Thread(() -> {
            try {
                List<PatientAllergy> all = patientAllergyDao.getAll();
                Platform.runLater(() -> {
                    patientAllergyList.clear();
                    patientAllergyList.addAll(all);
                });
            } catch (Exception e) {
                System.err.println("[PatientAllergyController] Error al cargar alergias: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    protected void onSaveAllergy() {
        if (patientAllergyDao == null || selectedPatient == null) {
            lblStatus.setText("No se pudo cargar el paciente.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        Allergy allergy = comboAllergies.getValue();
        if (allergy == null) {
            lblStatus.setText("Selecciona una alergia.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        LocalDate detectionDate = dpDetectionDate.getValue();
        if (detectionDate == null) {
            lblStatus.setText("Selecciona la fecha de detección.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        boolean isNew = (selectedPatientAllergy == null);
        PatientAllergy record = isNew ? new PatientAllergy() : selectedPatientAllergy;
        record.setAllergyId(allergy.getId());
        record.setDetectionDate(detectionDate.toString());
        record.setNotes(txtNotes.getText());

        lblStatus.setText(isNew ? "Guardando..." : "Actualizando...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                if (isNew) {
                    String id = patientAllergyDao.createDocumentId();
                    record.setId(id);
                    patientAllergyDao.save(id, record);
                } else {
                    patientAllergyDao.save(record.getId(), record);
                }

                Platform.runLater(() -> {
                    onClearForm();
                    loadPatientAllergies();
                    lblStatus.setText(isNew ? "Alergia registrada." : "Alergia actualizada.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al guardar la alergia.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onDeleteAllergy() {
        if (selectedPatientAllergy == null || patientAllergyDao == null) {
            lblStatus.setText("Selecciona una alergia para eliminar.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        String recordId = selectedPatientAllergy.getId();
        lblStatus.setText("Eliminando...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                patientAllergyDao.delete(recordId);
                Platform.runLater(() -> {
                    onClearForm();
                    loadPatientAllergies();
                    lblStatus.setText("Alergia eliminada.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al eliminar la alergia.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onClearForm() {
        comboAllergies.getSelectionModel().clearSelection();
        dpDetectionDate.setValue(null);
        txtNotes.clear();
        selectedPatientAllergy = null;
        tableAllergies.getSelectionModel().clearSelection();
    }

    private void fillForm(PatientAllergy record) {
        if (record == null) return;
        Allergy allergy = allergyMap.get(record.getAllergyId());
        if (allergy != null) {
            comboAllergies.setValue(allergy);
        }
        dpDetectionDate.setValue(record.getDetectionDate() != null
                ? LocalDate.parse(record.getDetectionDate()) : null);
        txtNotes.setText(record.getNotes());
    }

    // resuelve el nombre del alergeno a partir del id usando el mapa en memoria
    private String getAllergyName(String allergyId) {
        Allergy allergy = allergyMap.get(allergyId);
        return allergy != null && allergy.getAllergen() != null ? allergy.getAllergen() : "";
    }

    private String getSeverityLabel(String allergyId) {
        Allergy allergy = allergyMap.get(allergyId);
        return allergy != null ? toSeverityLabel(allergy.getSeverity()) : "";
    }

    private String toSeverityLabel(String value) {
        if (value == null) return "Leve";
        switch (value) {
            case "moderate": return "Moderada";
            case "severe":   return "Severa";
            default:         return "Leve";
        }
    }

    // formatea la etiqueta de la alergia para el combobox incluyendo nombre y nivel de severidad
    private String formatAllergyLabel(Allergy allergy) {
        if (allergy == null) return "";
        String name     = allergy.getAllergen() != null ? allergy.getAllergen() : "Alergia";
        String severity = toSeverityLabel(allergy.getSeverity());
        return name + " — " + severity;
    }
}
