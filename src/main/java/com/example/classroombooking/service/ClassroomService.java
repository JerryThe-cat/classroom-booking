package com.example.classroombooking.service;

import com.example.classroombooking.dto.ClassroomUpsertDTO;
import com.example.classroombooking.entity.Classroom;

import java.util.List;

public interface ClassroomService {
    List<Classroom> list(boolean admin, String building, String status);

    Classroom get(Long id, boolean admin);

    Classroom create(ClassroomUpsertDTO dto);

    Classroom update(Long id, ClassroomUpsertDTO dto);

    void disable(Long id);

    Classroom requireEntity(Long id);
}
