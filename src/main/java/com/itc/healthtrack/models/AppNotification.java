package com.itc.healthtrack.models;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// notificacion del sistema guardada en la coleccion notifications
// el tipo puede ser CRITICAL que no se puede eliminar WARNING o INFO
// delivered indica si ademas se envio un correo electronico al destinatario
// read se vuelve true cuando el usuario abre la notificacion
public class AppNotification {

    private String id;
    private String userId;
    private String message;
    private String type;        // CRITICAL WARNING o INFO
    private boolean delivered;  // true si ademas se envio un correo al destinatario
    private long createdAt;     // milisegundos unix del momento en que se genero
    private boolean read;

    // constructor vacio requerido por el sdk de firestore
    public AppNotification() {}

    public AppNotification(String id, String userId, String message, String type,
                           boolean delivered, long createdAt, boolean read) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.delivered = delivered;
        this.createdAt = createdAt;
        this.read = read;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    // texto formateado para la columna de fecha en la tabla
    public String getFormattedDate() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());
        return fmt.format(Instant.ofEpochMilli(createdAt));
    }

    // etiqueta legible del tipo para mostrar en la tabla de notificaciones
    public String getTypeLabel() {
        if (type == null) return "INFO";
        return switch (type) {
            case "CRITICAL" -> "CRÍTICA";
            case "WARNING"  -> "ADVERTENCIA";
            default         -> "INFO";
        };
    }
}
