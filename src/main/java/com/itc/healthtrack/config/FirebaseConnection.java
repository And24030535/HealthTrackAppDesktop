package com.itc.healthtrack.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;

// conexion a firebase implementada como singleton para reutilizar la misma instancia en todo el proyecto
public class FirebaseConnection {

    private static volatile FirebaseConnection instance;
    private final Firestore db;

    private FirebaseConnection() {
        try {
            // cargamos el archivo de credenciales desde el classpath para que firebase pueda autenticarse
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-key.json");

            if (serviceAccount == null) {
                throw new RuntimeException("Archivo firebase-key.json no encontrado.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // inicializamos la app solo si no habia sido inicializada antes para evitar duplicados
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            db = FirestoreClient.getFirestore();

        } catch (IOException e) {
            throw new RuntimeException("Error al conectar con Firebase: " + e.getMessage());
        }
    }

    // devuelve la unica instancia y la crea si es la primera vez que se llama
    public static FirebaseConnection getInstance() {
        if (instance == null) {
            synchronized (FirebaseConnection.class) {
                if (instance == null) {
                    instance = new FirebaseConnection();
                }
            }
        }
        return instance;
    }

    // regresa la instancia de firestore para hacer consultas
    public Firestore getFirestore() {
        return db;
    }
}
