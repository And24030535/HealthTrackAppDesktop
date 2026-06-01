package com.itc.healthtrack.controllers;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.AppNotification;
import com.itc.healthtrack.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.Comparator;
import java.util.List;

// panel de notificaciones del sistema
// muestra a cada usuario su historial con tipo mensaje y estado de lectura
// las notificaciones CRITICAL no pueden eliminarse solo marcarse como leidas
public class NotificationsController {

    @FXML private Label lblUnreadBadge;

    @FXML private TableView<AppNotification>          tableNotifications;
    @FXML private TableColumn<AppNotification, String> colType;
    @FXML private TableColumn<AppNotification, String> colMessage;
    @FXML private TableColumn<AppNotification, String> colDate;
    @FXML private TableColumn<AppNotification, String> colRead;

    @FXML private Label lblStatus;

    private User loggedInUser;
    private final GenericDAO<AppNotification> notifDao =
            new GenericDAO<>(AppNotification.class, "notifications");
    private final ObservableList<AppNotification> notifList = FXCollections.observableArrayList();

    // arranca el controlador con el usuario logeado y carga sus notificaciones
    public void initData(User user) {
        this.loggedInUser = user;
        setupTable();
        loadNotifications();
    }

    // configura las columnas el coloreado de filas y el listener de seleccion
    private void setupTable() {
        colType.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTypeLabel()));
        colMessage.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getMessage()));
        colDate.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getFormattedDate()));
        colRead.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().isRead() ? "✓ Leída" : "● No leída"));

        // el tipo define el color base y las no leidas se resaltan con un tono mas vivo
        tableNotifications.setRowFactory(tv -> new TableRow<AppNotification>() {
            @Override
            protected void updateItem(AppNotification item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                    return;
                }
                String bg = switch (item.getType() != null ? item.getType() : "INFO") {
                    case "CRITICAL" -> item.isRead() ? "#3a1a1a" : "#5a1a1a";
                    case "WARNING"  -> item.isRead() ? "#3a2a1a" : "#5a3a1a";
                    default         -> item.isRead() ? "#1a2a3a" : "#1a3a5a";
                };
                setStyle("-fx-background-color: " + bg + ";");
            }
        });

        tableNotifications.setItems(notifList);

        // al hacer click en una fila la marcamos como leida automaticamente
        tableNotifications.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, newVal) -> {
                if (newVal != null && !newVal.isRead()) {
                    markAsRead(newVal);
                }
            });
    }

    // descarga las notificaciones del usuario en orden descendente con las mas recientes primero
    private void loadNotifications() {
        new Thread(() -> {
            try {
                List<AppNotification> list = notifDao.getByField("userId", loggedInUser.getUid());
                list.sort(Comparator.comparingLong(AppNotification::getCreatedAt).reversed());
                Platform.runLater(() -> {
                    notifList.setAll(list);
                    updateUnreadBadge();
                    lblStatus.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al cargar las notificaciones.", true));
                e.printStackTrace();
            }
        }).start();
    }

    // marca la notificacion seleccionada como leida con rollback visual si falla la escritura
    private void markAsRead(AppNotification n) {
        n.setRead(true);
        new Thread(() -> {
            try {
                notifDao.save(n.getId(), n);
                Platform.runLater(() -> {
                    tableNotifications.refresh();
                    updateUnreadBadge();
                });
            } catch (Exception e) {
                // revertimos en memoria y refrescamos la ui desde el hilo correcto
                n.setRead(false);
                Platform.runLater(() -> {
                    tableNotifications.refresh();
                    updateUnreadBadge();
                });
                e.printStackTrace();
            }
        }).start();
    }

    // marca todas las notificaciones de la lista como leidas con rollback individual si alguna falla
    @FXML
    private void onMarkAllRead() {
        List<AppNotification> unread = notifList.stream()
                .filter(n -> !n.isRead())
                .toList();
        if (unread.isEmpty()) {
            setStatus("Todas las notificaciones ya están leídas.", false);
            return;
        }
        // marcamos en memoria para respuesta inmediata y luego persistimos en firestore
        unread.forEach(n -> n.setRead(true));
        tableNotifications.refresh();
        updateUnreadBadge();
        setStatus("Marcando " + unread.size() + " notificación(es) como leídas...", false);

        new Thread(() -> {
            int errors = 0;
            for (AppNotification n : unread) {
                try {
                    notifDao.save(n.getId(), n);
                } catch (Exception e) {
                    errors++;
                    n.setRead(false);
                    e.printStackTrace();
                }
            }
            final int finalErrors = errors;
            Platform.runLater(() -> {
                tableNotifications.refresh();
                updateUnreadBadge();
                if (finalErrors == 0) {
                    setStatus("Todas las notificaciones marcadas como leídas.", false);
                } else {
                    setStatus("Algunas notificaciones no pudieron marcarse (" + finalErrors + " errores).", true);
                }
            });
        }).start();
    }

    // elimina la notificacion seleccionada si no es de tipo CRITICAL
    @FXML
    private void onDeleteNotification() {
        AppNotification sel = tableNotifications.getSelectionModel().getSelectedItem();
        if (sel == null) {
            setStatus("Selecciona una notificación para eliminar.", true);
            return;
        }
        if ("CRITICAL".equals(sel.getType())) {
            setStatus("Las notificaciones críticas no pueden eliminarse, solo marcarse como leídas.", true);
            return;
        }
        new Thread(() -> {
            try {
                notifDao.delete(sel.getId());
                Platform.runLater(() -> {
                    notifList.remove(sel);
                    updateUnreadBadge();
                    setStatus("Notificación eliminada.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error al eliminar la notificación.", true));
                e.printStackTrace();
            }
        }).start();
    }

    // actualiza el contador de no leidas en el encabezado
    private void updateUnreadBadge() {
        long unread = notifList.stream().filter(n -> !n.isRead()).count();
        lblUnreadBadge.setText(unread > 0 ? "(" + unread + " sin leer)" : "");
    }

    private void setStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(error ? Color.ORANGERED : Color.web("#aaaaaa"));
    }
}
