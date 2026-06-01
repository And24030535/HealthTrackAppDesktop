package com.itc.healthtrack.controllers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// crud de pacientes las notas medicas viven en RecommendationsController
public class PatientsController {

    @FXML private TextField    txtFirstName, txtLastName, txtEmail, txtHeight;
    @FXML private ComboBox<String> comboGender;
    @FXML private DatePicker   dpBirthDate;

    @FXML private TableView<User>           tablePatients;
    @FXML private TableColumn<User, String> colFirstName, colLastName, colEmail, colGender;

    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");
    private final ObservableList<User> patientsObservableList = FXCollections.observableArrayList();

    private User loggedInDoctor;
    private User selectedPatient = null;

    public void initData(User doctor) {
        this.loggedInDoctor = doctor;
        setupTable();
        comboGender.setItems(FXCollections.observableArrayList("M", "F", "Otro"));
        loadPatients();
    }

    private void setupTable() {
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName .setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail    .setCellValueFactory(new PropertyValueFactory<>("email"));
        colGender   .setCellValueFactory(new PropertyValueFactory<>("gender"));
        tablePatients.setItems(patientsObservableList);

        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedPatient = newVal;
                fillForm(newVal);
            }
        });
    }

    // carga los pacientes visibles para el usuario logeado
    // el admin ve todos y el medico solo los que tiene asignados
    private void loadPatients() {
        new Thread(() -> {
            try {
                List<User> all = userDao.getByField("role", "patient");
                List<User> visible = new ArrayList<>();
                for (User p : all) {
                    if ("admin".equals(loggedInDoctor.getRole())) {
                        visible.add(p);
                    } else if (loggedInDoctor.getUid() != null && loggedInDoctor.getUid().equals(p.getAssignedDoctorId())) {
                        visible.add(p);
                    }
                }
                Platform.runLater(() -> {
                    patientsObservableList.clear();
                    patientsObservableList.addAll(visible);
                });
            } catch (Exception e) {
                System.err.println("[PatientsController] Error al cargar pacientes: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    protected void onSavePatient() {
        try {
            if (selectedPatient == null) {
                // crear nuevo paciente con password temporal que el admin debe compartir al paciente
                User newPatient = new User();
                fillUserFromForm(newPatient);
                newPatient.setRole("patient");
                newPatient.setAssignedDoctorId(loggedInDoctor.getUid());

                String tempPassword  = "Tmp@" + UUID.randomUUID().toString().substring(0, 8);
                final String emailForAuth = newPatient.getEmail();

                new Thread(() -> {
                    try {
                        // crear cuenta en Firebase Auth
                        UserRecord.CreateRequest authRequest = new UserRecord.CreateRequest()
                                .setEmail(emailForAuth)
                                .setPassword(tempPassword);
                        UserRecord createdRecord = FirebaseAuth.getInstance().createUser(authRequest);
                        String uid = createdRecord.getUid();
                        // guardar perfil en Firestore usando el uid de Auth como id del documento
                        newPatient.setUid(uid);
                        userDao.save(uid, newPatient);

                        Platform.runLater(() -> {
                            onClearForm();
                            loadPatients();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("[PatientsController] Error al crear paciente: " + e.getMessage());
                    }
                }).start();

            } else {
                // actualizar paciente existente
                fillUserFromForm(selectedPatient);
                new Thread(() -> {
                    try {
                        userDao.save(selectedPatient.getUid(), selectedPatient);
                        Platform.runLater(() -> { onClearForm(); loadPatients(); });
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }
        } catch (NumberFormatException e) {
            System.err.println("[PatientsController] Error: la altura debe ser un número.");
        }
    }

    @FXML
    protected void onDeletePatient() {
        if (selectedPatient == null) return;
        String uidToDelete = selectedPatient.getUid();

        new Thread(() -> {
            try {
                // eliminamos la cuenta de Firebase Auth para que el paciente no pueda seguir autenticandose
                if (uidToDelete != null && !uidToDelete.isEmpty()) {
                    FirebaseAuth.getInstance().deleteUser(uidToDelete);
                    System.out.println("[PatientsController] Auth eliminado — UID: " + uidToDelete);
                }

                // eliminamos el perfil de Firestore
                userDao.delete(uidToDelete);

                Platform.runLater(() -> { onClearForm(); loadPatients(); });
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("[PatientsController] Error al eliminar paciente: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    protected void onClearForm() {
        txtFirstName.clear();
        txtLastName .clear();
        txtEmail    .clear();
        dpBirthDate .setValue(null);
        comboGender .setValue(null);
        txtHeight   .clear();
        selectedPatient = null;
        tablePatients.getSelectionModel().clearSelection();
    }

    @FXML
    protected void onShowPatientAllergies() {
        if (selectedPatient == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Alergias del paciente");
            alert.setHeaderText("Selecciona un paciente");
            alert.setContentText("Selecciona un paciente de la tabla para ver sus alergias.");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/patient_allergies.fxml"));
            Parent root = loader.load();
            PatientAllergyController controller = loader.getController();
            controller.initData(loggedInDoctor, selectedPatient);

            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Alergias del paciente");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner((Stage) tablePatients.getScene().getWindow());
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // rellena los campos del formulario con los datos del paciente seleccionado
    private void fillForm(User p) {
        txtFirstName.setText(p.getFirstName());
        txtLastName .setText(p.getLastName());
        txtEmail    .setText(p.getEmail());
        dpBirthDate .setValue(p.getBirthDate() != null ? LocalDate.parse(p.getBirthDate()) : null);
        comboGender .setValue(p.getGender());
        txtHeight   .setText(p.getHeight() != null ? String.valueOf(p.getHeight()) : "");
    }

    // copia lo que el usuario escribio en el formulario al objeto usuario
    private void fillUserFromForm(User u) {
        u.setFirstName(txtFirstName.getText());
        u.setLastName (txtLastName.getText());
        u.setEmail    (txtEmail.getText());
        u.setBirthDate(dpBirthDate.getValue() != null ? dpBirthDate.getValue().toString() : null);
        u.setGender   (comboGender.getValue());
        u.setHeight   (txtHeight.getText().isEmpty() ? null : Double.parseDouble(txtHeight.getText()));
    }
}
