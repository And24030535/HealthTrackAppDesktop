package com.itc.healthtrack.services;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.EmergencyContact;
import com.itc.healthtrack.models.Metric;
import com.itc.healthtrack.models.User;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

// servicio asincrono para enviar correos por smtp
public class NotificationService {

    // config del servidor smtp de gmail
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String SYSTEM_EMAIL = "clinicahealthtrack@gmail.com";
    private static final String SYSTEM_PASSWORD = "yaih bgnl dubi ctgs";

    // formato de la marca de tiempo
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // notifica a un paciente en hilo secundario para no congelar la interfaz
    public void notifyPatient(User patient, String message) {
        new Thread(() -> {
            String subject = "HealthTrack - Alerta de Salud";
            String recipientName = patient.getFirstName() + " " + patient.getLastName();
            sendEmail(patient.getEmail(), subject, recipientName, message);
        }).start();
    }

    // manda al paciente una recomendacion formal desde el medico
    public void sendRecommendationEmail(User patient, String doctorFullName, String title, String message) {
        new Thread(() -> {
            String subject    = "HealthTrack - Nueva recomendación de tu médico";
            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String body = "Tu médico " + doctorFullName + " ha generado una nueva recomendación para ti:\n\n"
                    + "Título: " + title + "\n\n"
                    + "Mensaje:\n" + message;
            sendEmail(patient.getEmail(), subject, patientName, body);
        }).start();
    }

    // notifica al medico tambien en hilo aparte
    public void notifyDoctor(User doctor, String message) {
        new Thread(() -> {
            String subject = "HealthTrack - Actualización de Paciente";
            String recipientName = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
            sendEmail(doctor.getEmail(), subject, recipientName, message);
        }).start();
    }

    // notifica al contacto de emergencia principal cuando hay alerta critica
    public void notifyEmergencyContact(User patient, Metric metric) {
        if (patient == null || patient.getUid() == null) return;
        new Thread(() -> {
            try {
                GenericDAO<EmergencyContact> contactDao = new GenericDAO<>(
                        EmergencyContact.class, "users/" + patient.getUid() + "/emergencyContacts");
                List<EmergencyContact> contacts = contactDao.getAll();
                if (contacts == null || contacts.isEmpty()) {
                    System.err.println("[NotificationService] Sin contactos de emergencia para el paciente.");
                    return;
                }

                EmergencyContact primary = selectPrimaryContact(contacts);
                if (primary == null || primary.getPhone() == null || primary.getPhone().isBlank()) {
                    System.err.println("[NotificationService] Contacto de emergencia sin correo o teléfono válido.");
                    return;
                }

                String subject = "HealthTrack - Alerta de Emergencia";
                String recipientName = primary.getFullName() != null ? primary.getFullName() : "Contacto de Emergencia";
                String message = buildEmergencyMessage(patient, metric, primary);
                sendEmail(primary.getPhone(), subject, recipientName, message);
            } catch (Exception e) {
                System.err.println("[NotificationService] Error al notificar contacto de emergencia: " + e.getMessage());
            }
        }).start();
    }

    private EmergencyContact selectPrimaryContact(List<EmergencyContact> contacts) {
        return contacts.stream()
                .filter(contact -> contact != null)
                .sorted(Comparator.comparing(EmergencyContact::getId,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .findFirst()
                .orElse(null);
    }

    private String buildEmergencyMessage(User patient, Metric metric, EmergencyContact contact) {
        StringBuilder body = new StringBuilder();
        String patientName = patient.getFirstName() + " " + patient.getLastName();
        body.append("Se detectó una alerta crítica en el paciente ").append(patientName).append(".\n\n");

        if (metric != null) {
            if (metric.getSystolic() != null && metric.getDiastolic() != null) {
                body.append("Presión arterial: ").append(metric.getSystolic())
                        .append("/").append(metric.getDiastolic()).append(" mmHg\n");
            }
            if (metric.getGlucoseLevel() != null) {
                body.append("Glucosa: ").append(metric.getGlucoseLevel()).append(" mg/dL\n");
            }
        }

        if (contact.getRelationship() != null && !contact.getRelationship().isBlank()) {
            body.append("\nParentesco: ").append(contact.getRelationship()).append("\n");
        }

        body.append("\nPor favor contacte al paciente o a su médico lo antes posible.");
        return body.toString();
    }

    // construye y manda el correo
    private void sendEmail(String toEmail, String subject, String recipientName, String messageBody) {
        // checamos que la direccion sea valida antes de intentar conectar
        if (toEmail == null || toEmail.isEmpty()) {
            System.err.println("No se proporcionó correo para el destinatario: " + recipientName);
            return;
        }

        // propiedades de seguridad y conexion smtp
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);

        // sesion autenticada con las credenciales del sistema
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SYSTEM_EMAIL, SYSTEM_PASSWORD);
            }
        });

        try {
            // mensaje mime
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SYSTEM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            // cuerpo del correo que vera el usuario
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String fullMessage = "Hola " + recipientName + ",\n\n" +
                    messageBody + "\n\n" +
                    "Generado el: " + timestamp + "\n" +
                    "HealthTrack Community - OwO";

            message.setText(fullMessage);

            // envio final
            Transport.send(message);
            System.out.println("Correo enviado exitosamente a: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Fallo al enviar el correo a: " + toEmail);
            e.printStackTrace();
        }
    }
}
