package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Appointment;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.utils.DialogUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// sistema de citas del escritorio
// el paciente agenda con su medico asignado y el medico o admin gestiona el estado de las citas
// todas las citas viven en la coleccion top-level appointments
public class AppointmentsController {

    // seccion de agendado visible solo para pacientes
    @FXML private VBox bookingSection;
    @FXML private Label lblAssignedDoctor;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> comboTime;
    @FXML private TextField txtReason;
    @FXML private Button btnBook;

    @FXML private TableView<Appointment> tableAppointments;
    @FXML private TableColumn<Appointment, String> colDate;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, String> colPatient;
    @FXML private TableColumn<Appointment, String> colDoctor;
    @FXML private TableColumn<Appointment, String> colReason;
    @FXML private TableColumn<Appointment, String> colStatus;

    // botones de gestion de estado visibles solo para medico y admin
    @FXML private Button btnConfirm;
    @FXML private Button btnComplete;
    @FXML private Button btnCancel;

    @FXML private Label lblStatus;

    private final GenericDAO<Appointment> appointmentDao = new GenericDAO<>(Appointment.class, "appointments");
    private final ObservableList<Appointment> appointmentsObservableList = FXCollections.observableArrayList();

    private User loggedInUser;
    private Appointment selectedAppointment = null;

    // arranca el controlador y ajusta la interfaz segun el rol del usuario
    public void initData(User user) {
        this.loggedInUser = user;
        setupTable();
        // franjas horarias disponibles para agendar
        comboTime.setItems(FXCollections.observableArrayList(
                "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"));

        String role = user.getRole() != null ? user.getRole() : "patient";
        if ("patient".equals(role)) {
            // el paciente agenda y solo puede cancelar sus propias citas
            setupPatientView();
            loadAppointmentsByField("patientId", user.getUid());
        } else if ("doctor".equals(role)) {
            // el medico ve las citas dirigidas a el y gestiona su estado
            setupStaffView();
            loadAppointmentsByField("doctorId", user.getUid());
        } else {
            // el admin ve todas las citas del sistema
            setupStaffView();
            loadAllAppointments();
        }
    }

    // muestra el formulario de agendado y oculta los botones de gestion del personal
    private void setupPatientView() {
        bookingSection.setVisible(true);
        bookingSection.setManaged(true);
        btnConfirm.setVisible(false);
        btnConfirm.setManaged(false);
        btnComplete.setVisible(false);
        btnComplete.setManaged(false);

        // mostramos el medico asignado o avisamos si no tiene ninguno
        if (loggedInUser.getAssignedDoctorId() == null || loggedInUser.getAssignedDoctorId().isEmpty()) {
            lblAssignedDoctor.setText("No tienes un médico asignado; no puedes agendar citas.");
            lblAssignedDoctor.setTextFill(Color.web("#ff9800"));
            btnBook.setDisable(true);
        } else {
            String docName = loggedInUser.getAssignedDoctorName() != null
                    ? loggedInUser.getAssignedDoctorName() : "tu médico asignado";
            lblAssignedDoctor.setText("Médico: " + docName);
            lblAssignedDoctor.setTextFill(Color.web("#aaaaaa"));
        }
    }

    // oculta el formulario de agendado y deja los botones de gestion para medico y admin
    private void setupStaffView() {
        bookingSection.setVisible(false);
        bookingSection.setManaged(false);
    }

    // configura las columnas y el listener de seleccion de la tabla
    private void setupTable() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableAppointments.setItems(appointmentsObservableList);

        tableAppointments.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedAppointment = newVal;
                lblStatus.setText("Cita seleccionada: " + newVal.getDate() + " " + newVal.getTime()
                        + " (" + newVal.getStatus() + ")");
                lblStatus.setTextFill(Color.web("#aaaaaa"));
            }
        });
    }

    // carga las citas filtrando por un campo concreto como patientId o doctorId
    private void loadAppointmentsByField(String field, String value) {
        new Thread(() -> {
            try {
                List<Appointment> all = appointmentDao.getByField(field, value);
                sortAndShow(all);
            } catch (Exception e) {
                showLoadError();
                e.printStackTrace();
            }
        }).start();
    }

    // carga todas las citas del sistema para la vista de admin
    private void loadAllAppointments() {
        new Thread(() -> {
            try {
                List<Appointment> all = appointmentDao.getAll();
                sortAndShow(all);
            } catch (Exception e) {
                showLoadError();
                e.printStackTrace();
            }
        }).start();
    }

    // ordena las citas por fecha y hora y refresca la tabla en el hilo de JavaFX
    private void sortAndShow(List<Appointment> all) {
        all.sort(Comparator
                .comparing((Appointment a) -> a.getDate() == null ? "" : a.getDate())
                .thenComparing(a -> a.getTime() == null ? "" : a.getTime()));
        Platform.runLater(() -> {
            appointmentsObservableList.clear();
            appointmentsObservableList.addAll(all);
            lblStatus.setText(all.size() + " cita(s).");
            lblStatus.setTextFill(Color.web("#aaaaaa"));
        });
    }

    private void showLoadError() {
        Platform.runLater(() -> {
            lblStatus.setText("Error al cargar las citas.");
            lblStatus.setTextFill(Color.web("#ff5252"));
        });
    }

    // el paciente agenda una nueva cita con su medico asignado
    @FXML
    protected void onBookAppointment() {
        if (loggedInUser.getAssignedDoctorId() == null || loggedInUser.getAssignedDoctorId().isEmpty()) {
            lblStatus.setText("No tienes un médico asignado.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final LocalDate date = dpDate.getValue();
        final String time = comboTime.getValue();
        final String reason = txtReason.getText() == null ? "" : txtReason.getText().trim();

        if (date == null || time == null || reason.isEmpty()) {
            lblStatus.setText("Fecha, hora y motivo son obligatorios.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }
        // no se permite agendar en una fecha pasada
        if (date.isBefore(LocalDate.now())) {
            lblStatus.setText("La fecha de la cita no puede ser en el pasado.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        lblStatus.setText("Agendando cita...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                Appointment appt = new Appointment();
                appt.setId(appointmentDao.createDocumentId());
                appt.setPatientId(loggedInUser.getUid());
                appt.setPatientName(loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
                appt.setDoctorId(loggedInUser.getAssignedDoctorId());
                appt.setDoctorName(loggedInUser.getAssignedDoctorName());
                appt.setDate(date.toString());
                appt.setTime(time);
                appt.setReason(reason);
                appt.setStatus("Pendiente");
                appt.setNotes("");

                appointmentDao.save(appt.getId(), appt);

                Platform.runLater(() -> {
                    dpDate.setValue(null);
                    comboTime.setValue(null);
                    txtReason.clear();
                    loadAppointmentsByField("patientId", loggedInUser.getUid());
                    lblStatus.setText("Cita agendada correctamente (Pendiente de confirmación).");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al agendar la cita.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // el medico o admin confirma la cita seleccionada
    @FXML
    protected void onConfirmAppointment() {
        changeStatus("Confirmada", "Pendiente");
    }

    // el medico o admin marca la cita como completada
    @FXML
    protected void onCompleteAppointment() {
        changeStatus("Completada", null);
    }

    // cancela la cita seleccionada disponible para todos los roles
    @FXML
    protected void onCancelAppointment() {
        if (selectedAppointment == null) {
            lblStatus.setText("Selecciona una cita de la tabla.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }
        if ("Cancelada".equals(selectedAppointment.getStatus())
                || "Completada".equals(selectedAppointment.getStatus())) {
            lblStatus.setText("Esta cita ya está " + selectedAppointment.getStatus().toLowerCase() + ".");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar cancelación");
        confirm.setHeaderText("Cancelar cita");
        confirm.setContentText("¿Cancelar la cita del " + selectedAppointment.getDate()
                + " a las " + selectedAppointment.getTime() + "?");
        DialogUtils.applyWhiteStyle(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                changeStatus("Cancelada", null);
            }
        });
    }

    // cambia el estado de la cita seleccionada
    // requiredCurrent limita desde que estado se permite hacer el cambio y null significa cualquiera
    private void changeStatus(String newStatus, String requiredCurrent) {
        if (selectedAppointment == null) {
            lblStatus.setText("Selecciona una cita de la tabla.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }
        if (requiredCurrent != null && !requiredCurrent.equals(selectedAppointment.getStatus())) {
            lblStatus.setText("Solo se puede " + newStatus.toLowerCase()
                    + " una cita en estado " + requiredCurrent + ".");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final Appointment target = selectedAppointment;
        lblStatus.setText("Actualizando estado...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                target.setStatus(newStatus);
                appointmentDao.save(target.getId(), target);
                Platform.runLater(() -> {
                    tableAppointments.refresh();
                    lblStatus.setText("Cita marcada como " + newStatus + ".");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al actualizar la cita.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }
}
