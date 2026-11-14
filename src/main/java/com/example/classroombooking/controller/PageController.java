package com.example.classroombooking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/classrooms")
    public String classrooms() {
        return "classrooms";
    }

    @GetMapping("/classrooms/{id}")
    public String classroomDetail(@PathVariable Long id) {
        return "classroom-detail";
    }

    @GetMapping("/booking")
    public String booking() {
        return "booking";
    }

    @GetMapping("/my-bookings")
    public String myBookings() {
        return "my-bookings";
    }

    @GetMapping("/admin/classrooms")
    public String adminClassrooms() {
        return "admin-classrooms";
    }

    @GetMapping("/admin/pending")
    public String adminPending() {
        return "admin-pending";
    }

    @GetMapping("/admin/bookings")
    public String adminBookings() {
        return "admin-bookings";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }
}
