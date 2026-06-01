package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Medicine;
import com.itc.healthtrack.models.Treatment;
import com.itc.healthtrack.models.TreatmentDetail;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.services.UserService;
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

// tratamientos y recetas del paciente
// medico crea tratamientos y les agrega medicamentos con dosis y frecuencia
// paciente ve solo lectura de sus propios tratamientos
// admin ve solo lectura y puede ver cualquier paciente
// coleccion top-level treatments y medicamentos en subcol treatments/{id}/details
public class TreatmentsController {

    @FXML private ComboBox<User> comboPatients;

    @FXML private TableView<Treatment>           tableTreatments;
    @FXML private TableColumn<Treatment, String> colDiagnosis;
    @FXML private TableColumn<Treatment, String> colStartDate;
    @FXML private TableColumn<Treatment, String> colEndDate;
    @FXML private TableColumn<Treatment, String> colStatus;

    @FXML private VBox      formSection;
    @FXML private TextField txtDiagnosis;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Button    btnSaveTreatment;
    @FXML private Button    btnCloseTreatment;
    @FXML private Button    btnDeleteTreatment;

    @FXML private TableView<TreatmentDetail>           tableDetails;
    @FXML private TableColumn<TreatmentDetail, String> colMedicine;
    @FXML private TableColumn<TreatmentDetail, String> colDose;
    @FXML private TableColumn<TreatmentDetail, String> colFrequency;

    @FXML private VBox               detailFormSection;
    @FXML private ComboBox<Medicine> comboMedicine;
    @FXML private TextField          txtDose;
    @FXML private TextField          txtFrequency;
    @FXML private Button             btnAddDetail;
    @FXML private Button             btnDeleteDetail;

    @FXML private Label lblStatus;

    private User loggedInUser;
    private UserService userService;
    private GenericDAO<Treatment> treatmentDao;

    private final ObservableList<Treatment>       treatmentList = FXCollections.observableArrayList();
    private final ObservableList<TreatmentDetail> detailList    = FXCollections.observableArrayList();

    // tratamiento seleccionado actualmente en la tabla
    private Treatment selectedTreatment;
    // uid del paciente cuya lista de tratamientos esta cargada
    private String currentPatientId;

    // arranca el controlador y configura la vista segun el rol del usuario
    public void initData(User user) {
        this.loggedInUser = user;
        this.userService  = new UserService();
        this.treatmentDao = new GenericDAO<>(Treatment.class, "treatments");

        setupTreatmentTable();
        setupDetailTable();

        String role = user.getRole() != null ? user.getRole() : "patient";
        switch (role) {
            case "doctor":  setupDoctorView();  break;
            case "admin":   setupAdminView();   break;
            default:        setupPatientView(); break;
        }
    }

    // configura columnas y listener de seleccion de la tabla de tratamientos
    private void setupTreatmentTable() {
        colDiagnosis.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        // la columna estado deriva su valor del campo boolean closed
        colStatus.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().isClosed() ? "Cerrado" : "Activo"));

        tableTreatments.setItems(treatmentList);
        tableTreatments.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> onTreatmentSelected(newVal));
    }

    // configura columnas de la tabla de medicamentos del tratamiento
    private void setupDetailTable() {
        colMedicine.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colDose.setCellValueFactory(new PropertyValueFactory<>("dose"));
        colFrequency.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        tableDetails.setItems(detailList);
    }

    // medico puede crear editar cerrar y eliminar tratamientos y sus medicamentos
    private void setupDoctorView() {
        comboPatients.setVisible(true);
        comboPatients.setManaged(true);
        formSection.setVisible(true);
        formSection.setManaged(true);
        detailFormSection.setVisible(true);
        detailFormSection.setManaged(true);

        loadPatientsIntoCombo(true);
        // cargamos el catalogo de medicamentos activos una sola vez al abrir la vista
        loadMedicines();

        comboPatients.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                clearTreatmentForm();
                detailList.clear();
                selectPatient(newVal.getUid());
            }
        });
    }

    // carga los medicamentos activos del catalogo en el combo del formulario de detalles
    private void loadMedicines() {
        new Thread(() -> {
            try {
                GenericDAO<Medicine> medDao = new GenericDAO<>(Medicine.class, "medicines");
                List<Medicine> activos = medDao.getByField("active", true);
                activos.sort(Comparator.comparing(Medicine::getGenericName,
                        String.CASE_INSENSITIVE_ORDER));
                Platform.runLater(() ->
                    comboMedicine.setItems(FXCollections.observableArrayList(activos)));
            } catch (Exception e) {
                // si falla la carga del catalogo el combo queda vacio pero no bloquea la app
                e.printStackTrace();
            }
        }).start();
    }

    // admin puede ver tratamientos de cualquier paciente pero no puede modificar
    private void setupAdminView() {
        comboPatients.setVisible(true);
        comboPatients.setManaged(true);
        formSection.setVisible(false);
        formSection.setManaged(false);
        detailFormSection.setVisible(false);
        detailFormSection.setManaged(false);

        // reutilizamos el mismo metodo que el medico ya que UserService maneja el rol admin
        loadPatientsIntoCombo(false);

        comboPatients.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                detailList.clear();
                selectPatient(newVal.getUid());
            }
        });
    }

    // paciente ve solo sus propios tratamientos sin posibilidad de edicion
    private void setupPatientView() {
        comboPatients.setVisible(false);
        comboPatients.setManaged(false);
        formSection.setVisible(false);
        formSection.setManaged(false);
        detailFormSection.setVisible(false);
        detailFormSection.setManaged(false);

        selectPatient(loggedInUser.getUid());
    }

    // carga los pacientes visibles a traves de UserService que ya maneja medico y admin
    // autoSelect preselecciona el primero de la lista solo para la vista del medico
    private void loadPatientsIntoCombo(boolean autoSelect) {
        new Thread(() -> {
            try {
                List<User> patients = userService.getPatientsForUser(loggedInUser);
                patients.sort(Comparator.comparing(u -> u.getLastName() + u.getFirstName()));
                Platform.runLater(() -> {
                    comboPatients.setItems(FXCollections.observableArrayList(patients));
                    if (autoSelect && !patients.isEmpty()) {
                        comboPatients.getSelectionModel().selectFirst();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al cargar pacientes", true));
            }
        }).start();
    }

    // descarga los tratamientos del paciente desde firestore
    private void selectPatient(String patientId) {
        this.currentPatientId = patientId;
        loadTreatments(patientId);
    }

    // carga los tratamientos del paciente ordenados con los mas recientes primero
    private void loadTreatments(String patientId) {
        new Thread(() -> {
            try {
                List<Treatment> list = treatmentDao.getByField("patientId", patientId);
                list.sort(Comparator.comparing(Treatment::getStartDate).reversed());
                Platform.runLater(() -> {
                    treatmentList.setAll(list);
                    lblStatus.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al cargar tratamientos", true));
            }
        }).start();
    }

    // cuando el usuario hace clic en una fila rellena el formulario y carga los medicamentos
    private void onTreatmentSelected(Treatment t) {
        selectedTreatment = t;
        detailList.clear();
        if (t == null) {
            clearTreatmentForm();
            return;
        }

        txtDiagnosis.setText(t.getDiagnosis());
        dpStartDate.setValue(parseDate(t.getStartDate()));
        dpEndDate.setValue(t.getEndDate() != null && !t.getEndDate().isEmpty()
                ? parseDate(t.getEndDate()) : null);

        // los controles de edicion se deshabilitan si el tratamiento ya fue cerrado
        boolean esDoctor = "doctor".equals(loggedInUser.getRole());
        boolean editable = !t.isClosed() && esDoctor;
        txtDiagnosis.setEditable(editable);
        dpStartDate.setDisable(!editable);
        dpEndDate.setDisable(!editable);
        btnSaveTreatment.setDisable(!editable);
        btnCloseTreatment.setDisable(t.isClosed() || !esDoctor);
        btnDeleteTreatment.setDisable(!esDoctor);
        btnAddDetail.setDisable(!editable);
        btnDeleteDetail.setDisable(!editable);

        loadDetails(t.getId());
    }

    // descarga los medicamentos del tratamiento desde su subcoleccion
    private void loadDetails(String treatmentId) {
        new Thread(() -> {
            try {
                GenericDAO<TreatmentDetail> dDao = detailDaoFor(treatmentId);
                List<TreatmentDetail> list = dDao.getAll();
                Platform.runLater(() -> detailList.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al cargar medicamentos", true));
            }
        }).start();
    }

    // guarda un tratamiento nuevo o actualiza el seleccionado
    @FXML
    private void onSaveTreatment() {
        String diagnosis = txtDiagnosis.getText().trim();
        LocalDate startDate = dpStartDate.getValue();

        if (diagnosis.isEmpty() || startDate == null) {
            setStatus("El diagnóstico y la fecha de inicio son obligatorios", true);
            return;
        }

        String startStr = startDate.toString();
        String endStr   = dpEndDate.getValue() != null ? dpEndDate.getValue().toString() : "";

        if (!endStr.isEmpty() && dpEndDate.getValue().isBefore(startDate)) {
            setStatus("La fecha de fin no puede ser anterior a la de inicio", true);
            return;
        }

        String id         = selectedTreatment != null
                            ? selectedTreatment.getId()
                            : treatmentDao.createDocumentId();
        boolean wasClosed = selectedTreatment != null && selectedTreatment.isClosed();

        // nombre del paciente para desnormalizar en el documento
        User comboUser    = comboPatients.isVisible() ? comboPatients.getValue() : null;
        String patientId  = currentPatientId != null ? currentPatientId : loggedInUser.getUid();
        String patientName = comboUser != null
                             ? comboUser.getFirstName() + " " + comboUser.getLastName()
                             : loggedInUser.getFirstName() + " " + loggedInUser.getLastName();
        String doctorName = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();

        Treatment t = new Treatment(id, patientId, patientName,
                loggedInUser.getUid(), doctorName,
                diagnosis, startStr, endStr, wasClosed);

        new Thread(() -> {
            try {
                treatmentDao.save(id, t);
                Platform.runLater(() -> {
                    setStatus("Tratamiento guardado correctamente", false);
                    clearTreatmentForm();
                    loadTreatments(patientId);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al guardar: " + e.getMessage(), true));
            }
        }).start();
    }

    // cierra el tratamiento seleccionado y lo deja en solo lectura para siempre
    @FXML
    private void onCloseTreatment() {
        if (selectedTreatment == null || selectedTreatment.isClosed()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Cerrar el tratamiento \"" + selectedTreatment.getDiagnosis() + "\"?\n"
                        + "Esta acción no se puede deshacer.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Cerrar tratamiento");
        DialogUtils.applyWhiteStyle(confirm.getDialogPane());
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) doCloseTreatment();
        });
    }

    // persiste el cierre del tratamiento en firestore
    private void doCloseTreatment() {
        selectedTreatment.setClosed(true);
        String id    = selectedTreatment.getId();
        String patId = currentPatientId;
        new Thread(() -> {
            try {
                treatmentDao.save(id, selectedTreatment);
                Platform.runLater(() -> {
                    setStatus("Tratamiento cerrado", false);
                    clearTreatmentForm();
                    loadTreatments(patId);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al cerrar: " + e.getMessage(), true));
            }
        }).start();
    }

    // elimina el tratamiento seleccionado y todos sus medicamentos de la subcoleccion
    @FXML
    private void onDeleteTreatment() {
        if (selectedTreatment == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el tratamiento \"" + selectedTreatment.getDiagnosis()
                        + "\" y todos sus medicamentos?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Eliminar tratamiento");
        DialogUtils.applyWhiteStyle(confirm.getDialogPane());
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) doDeleteTreatment();
        });
    }

    // borra primero los medicamentos de la subcoleccion y luego el tratamiento raiz
    private void doDeleteTreatment() {
        String treatId = selectedTreatment.getId();
        String patId   = currentPatientId;
        new Thread(() -> {
            try {
                GenericDAO<TreatmentDetail> dDao = detailDaoFor(treatId);
                for (TreatmentDetail d : dDao.getAll()) {
                    dDao.delete(d.getId());
                }
                treatmentDao.delete(treatId);
                Platform.runLater(() -> {
                    setStatus("Tratamiento eliminado", false);
                    detailList.clear();
                    selectedTreatment = null;
                    clearTreatmentForm();
                    loadTreatments(patId);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al eliminar: " + e.getMessage(), true));
            }
        }).start();
    }

    // agrega un nuevo medicamento al tratamiento seleccionado
    @FXML
    private void onAddDetail() {
        if (selectedTreatment == null) {
            setStatus("Selecciona primero un tratamiento", true);
            return;
        }
        if (selectedTreatment.isClosed()) {
            setStatus("No se pueden agregar medicamentos a un tratamiento cerrado", true);
            return;
        }

        Medicine medicine = comboMedicine.getValue();
        String dose = txtDose.getText().trim();
        String freq = txtFrequency.getText().trim();

        if (medicine == null) {
            setStatus("Selecciona un medicamento del catálogo", true);
            return;
        }

        GenericDAO<TreatmentDetail> dDao = detailDaoFor(selectedTreatment.getId());
        String detailId = dDao.createDocumentId();
        // guardamos id y nombre para mantener el historial aunque el medicamento sea desactivado
        TreatmentDetail detail = new TreatmentDetail(
                detailId, selectedTreatment.getId(),
                medicine.getId(), medicine.toString(), dose, freq);

        new Thread(() -> {
            try {
                dDao.save(detailId, detail);
                Platform.runLater(() -> {
                    setStatus("Medicamento agregado", false);
                    clearDetailForm();
                    loadDetails(selectedTreatment.getId());
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al agregar: " + e.getMessage(), true));
            }
        }).start();
    }

    // elimina el medicamento seleccionado en la tabla de detalles
    @FXML
    private void onDeleteDetail() {
        TreatmentDetail sel = tableDetails.getSelectionModel().getSelectedItem();
        if (sel == null) {
            setStatus("Selecciona un medicamento para eliminar", true);
            return;
        }
        if (selectedTreatment != null && selectedTreatment.isClosed()) {
            setStatus("No se pueden modificar medicamentos de un tratamiento cerrado", true);
            return;
        }

        GenericDAO<TreatmentDetail> dDao = detailDaoFor(selectedTreatment.getId());
        new Thread(() -> {
            try {
                dDao.delete(sel.getId());
                Platform.runLater(() -> {
                    setStatus("Medicamento eliminado", false);
                    loadDetails(selectedTreatment.getId());
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al eliminar: " + e.getMessage(), true));
            }
        }).start();
    }

    // construye el dao para la subcoleccion de detalles del tratamiento dado
    private GenericDAO<TreatmentDetail> detailDaoFor(String treatmentId) {
        return new GenericDAO<>(TreatmentDetail.class,
                "treatments/" + treatmentId + "/details");
    }

    // limpia el formulario de tratamiento y deshabilita los botones de accion
    private void clearTreatmentForm() {
        txtDiagnosis.clear();
        dpStartDate.setValue(null);
        dpEndDate.setValue(null);
        txtDiagnosis.setEditable(true);
        dpStartDate.setDisable(false);
        dpEndDate.setDisable(false);
        btnSaveTreatment.setDisable(false);
        btnCloseTreatment.setDisable(true);
        btnDeleteTreatment.setDisable(true);
        tableTreatments.getSelectionModel().clearSelection();
        selectedTreatment = null;
    }

    // limpia el formulario de medicamento
    private void clearDetailForm() {
        comboMedicine.setValue(null);
        txtDose.clear();
        txtFrequency.clear();
    }

    // convierte un string yyyy-MM-dd en LocalDate y regresa null si falla
    private LocalDate parseDate(String d) {
        try { return LocalDate.parse(d); } catch (Exception e) { return null; }
    }

    private void setStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(error ? Color.ORANGERED : Color.LIGHTGREEN);
    }
}
