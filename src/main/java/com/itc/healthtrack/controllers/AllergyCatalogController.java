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

// controlador para administrar el catalogo de alergias
public class AllergyCatalogController {

    @FXML private TextField txtAllergyName;
    @FXML private ComboBox<String> comboSeverity;
    @FXML private TableView<Allergy> tableAllergies;
    @FXML private TableColumn<Allergy, String> colAllergyName;
    @FXML private TableColumn<Allergy, String> colAllergySeverity;
    @FXML private TableColumn<Allergy, Boolean> colAllergyStatus;
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

    private void setupTable() {
        colAllergyName.setCellValueFactory(new PropertyValueFactory<>("name"));
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
        colAllergyStatus.setCellValueFactory(new PropertyValueFactory<>("active"));
        colAllergyStatus.setCellFactory(column -> new TableCell<Allergy, Boolean>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(Boolean.TRUE.equals(active) ? "Activa" : "Inactiva");
                }
            }
        });

        tableAllergies.setItems(allergiesObservableList);
        tableAllergies.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedAllergy = newVal;
                fillForm(newVal);
                updateToggleButtonText();
                lblAllergyStatus.setText("Alergia seleccionada: " + newVal.getName());
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

    @FXML
    protected void onAddAllergy() {
        String name = txtAllergyName.getText() != null ? txtAllergyName.getText().trim() : "";
        String severityLabel = comboSeverity.getValue();

        if (name.isEmpty()) {
            lblAllergyStatus.setText("Ingresa el nombre de la alergia.");
            lblAllergyStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        for (Allergy existing : allergiesObservableList) {
            if (existing.getName() != null && existing.getName().equalsIgnoreCase(name)) {
                lblAllergyStatus.setText("Ya existe una alergia con ese nombre.");
                lblAllergyStatus.setTextFill(Color.web("#ff9800"));
                return;
            }
        }

        String severityValue = toSeverityValue(severityLabel);
        lblAllergyStatus.setText("Guardando alergia...");
        lblAllergyStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                String id = allergyDao.createDocumentId();
                Allergy allergy = new Allergy();
                allergy.setId(id);
                allergy.setName(name);
                allergy.setSeverity(severityValue);
                allergy.setActive(true);
                allergyDao.save(id, allergy);

                Platform.runLater(() -> {
                    allergiesObservableList.add(allergy);
                    tableAllergies.refresh();
                    onClearForm();
                    lblAllergyStatus.setText("Alergia registrada correctamente.");
                    lblAllergyStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblAllergyStatus.setText("Error al guardar la alergia.");
                    lblAllergyStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onToggleActive() {
        if (selectedAllergy == null) {
            lblAllergyStatus.setText("Selecciona una alergia.");
            lblAllergyStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        boolean isActive = Boolean.TRUE.equals(selectedAllergy.getActive());
        boolean newState = !isActive;
        selectedAllergy.setActive(newState);

        lblAllergyStatus.setText(newState ? "Activando alergia..." : "Desactivando alergia...");
        lblAllergyStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                allergyDao.save(selectedAllergy.getId(), selectedAllergy);
                Platform.runLater(() -> {
                    tableAllergies.refresh();
                    updateToggleButtonText();
                    lblAllergyStatus.setText(newState ? "Alergia activada." : "Alergia desactivada.");
                    lblAllergyStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblAllergyStatus.setText("Error al actualizar la alergia.");
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
        txtAllergyName.setText(allergy.getName());
        comboSeverity.setValue(toSeverityLabel(allergy.getSeverity()));
    }

    private void updateToggleButtonText() {
        if (btnToggleActive == null) return;
        if (selectedAllergy == null) {
            btnToggleActive.setText("Desactivar");
        } else {
            btnToggleActive.setText(Boolean.TRUE.equals(selectedAllergy.getActive()) ? "Desactivar" : "Activar");
        }
    }

    private String toSeverityValue(String label) {
        if (label == null) return "mild";
        switch (label) {
            case "Moderada": return "moderate";
            case "Severa": return "severe";
            default: return "mild";
        }
    }

    private String toSeverityLabel(String value) {
        if (value == null) return "Leve";
        switch (value) {
            case "moderate": return "Moderada";
            case "severe": return "Severa";
            default: return "Leve";
        }
    }
}
