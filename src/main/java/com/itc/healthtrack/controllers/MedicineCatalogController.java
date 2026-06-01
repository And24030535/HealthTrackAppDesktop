package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.Medicine;
import com.itc.healthtrack.models.User;
import com.itc.healthtrack.utils.DialogUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.Comparator;
import java.util.List;

// catalogo de medicamentos disponible solo para el admin
// el admin agrega medicamentos y puede desactivarlos o reactivarlos
// los medicamentos nunca se eliminan para preservar el historial de tratamientos
public class MedicineCatalogController {

    @FXML private TextField txtGenericName;
    @FXML private TextField txtCommercialName;
    @FXML private TextField txtManufacturer;

    @FXML private TableView<Medicine>            tableMedicines;
    @FXML private TableColumn<Medicine, String>  colGenericName;
    @FXML private TableColumn<Medicine, String>  colCommercialName;
    @FXML private TableColumn<Medicine, String>  colManufacturer;
    @FXML private TableColumn<Medicine, String>  colActive;

    @FXML private Label lblStatus;

    private final GenericDAO<Medicine> medicineDao = new GenericDAO<>(Medicine.class, "medicines");
    private final ObservableList<Medicine> medicineList = FXCollections.observableArrayList();

    private Medicine selectedMedicine;

    public void initData(User admin) {
        setupTable();
        loadAllMedicines();
    }

    // configura columnas y listener de seleccion de la tabla
    private void setupTable() {
        colGenericName.setCellValueFactory(new PropertyValueFactory<>("genericName"));
        colCommercialName.setCellValueFactory(new PropertyValueFactory<>("commercialName"));
        colManufacturer.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        // columna estado derivada del campo active boolean
        colActive.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().isActive() ? "Activo" : "Inactivo"));

        // coloreamos la fila segun el estado activo o inactivo del medicamento
        tableMedicines.setRowFactory(tv -> new TableRow<Medicine>() {
            @Override
            protected void updateItem(Medicine item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (!item.isActive()) {
                    setStyle("-fx-background-color: #3a2a2a;");
                } else {
                    setStyle("");
                }
            }
        });

        tableMedicines.setItems(medicineList);
        tableMedicines.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, newVal) -> {
                selectedMedicine = newVal;
                if (newVal != null) {
                    // rellena el formulario con el medicamento seleccionado para facilitar edicion
                    txtGenericName.setText(newVal.getGenericName());
                    txtCommercialName.setText(newVal.getCommercialName() != null
                            ? newVal.getCommercialName() : "");
                    txtManufacturer.setText(newVal.getManufacturer() != null
                            ? newVal.getManufacturer() : "");
                    setStatus("Seleccionado: " + newVal.getGenericName(), false);
                }
            });
    }

    // carga todos los medicamentos del catalogo activos e inactivos
    private void loadAllMedicines() {
        new Thread(() -> {
            try {
                List<Medicine> all = medicineDao.getAll();
                all.sort(Comparator.comparing(Medicine::getGenericName, String.CASE_INSENSITIVE_ORDER));
                Platform.runLater(() -> {
                    medicineList.setAll(all);
                    setStatus(all.size() + " medicamento(s) en el catálogo.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al cargar el catálogo.", true));
                e.printStackTrace();
            }
        }).start();
    }

    // agrega un medicamento nuevo validando nombre generico obligatorio y sin duplicados
    @FXML
    private void onAddMedicine() {
        String generic      = txtGenericName.getText()    != null ? txtGenericName.getText().trim()    : "";
        String commercial   = txtCommercialName.getText() != null ? txtCommercialName.getText().trim() : "";
        String manufacturer = txtManufacturer.getText()   != null ? txtManufacturer.getText().trim()   : "";

        if (generic.isEmpty()) {
            setStatus("El nombre genérico es obligatorio.", true);
            return;
        }

        // no permitimos duplicados en nombre generico ignorando mayusculas
        boolean duplicate = medicineList.stream()
            .anyMatch(m -> m.getGenericName() != null
                       && m.getGenericName().equalsIgnoreCase(generic));
        if (duplicate) {
            setStatus("Ya existe un medicamento con ese nombre genérico.", true);
            return;
        }

        setStatus("Guardando medicamento...", false);
        String id = medicineDao.createDocumentId();
        Medicine m = new Medicine(id, generic, commercial, manufacturer, true);

        new Thread(() -> {
            try {
                medicineDao.save(id, m);
                Platform.runLater(() -> {
                    medicineList.add(m);
                    // reordenamos la lista tras insertar para mantener el orden alfabetico
                    medicineList.sort(Comparator.comparing(Medicine::getGenericName,
                            String.CASE_INSENSITIVE_ORDER));
                    clearForm();
                    setStatus("Medicamento \"" + generic + "\" agregado correctamente.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al guardar el medicamento.", true));
                e.printStackTrace();
            }
        }).start();
    }

    // marca el medicamento seleccionado como inactivo sin eliminarlo
    @FXML
    private void onDeactivateMedicine() {
        if (selectedMedicine == null) {
            setStatus("Selecciona un medicamento de la tabla.", true);
            return;
        }
        if (!selectedMedicine.isActive()) {
            setStatus("El medicamento ya está inactivo.", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Desactivar medicamento");
        confirm.setHeaderText("Desactivar \"" + selectedMedicine.getGenericName() + "\"");
        confirm.setContentText("El medicamento dejará de aparecer en las recetas nuevas.\n"
                + "Los tratamientos existentes no se verán afectados.");
        DialogUtils.applyWhiteStyle(confirm.getDialogPane());
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) changeActiveState(selectedMedicine, false);
        });
    }

    // reactiva un medicamento previamente desactivado
    @FXML
    private void onReactivateMedicine() {
        if (selectedMedicine == null) {
            setStatus("Selecciona un medicamento de la tabla.", true);
            return;
        }
        if (selectedMedicine.isActive()) {
            setStatus("El medicamento ya está activo.", true);
            return;
        }
        changeActiveState(selectedMedicine, true);
    }

    // persiste el cambio de estado activo o inactivo en Firestore con rollback visual si falla
    private void changeActiveState(Medicine medicine, boolean active) {
        medicine.setActive(active);
        new Thread(() -> {
            try {
                medicineDao.save(medicine.getId(), medicine);
                Platform.runLater(() -> {
                    // refrescamos la tabla sin recargar Firestore
                    tableMedicines.refresh();
                    String verb = active ? "reactivado" : "desactivado";
                    setStatus("\"" + medicine.getGenericName() + "\" " + verb + ".", false);
                });
            } catch (Exception e) {
                // revertimos el cambio en memoria si fallo la escritura
                medicine.setActive(!active);
                Platform.runLater(() -> setStatus("Error al actualizar el estado.", true));
                e.printStackTrace();
            }
        }).start();
    }

    private void clearForm() {
        txtGenericName.clear();
        txtCommercialName.clear();
        txtManufacturer.clear();
        tableMedicines.getSelectionModel().clearSelection();
        selectedMedicine = null;
    }

    private void setStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(error ? Color.ORANGERED : Color.web("#aaaaaa"));
    }
}
