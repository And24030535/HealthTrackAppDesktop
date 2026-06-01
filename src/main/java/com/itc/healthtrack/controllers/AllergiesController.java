package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Allergy;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.services.UserService;
import com.itc.healthtrack.utils.DialogUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

// gestion de alergias del paciente
// el paciente ve y edita las suyas y el medico o admin elige un paciente del combo
// las alergias viven en la subcoleccion users/{uid}/allergies
public class AllergiesController {

    @FXML private ComboBox<User> comboPatients;

    @FXML private Label           lblFormTitle;
    @FXML private TextField       txtAllergen;
    @FXML private ComboBox<String> comboType;
    @FXML private ComboBox<String> comboSeverity;
    @FXML private TextField       txtReaction;
    @FXML private Button          btnSave;

    @FXML private TableView<Allergy>            tableAllergies;
    @FXML private TableColumn<Allergy, String>  colAllergen;
    @FXML private TableColumn<Allergy, String>  colType;
    @FXML private TableColumn<Allergy, String>  colSeverity;
    @FXML private TableColumn<Allergy, String>  colReaction;

    @FXML private Label lblStatus;

    private final ObservableList<Allergy> allergiesObservableList = FXCollections.observableArrayList();
    private final UserService userService = new UserService();

    private User loggedInUser;
    // paciente cuyas alergias se estan gestionando
    private User currentPatient;
    // dao apuntado a la subcoleccion del paciente actual y se rearma al cambiar de paciente
    private GenericDAO<Allergy> allergyDao;
    private Allergy selectedAllergy = null;

    // arranca el controlador con el usuario logeado y ajusta el combo segun el rol
    public void initData(User user) {
        this.loggedInUser = user;
        setupTable();
        setupCombos();

        if ("patient".equals(user.getRole())) {
            // el paciente solo gestiona sus propias alergias
            comboPatients.getItems().add(user);
            comboPatients.getSelectionModel().selectFirst();
            comboPatients.setDisable(true);
            selectPatient(user);
        } else {
            // medicos y admins eligen al paciente del desplegable
            loadPatientsIntoCombo();
            comboPatients.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    onClearForm();
                    selectPatient(newVal);
                }
            });
        }
    }

    // llena las opciones de los combos de tipo y gravedad
    private void setupCombos() {
        comboType.setItems(FXCollections.observableArrayList("Medicamento", "Alimento", "Ambiental", "Otro"));
        comboSeverity.setItems(FXCollections.observableArrayList("Leve", "Moderada", "Severa"));
    }

    // arma el dao para el paciente elegido y carga sus alergias
    private void selectPatient(User patient) {
        this.currentPatient = patient;
        this.allergyDao = new GenericDAO<>(Allergy.class, "users/" + patient.getUid() + "/allergies");
        loadAllergies();
    }

    // configura las columnas y el listener de seleccion de la tabla
    private void setupTable() {
        colAllergen.setCellValueFactory(new PropertyValueFactory<>("allergen"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colReaction.setCellValueFactory(new PropertyValueFactory<>("reaction"));
        tableAllergies.setItems(allergiesObservableList);

        // al seleccionar una fila llenamos el formulario para editar
        tableAllergies.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedAllergy = newVal;
                txtAllergen.setText(newVal.getAllergen());
                comboType.setValue(newVal.getType());
                comboSeverity.setValue(newVal.getSeverity());
                txtReaction.setText(newVal.getReaction());
                lblFormTitle.setText("Editar Alergia");
                btnSave.setText("Actualizar");
            }
        });
    }

    // carga los pacientes visibles para el usuario logeado en el combo
    private void loadPatientsIntoCombo() {
        new Thread(() -> {
            try {
                List<User> patients = userService.getPatientsForUser(loggedInUser);
                Platform.runLater(() -> comboPatients.setItems(FXCollections.observableArrayList(patients)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // carga las alergias del paciente actual en un hilo de fondo
    private void loadAllergies() {
        if (allergyDao == null) return;
        new Thread(() -> {
            try {
                List<Allergy> all = allergyDao.getAll();
                Platform.runLater(() -> {
                    allergiesObservableList.clear();
                    allergiesObservableList.addAll(all);
                    lblStatus.setText(all.size() + " alergia(s) registrada(s).");
                    lblStatus.setTextFill(Color.web("#aaaaaa"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar las alergias.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // agrega una alergia nueva o actualiza la seleccionada
    @FXML
    protected void onSaveAllergy() {
        if (currentPatient == null || allergyDao == null) {
            lblStatus.setText("Selecciona un paciente primero.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final String allergen  = txtAllergen.getText() == null  ? "" : txtAllergen.getText().trim();
        final String type      = comboType.getValue();
        final String severity  = comboSeverity.getValue();
        final String reaction  = txtReaction.getText() == null  ? "" : txtReaction.getText().trim();

        if (allergen.isEmpty()) {
            lblStatus.setText("El nombre del alérgeno es obligatorio.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }
        if (severity == null) {
            lblStatus.setText("Selecciona la gravedad de la alergia.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final boolean isNew = (selectedAllergy == null);
        lblStatus.setText(isNew ? "Guardando alergia..." : "Actualizando alergia...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                Allergy allergy = isNew ? new Allergy() : selectedAllergy;
                if (isNew) {
                    allergy.setId(allergyDao.createDocumentId());
                    allergy.setPatientId(currentPatient.getUid());
                }
                allergy.setAllergen(allergen);
                allergy.setType(type);
                allergy.setSeverity(severity);
                allergy.setReaction(reaction);

                allergyDao.save(allergy.getId(), allergy);

                Platform.runLater(() -> {
                    loadAllergies();
                    onClearForm();
                    lblStatus.setText(isNew ? "Alergia agregada correctamente." : "Alergia actualizada correctamente.");
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

    // elimina la alergia seleccionada previa confirmacion
    @FXML
    protected void onDeleteAllergy() {
        if (selectedAllergy == null) {
            lblStatus.setText("Selecciona una alergia de la tabla para eliminar.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final String id      = selectedAllergy.getId();
        final String allergen = selectedAllergy.getAllergen();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar alergia");
        confirm.setContentText("¿Eliminar la alergia \"" + allergen + "\" del expediente?");
        DialogUtils.applyWhiteStyle(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            lblStatus.setText("Eliminando...");
            lblStatus.setTextFill(Color.web("#ffffff"));
            new Thread(() -> {
                try {
                    allergyDao.delete(id);
                    Platform.runLater(() -> {
                        loadAllergies();
                        onClearForm();
                        lblStatus.setText("Alergia eliminada correctamente.");
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
        });
    }

    // limpia el formulario y deselecciona la alergia actual
    @FXML
    protected void onClearForm() {
        txtAllergen.clear();
        comboType.setValue(null);
        comboSeverity.setValue(null);
        txtReaction.clear();
        selectedAllergy = null;
        tableAllergies.getSelectionModel().clearSelection();
        lblFormTitle.setText("Nueva Alergia");
        btnSave.setText("Agregar");
    }
}
