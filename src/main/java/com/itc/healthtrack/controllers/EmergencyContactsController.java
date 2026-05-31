package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.EmergencyContact;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

// controlador CRUD para los contactos de emergencia del paciente
public class EmergencyContactsController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtRelationship;
    @FXML private Label lblStatus;

    @FXML private TableView<EmergencyContact> tableContacts;
    @FXML private TableColumn<EmergencyContact, String> colFullName;
    @FXML private TableColumn<EmergencyContact, String> colPhone;
    @FXML private TableColumn<EmergencyContact, String> colRelationship;

    private final ObservableList<EmergencyContact> contactsObservableList = FXCollections.observableArrayList();

    private User loggedInPatient;
    private EmergencyContact selectedContact;
    private GenericDAO<EmergencyContact> contactDao;

    public void initData(User patient) {
        this.loggedInPatient = patient;
        if (patient != null && patient.getUid() != null) {
            this.contactDao = new GenericDAO<>(EmergencyContact.class,
                    "users/" + patient.getUid() + "/emergencyContacts");
        }
        setupTable();
        loadContacts();
    }

    private void setupTable() {
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRelationship.setCellValueFactory(new PropertyValueFactory<>("relationship"));

        tableContacts.setItems(contactsObservableList);
        tableContacts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedContact = newVal;
                fillForm(newVal);
            }
        });
    }

    private void loadContacts() {
        if (contactDao == null) return;
        new Thread(() -> {
            try {
                List<EmergencyContact> contacts = contactDao.getAll();
                Platform.runLater(() -> {
                    contactsObservableList.clear();
                    contactsObservableList.addAll(contacts);
                });
            } catch (Exception e) {
                System.err.println("[EmergencyContactsController] Error al cargar contactos: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    protected void onSaveContact() {
        if (contactDao == null) {
            lblStatus.setText("No se pudo cargar el paciente.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        String fullName = txtFullName.getText().trim();
        String phone = txtPhone.getText().trim();
        String relationship = txtRelationship.getText().trim();

        if (fullName.isEmpty() || phone.isEmpty() || relationship.isEmpty()) {
            lblStatus.setText("Completa nombre, teléfono y parentesco.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        boolean isNew = (selectedContact == null);
        EmergencyContact contact = isNew ? new EmergencyContact() : selectedContact;
        contact.setFullName(fullName);
        contact.setPhone(phone);
        contact.setRelationship(relationship);

        lblStatus.setText(isNew ? "Guardando..." : "Actualizando...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                if (isNew) {
                    String newId = contactDao.createDocumentId();
                    contact.setId(newId);
                    contactDao.save(newId, contact);
                } else {
                    contactDao.save(contact.getId(), contact);
                }

                Platform.runLater(() -> {
                    onClearForm();
                    loadContacts();
                    lblStatus.setText(isNew ? "Contacto guardado." : "Contacto actualizado.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al guardar el contacto.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                System.err.println("[EmergencyContactsController] Error al guardar contacto: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    protected void onDeleteContact() {
        if (selectedContact == null || contactDao == null) {
            lblStatus.setText("Selecciona un contacto para eliminar.");
            lblStatus.setTextFill(Color.web("#ff5252"));
            return;
        }

        String contactId = selectedContact.getId();
        lblStatus.setText("Eliminando...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                contactDao.delete(contactId);
                Platform.runLater(() -> {
                    onClearForm();
                    loadContacts();
                    lblStatus.setText("Contacto eliminado.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al eliminar el contacto.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                System.err.println("[EmergencyContactsController] Error al eliminar contacto: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    protected void onClearForm() {
        txtFullName.clear();
        txtPhone.clear();
        txtRelationship.clear();
        selectedContact = null;
        tableContacts.getSelectionModel().clearSelection();
    }

    private void fillForm(EmergencyContact contact) {
        txtFullName.setText(contact.getFullName());
        txtPhone.setText(contact.getPhone());
        txtRelationship.setText(contact.getRelationship());
    }
}
