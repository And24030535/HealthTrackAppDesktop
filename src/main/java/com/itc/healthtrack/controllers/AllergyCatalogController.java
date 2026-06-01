package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Allergy;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

// administra el catalogo global de alergias disponible solo para el admin
// las alergias del catalogo se eliminan directamente porque no tienen concepto de activo/inactivo
public class AllergyCatalogController {

    @FXML private TextField txtAllergyName;
    @FXML private ComboBox<String> comboSeverity;
    @FXML private TableView<Allergy> tableAllergies;
    @FXML private TableColumn<Allergy, String> colAllergyName;
    @FXML private TableColumn<Allergy, String> colAllergySeverity;
    @FXML private Label lblAllergyStatus;
    @FXML private Button btnToggleActive;

    private final GenericDAO<Allergy> allergyDao = new GenericDAO<>(Allergy.class, "allergies");
    private final ObservableList<Allergy> allergiesObservableList = FXCollections.observableArrayList();

    private Allergy selectedAllergy;

    public void initData(User admin) {
        setupSeverityCombo();
        setupTable();
        loadAllergies();
    }

    private void setupSeverityCombo() {
        if (comboSeverity != null) {
            comboSeverity.setItems(FXCollections.observableArrayList("Leve", "Moderada", "Severa"));
            comboSeverity.getSelectionModel().selectFirst();
        }
    }

    // configura columnas de la tabla con severidad traducida al espanol
    private void setupTable() {
        colAllergyName.setCellValueFactory(new PropertyValueFactory<>("allergen"));
        colAllergySeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colAllergySeverity.setCellFactory(column -> new TableCell<Allergy, String>() {
            @Override
            protected void updateItem(String severity, boolean empty) {
                super.updateItem(severity, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(toSeverityLabel(severity));
                }
            }
        });

        tableAllergies.setItems(allergiesObservableList);
        tableAllergies.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedAllergy = newVal;
                fillForm(newVal);
                updateToggleButtonText();
                lblAllergyStatus.setText("Alergia seleccionada: " + newVal.getAllergen());
                lblAllergyStatus.setTextFill(Color.web("#aaaaaa"));
            }
        });
    }

    private void loadAllergies() {
        new Thread(() -> {
            try {
                List<Allergy> all = allergyDao.getAll();
                Platform.runLater(() -> {
                    allergiesObservableList.clear();
                    allergiesObservableList.addAll(all);
                    lblAllergyStatus.setText("Alergias cargadas: " + all.size());
                    lblAllergyStatus.setTextFill(Color.web("#aaaaaa"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblAllergyStatus.setText("Error al cargar el catálogo de alergias.");
                    lblAllergyStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // agrega una nueva alergia al catalogo validando nombre no vacio y sin duplicados
    @FXML
    protected void onAddAllergy() {
        String name          = txtAllergyName.getText() != null ? txtAllergyName.getText().trim() : "";
        String severityLabel = comboSeverity.getValue();

        if (name.isEmpty()) {
            lblAllergyStatus.setText("Ingresa el nombre de la alergia");
            lblAllergyStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        // no permitimos duplicados ignorando mayusculas
        for (Allergy existing : allergiesObservableList) {
            if (existing.getAllergen() != null && existing.getAllergen().equalsIgnoreCase(name)) {
                lblAllergyStatus.setText("Ya existe una alergia con ese nombre");
                lblAllergyStatus.setTextFill(Color.web("#ff9800"));
                return;
            }
        }

        String severityValue = toSeverityValue(severityLabel);
        lblAllergyStatus.setText("Guardando alergia...");
        lblAllergyStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                String id      = allergyDao.createDocumentId();
                Allergy allergy = new Allergy();
                allergy.setId(id);
                allergy.setAllergen(name);
                allergy.setSeverity(severityValue);
                allergyDao.save(id, allergy);

                Platform.runLater(() -> {
                    allergiesObservableList.add(allergy);
                    tableAllergies.refresh();
                    onClearForm();
                    lblAllergyStatus.setText("Alergia registrada correctamente");
                    lblAllergyStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblAllergyStatus.setText("Error al guardar la alergia");
                    lblAllergyStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // elimina la alergia seleccionada del catalogo
    @FXML
    protected void onToggleActive() {
        if (selectedAllergy == null) {
            lblAllergyStatus.setText("Selecciona una alergia");
            lblAllergyStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        if (selectedAllergy.getId() == null) {
            lblAllergyStatus.setText("Error la alergia seleccionada no tiene ID válido");
            lblAllergyStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        lblAllergyStatus.setText("Eliminando alergia...");
        lblAllergyStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                allergyDao.delete(selectedAllergy.getId());
                Platform.runLater(() -> {
                    onClearForm();
                    loadAllergies();
                    lblAllergyStatus.setText("Alergia eliminada");
                    lblAllergyStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblAllergyStatus.setText("Error al eliminar la alergia");
                    lblAllergyStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onClearForm() {
        txtAllergyName.clear();
        comboSeverity.getSelectionModel().selectFirst();
        selectedAllergy = null;
        tableAllergies.getSelectionModel().clearSelection();
        updateToggleButtonText();
    }

    private void fillForm(Allergy allergy) {
        if (allergy == null) return;
        txtAllergyName.setText(allergy.getAllergen());
        comboSeverity.setValue(toSeverityLabel(allergy.getSeverity()));
    }

    private void updateToggleButtonText() {
        if (btnToggleActive == null) return;
        btnToggleActive.setText("Eliminar");
    }

    // convierte la etiqueta en espanol al valor interno en ingles para firestore
    private String toSeverityValue(String label) {
        if (label == null) return "mild";
        switch (label) {
            case "Moderada": return "moderate";
            case "Severa":   return "severe";
            default:         return "mild";
        }
    }

    // convierte el valor interno en ingles a la etiqueta legible en espanol
    private String toSeverityLabel(String value) {
        if (value == null) return "Leve";
        switch (value) {
            case "moderate": return "Moderada";
            case "severe":   return "Severa";
            default:         return "Leve";
        }
    }
}
