package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Specialty;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

// permite al admin crear y eliminar especialidades guardadas en la coleccion "specialties"
public class SpecialtiesController {

    @FXML private TextField txtName;
    @FXML private TextField txtDescription;

    @FXML private TableView<Specialty> tableSpecialties;
    @FXML private TableColumn<Specialty, String> colName;
    @FXML private TableColumn<Specialty, String> colDescription;

    @FXML private Label lblStatus;

    // unico dao para las operaciones sobre la coleccion de especialidades
    private final GenericDAO<Specialty> specialtyDao = new GenericDAO<>(Specialty.class, "specialties");
    private final ObservableList<Specialty> specialtiesObservableList = FXCollections.observableArrayList();

    private User loggedInAdmin;
    private Specialty selectedSpecialty = null;

    // arranca el controlador con el admin logeado y carga el catalogo
    public void initData(User admin) {
        this.loggedInAdmin = admin;
        setupTable();
        loadAllSpecialties();
    }

    // configura las columnas y el listener de seleccion de la tabla
    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        tableSpecialties.setItems(specialtiesObservableList);

        // guardamos la seleccion cuando el admin clickea una fila
        tableSpecialties.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedSpecialty = newVal;
                lblStatus.setText("Especialidad seleccionada: " + newVal.getName());
                lblStatus.setTextFill(Color.web("#aaaaaa"));
            }
        });
    }

    // carga todas las especialidades en un hilo de fondo y refresca la tabla en el hilo fx
    private void loadAllSpecialties() {
        new Thread(() -> {
            try {
                List<Specialty> all = specialtyDao.getAll();
                Platform.runLater(() -> {
                    specialtiesObservableList.clear();
                    specialtiesObservableList.addAll(all);
                    lblStatus.setText(all.size() + " especialidad(es) en el catálogo.");
                    lblStatus.setTextFill(Color.web("#aaaaaa"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar el catálogo de especialidades.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // crea una nueva especialidad validando que el nombre no este vacio ni repetido
    @FXML
    protected void onCreateSpecialty() {
        final String name = txtName.getText() == null ? "" : txtName.getText().trim();
        final String description = txtDescription.getText() == null ? "" : txtDescription.getText().trim();

        if (name.isEmpty()) {
            lblStatus.setText("El nombre de la especialidad es obligatorio.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        // no permitimos nombres duplicados (ignorando mayusculas) en la lista local
        boolean duplicate = specialtiesObservableList.stream()
                .anyMatch(s -> s.getName() != null && s.getName().equalsIgnoreCase(name));
        if (duplicate) {
            lblStatus.setText("Ya existe una especialidad con ese nombre.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        lblStatus.setText("Guardando especialidad...");
        lblStatus.setTextFill(Color.web("#ffffff"));

        new Thread(() -> {
            try {
                // generamos un id nuevo y lo guardamos tambien como campo del documento
                String id = specialtyDao.createDocumentId();
                Specialty specialty = new Specialty(id, name, description);
                specialtyDao.save(id, specialty);

                Platform.runLater(() -> {
                    // refrescamos la lista local sin recargar firestore
                    specialtiesObservableList.add(specialty);
                    txtName.clear();
                    txtDescription.clear();
                    lblStatus.setText("Especialidad \"" + name + "\" agregada correctamente.");
                    lblStatus.setTextFill(Color.web("#4caf50"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al guardar la especialidad.");
                    lblStatus.setTextFill(Color.web("#ff5252"));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // elimina la especialidad seleccionada previa confirmacion
    @FXML
    protected void onDeleteSpecialty() {
        if (selectedSpecialty == null) {
            lblStatus.setText("Selecciona una especialidad de la tabla para eliminar.");
            lblStatus.setTextFill(Color.web("#ff9800"));
            return;
        }

        final String id = selectedSpecialty.getId();
        final String name = selectedSpecialty.getName();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("Eliminar especialidad");
        confirm.setContentText("¿Eliminar la especialidad \"" + name + "\"?\n\nEsta acción no se puede deshacer.");
        applyWhiteStyle(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            lblStatus.setText("Eliminando...");
            lblStatus.setTextFill(Color.web("#ffffff"));
            new Thread(() -> {
                try {
                    specialtyDao.delete(id);
                    Platform.runLater(() -> {
                        // quitamos la especialidad de la lista local sin recargar firestore
                        specialtiesObservableList.removeIf(s -> id.equals(s.getId()));
                        selectedSpecialty = null;
                        tableSpecialties.getSelectionModel().clearSelection();
                        lblStatus.setText("Especialidad eliminada correctamente.");
                        lblStatus.setTextFill(Color.web("#4caf50"));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblStatus.setText("Error al eliminar la especialidad.");
                        lblStatus.setTextFill(Color.web("#ff5252"));
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    // aplica el estilo blanco al panel del dialogo (mismo criterio que el resto de la app)
    private static void applyWhiteStyle(DialogPane dp) {
        dp.setStyle("-fx-background-color: #ffffff; -fx-font-size: 13px;");

        javafx.scene.Node content = dp.lookup(".content.label");
        if (content != null) {
            content.setStyle("-fx-text-fill: #222222; -fx-font-size: 13px;");
        }

        javafx.scene.Node header = dp.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: #f5f5f5;");
        }

        javafx.scene.Node headerLabel = dp.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
        }

        for (ButtonType bt : dp.getButtonTypes()) {
            javafx.scene.Node node = dp.lookupButton(bt);
            if (node instanceof Button) {
                Button btn = (Button) node;
                boolean isCancel = (bt == ButtonType.CANCEL
                        || bt == ButtonType.NO
                        || bt == ButtonType.CLOSE);
                String color = isCancel ? "#9e9e9e" : "#2196f3";
                btn.setStyle("-fx-background-color: " + color
                        + "; -fx-text-fill: #ffffff; -fx-cursor: hand;"
                        + " -fx-padding: 6 22; -fx-background-radius: 4;");
            }
        }
    }
}
