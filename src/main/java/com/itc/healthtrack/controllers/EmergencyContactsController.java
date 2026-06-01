package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.EmergencyContact;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.utils.DialogUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

// el paciente agrega edita y elimina sus contactos de emergencia
// los contactos viven en la subcoleccion users/{uid}/emergencyContacts del documento del paciente
public class EmergencyContactsController {

    @FXML private Label lblFormTitle;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtRelationship;
    @FXML private TextField txtEmail;
    @FXML private CheckBox chkPrimary;
    @FXML private Button btnSave;

    @FXML private TableView<EmergencyContact> tableContacts;
    @FXML private TableColumn<EmergencyContact, String> colName;
    @FXML private TableColumn<EmergencyContact, String> colPhone;
    @FXML private TableColumn<EmergencyContact, String> colRelationship;
    @FXML private TableColumn<EmergencyContact, String> colEmail;
    @FXML private TableColumn<EmergencyContact, String> colPrimary;

    @FXML private Label lblStatus;

    private final ObservableList<EmergencyContact> contactsObservableList = FXCollections.observableArrayList();

    private User loggedInUser;
    // dao apuntado a la subcoleccion del paciente se arma en initData con el uid
    private GenericDAO<EmergencyContact> contactDao;
    private EmergencyContact selectedContact = null;

    // arranca el controlador con el paciente logeado y carga sus contactos
    public void initData(User user) {
        this.loggedInUser = user;
        this.contactDao = new GenericDAO<>(EmergencyContact.class, "users/" + user.getUid() + "/emergencyContacts");
        setupTable();
        loadContacts();
    }

    // configura las columnas y el listener de seleccion de la tabla
    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRelationship.setCellValueFactory(new PropertyValueFactory<>("relationship"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        // columna principal muestra Si o No segun la bandera del contacto
        colPrimary.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().isPrimary() ? "Sí" : "No"));
        tableContacts.setItems(contactsObservableList);

        // al seleccionar una fila llenamos el formulario para editar
        tableContacts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedContact = newVal;
                txtName.setText(newVal.getName());
                txtPhone.setText(newVal.getPhone());
                txtRelationship.setText(newVal.getRelationship());
                txtEmail.setText(newVal.getEmail());
                chkPrimary.setSelected(newVal.isPrimary());
                lblFormTitle.setText("Editar Contacto");
                btnSave.setText("Actualizar");
            }
        });
    }

    // carga los contactos del paciente en un hilo de fondo
    private void loadContacts() {
        new Thread(() -> {
            try {
                List<EmergencyContact> all = contactDao.getAll();
                Platform.runLater(() -> {
                    contactsObservableList.clear();
                    contactsObservableList.addAll(all);
                    lblStatus.setText(all.size() + " contacto(s) registrado(s).");
                    lblStatus.setTextFill(Color.web("#aaaaaa"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar los contactos de emergencia.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // agrega un contacto nuevo o actualiza el seleccionado
    @FXML
    protected void onSaveContact() {
        final String name = txtName.getText() == null ? "" : txtName.getText().trim();
        final String phone = txtPhone.getText() == null ? "" : txtPhone.getText().trim();
        final String relationship = txtRelationship.getText() == null ? "" : txtRelationship.getText().trim();
        final String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        final boolean primary = chkPrimary.isSelected();

        if (name.isEmpty() || phone.isEmpty()) {
            lblStatus.setText("El nombre y el teléfono son obligatorios.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }
        // si se marca como principal pero no tiene correo avisamos que no podra recibir alertas
        if (primary && email.isEmpty()) {
            lblStatus.setText("Un contacto principal necesita correo para recibir las alertas críticas.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final boolean isNew = (selectedContact == null);
        lblStatus.setText(isNew ? "Guardando contacto..." : "Actualizando contacto...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                EmergencyContact contact = isNew ? new EmergencyContact() : selectedContact;
                if (isNew) {
                    contact.setId(contactDao.createDocumentId());
                    contact.setPatientId(loggedInUser.getUid());
                }
                contact.setName(name);
                contact.setPhone(phone);
                contact.setRelationship(relationship);
                contact.setEmail(email);
                contact.setPrimary(primary);

                // si este contacto sera el principal quitamos la marca a los demas primero
                if (primary) {
                    clearOtherPrimaries(contact.getId());
                }

                contactDao.save(contact.getId(), contact);

                Platform.runLater(() -> {
                    loadContacts();
                    onClearForm();
                    lblStatus.setText(isNew ? "Contacto agregado correctamente." : "Contacto actualizado correctamente.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al guardar el contacto.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // marca el contacto seleccionado como principal y desmarca los demas
    @FXML
    protected void onSetPrimary() {
        if (selectedContact == null) {
            lblStatus.setText("Selecciona un contacto de la tabla.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }
        if (selectedContact.getEmail() == null || selectedContact.getEmail().isBlank()) {
            lblStatus.setText("Este contacto no tiene correo; agrégalo antes de marcarlo como principal.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final EmergencyContact target = selectedContact;
        new Thread(() -> {
            try {
                clearOtherPrimaries(target.getId());
                target.setPrimary(true);
                contactDao.save(target.getId(), target);
                Platform.runLater(() -> {
                    loadContacts();
                    onClearForm();
                    lblStatus.setText("Contacto principal actualizado.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al actualizar el contacto principal.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // quita la marca de principal a todos los contactos menos al indicado corre en hilo de fondo
    private void clearOtherPrimaries(String keepId) throws Exception {
        List<EmergencyContact> all = contactDao.getAll();
        for (EmergencyContact c : all) {
            if (c.isPrimary() && !c.getId().equals(keepId)) {
                c.setPrimary(false);
                contactDao.save(c.getId(), c);
            }
        }
    }

    // elimina el contacto seleccionado previa confirmacion
    @FXML
    protected void onDeleteContact() {
        if (selectedContact == null) {
            lblStatus.setText("Selecciona un contacto de la tabla para eliminar.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final String id = selectedContact.getId();
        final String name = selectedContact.getName();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar contacto");
        confirm.setContentText("¿Eliminar a \"" + name + "\" de tus contactos de emergencia?");
        DialogUtils.applyWhiteStyle(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            lblStatus.setText("Eliminando...");
            lblStatus.setTextFill(Color.web("#ffffff"));
            new Thread(() -> {
                try {
                    contactDao.delete(id);
                    Platform.runLater(() -> {
                        loadContacts();
                        onClearForm();
                        lblStatus.setText("Contacto eliminado correctamente.");
                        lblStatus.setTextFill(Color.web("#4caf50"));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error al eliminar el contacto.");
                        lblStatus.setTextFill(Color.web("#ff5252"));
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    // limpia el formulario y deselecciona el contacto actual
    @FXML
    protected void onClearForm() {
        txtName.clear();
        txtPhone.clear();
        txtRelationship.clear();
        txtEmail.clear();
        chkPrimary.setSelected(false);
        selectedContact = null;
        tableContacts.getSelectionModel().clearSelection();
        lblFormTitle.setText("Nuevo Contacto");
        btnSave.setText("Agregar");
    }
}
