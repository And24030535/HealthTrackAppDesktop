package com.itc.healthtrack.utils;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

// estilo visual uniforme para todos los dialogos del proyecto
// fondo blanco encabezado gris boton de confirmacion azul y boton de cancelar gris
public class DialogUtils {

    private DialogUtils() {}

    // aplica el estilo blanco a cualquier dialogo de la aplicacion
    public static void applyWhiteStyle(DialogPane dp) {
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
            if (node instanceof Button btn) {
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
