package com.example.classroombooking.controller;

import com.example.classroombooking.common.RequestAuth;
import com.example.classroombooking.common.Result;
import com.example.classroombooking.dto.ClassroomUpsertDTO;
import com.example.classroombooking.entity.Classroom;
import com.example.classroombooking.service.ClassroomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classrooms")
public class ClassroomController {
    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping
    public Result<List<Classroom>> list(
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        return Result.success(
                classroomService.list(RequestAuth.isAdmin(request), building, status)
        );
    }

    @GetMapping("/{id}")
    public Result<Classroom> detail(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(classroomService.get(id, RequestAuth.isAdmin(request)));
    }

    @PostMapping
    public ResponseEntity<Result<Classroom>> create(
            @Valid @RequestBody ClassroomUpsertDTO dto,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        Classroom classroom = classroomService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(201, classroom));
    }

    @PutMapping("/{id}")
    public Result<Classroom> update(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomUpsertDTO dto,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        return Result.success(classroomService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<String> disable(@PathVariable Long id, HttpServletRequest request) {
        RequestAuth.requireAdmin(request);
        classroomService.disable(id);
        return Result.success("教室已停用");
    }
}
