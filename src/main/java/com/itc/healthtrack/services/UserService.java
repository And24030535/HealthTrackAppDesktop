package com.itc.healthtrack.services;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;

import java.util.ArrayList;
import java.util.List;

// centraliza la logica para obtener la lista de pacientes visibles para cada usuario
public class UserService {

    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");

    // devuelve los pacientes que el usuario puede ver
    // el admin ve todos y el medico solo ve los que tiene asignados
    public List<User> getPatientsForUser(User viewer) throws Exception {
        List<User> allPatients     = userDao.getByField("role", "patient");
        List<User> visiblePatients = new ArrayList<>();

        for (User patient : allPatients) {
            if ("admin".equals(viewer.getRole())) {
                // el admin tiene acceso a todos los pacientes del sistema
                visiblePatients.add(patient);
            } else if (viewer.getUid() != null
                    && viewer.getUid().equals(patient.getAssignedDoctorId())) {
                // el medico solo ve los pacientes que tiene asignados
                visiblePatients.add(patient);
            }
        }

        return visiblePatients;
    }
}
