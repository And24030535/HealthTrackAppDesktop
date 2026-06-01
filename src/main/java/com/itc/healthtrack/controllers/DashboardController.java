package com.itc.healthtrack.controllers;

import com.itc.healthtrack.models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

// controlador del menu lateral y el area central que cambia segun el rol del usuario logeado
public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label roleLabel;
    @FXML private VBox contentArea;
    @FXML private Button btnPatientsList;
    @FXML private Button btnAdminPanel;
    @FXML private Button btnEmergencyContacts;
@FXML private Button btnSpecialties;
    @FXML private Button btnAllergies;

    private User loggedInUser;

    // arranca el panel con el usuario logeado y ajusta la barra lateral y la vista inicial segun el rol
    public void initData(User user) {
        this.loggedInUser = user;

        // el prefijo cambia segun si es medico admin o paciente
        String role = user.getRole() != null ? user.getRole() : "patient";
        switch (role) {
            case "doctor":
                userNameLabel.setText("Dr. " + user.getLastName());
                roleLabel.setText("Médico");
                break;
            case "admin":
                userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                roleLabel.setText("Administrador");
                break;
            default:
                userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                roleLabel.setText("Paciente");
                break;
        }

        // cada rol ve solo los botones que le corresponden y abre su modulo inicial
        if ("patient".equals(role)) {
            btnPatientsList.setVisible(false);
            btnPatientsList.setManaged(false);
            btnAdminPanel.setVisible(false);
            btnAdminPanel.setManaged(false);
            btnSpecialties.setVisible(false);
            btnSpecialties.setManaged(false);
            btnAllergies.setVisible(false);
            btnAllergies.setManaged(false);
            btnEmergencyContacts.setVisible(true);
            btnEmergencyContacts.setManaged(true);
            onShowMetrics();
        } else if ("admin".equals(role)) {
            btnEmergencyContacts.setVisible(false);
            btnEmergencyContacts.setManaged(false);
            btnSpecialties.setVisible(true);
            btnSpecialties.setManaged(true);
            onShowAdmin();
        } else {
            btnAdminPanel.setVisible(false);
            btnAdminPanel.setManaged(false);
            btnEmergencyContacts.setVisible(false);
            btnEmergencyContacts.setManaged(false);
            btnSpecialties.setVisible(false);
            btnSpecialties.setManaged(false);
            onShowPatientsList();
        }
    }

    @FXML
    protected void onShowPatientsList() {
        changeModule("/com/itc/healthtrack/views/patients-view.fxml", "patients");
    }

    @FXML
    protected void onShowAdmin() {
        changeModule("/com/itc/healthtrack/views/admin-view.fxml", "admin");
    }

    @FXML
    protected void onShowMetrics() {
        changeModule("/com/itc/healthtrack/views/metrics-view.fxml", "metrics");
    }

    @FXML
    protected void onShowReports() {
        changeModule("/com/itc/healthtrack/views/reports-view.fxml", "reports");
    }

    @FXML
    protected void onShowRecommendations() {
        changeModule("/com/itc/healthtrack/views/recommendations-view.fxml", "recommendations");
    }

    @FXML
    protected void onShowEmergencyContacts() {
        changeModule("/com/itc/healthtrack/views/emergency_contacts.fxml", "emergency_contacts");
    }

    @FXML
    protected void onShowAllergies() {
        changeModule("/com/itc/healthtrack/views/allergies-view.fxml", "allergies");
    }

    @FXML
    protected void onShowAppointments() {
        changeModule("/com/itc/healthtrack/views/appointments-view.fxml", "appointments");
    }

    @FXML
    protected void onShowTreatments() {
        changeModule("/com/itc/healthtrack/views/treatments-view.fxml", "treatments");
    }

    @FXML
    protected void onShowSpecialties() {
        changeModule("/com/itc/healthtrack/views/specialties-view.fxml", "specialties");
    }

    // carga la vista fxml instancia el controlador correspondiente y le pasa el usuario logeado
    private void changeModule(String fxmlPath, String moduleType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

            switch (moduleType) {
                case "admin":
                    AdminController ac = loader.getController();
                    ac.initData(loggedInUser);
                    break;
                case "patients":
                    PatientsController pc = loader.getController();
                    pc.initData(loggedInUser);
                    break;
                case "metrics":
                    MetricsController mc = loader.getController();
                    mc.initData(loggedInUser);
                    break;
                case "reports":
                    ReportsController rc = loader.getController();
                    rc.initData(loggedInUser);
                    break;
                case "recommendations":
                    RecommendationsController rcc = loader.getController();
                    rcc.initData(loggedInUser);
                    break;
                case "emergency_contacts":
                    EmergencyContactsController ecc = loader.getController();
                    ecc.initData(loggedInUser);
                    break;
                case "allergies":
                    AllergiesController alc = loader.getController();
                    alc.initData(loggedInUser);
                    break;
                case "appointments":
                    AppointmentsController apc = loader.getController();
                    apc.initData(loggedInUser);
                    break;
                case "treatments":
                    TreatmentsController tc = loader.getController();
                    tc.initData(loggedInUser);
                    break;
                case "specialties":
                    SpecialtiesController sc = loader.getController();
                    sc.initData(loggedInUser);
                    break;
            }

            // limpiamos el area central y colocamos el nuevo modulo
            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al cargar el módulo: " + fxmlPath);
        }
    }

    // cierra la sesion del usuario y regresa a la pantalla de login manteniendo pantalla completa
    @FXML
    protected void onLogout(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/itc/healthtrack/views/login-view.fxml"));
            Scene loginScene = new Scene(fxmlLoader.load(), 960, 620);

            loginScene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
            String cssPath = getClass().getResource("/css/main.css").toExternalForm();
            loginScene.getStylesheets().add(cssPath);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(loginScene);
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
