package com.example.classroombooking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ApiIntegrationTests {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private String adminToken;
    private String studentOneToken;
    private String studentTwoToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        adminToken = login("admin", "admin-test-pass");
        studentOneToken = registerAndLogin("one-" + UUID.randomUUID(), "student-pass-1", "学生一");
        studentTwoToken = registerAndLogin("two-" + UUID.randomUUID(), "student-pass-2", "学生二");
    }

    @Test
    void authenticationAndCameraReportWork() throws Exception {
        mockMvc.perform(get("/api/v1/classrooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID_OR_EXPIRED"));

        String collectedAt = java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Shanghai"))
                .withNano(0)
                .toString();
        String body = """
                {
                  "classroomId": 1,
                  "deviceId": "cam-A101-01",
                  "headCount": 42,
                  "occupancyRate": 43.75,
                  "collectedAt": "%s",
                  "source": "REAL",
                  "sequence": 1001
                }
                """.formatted(collectedAt);

        mockMvc.perform(post("/api/v1/ai/occupancy/report")
                        .header("X-Device-Id", "cam-A101-01")
                        .header("X-Device-Token", "camera-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentUpdated").value(true));

        mockMvc.perform(get("/api/v1/classrooms")
                        .header("Authorization", "Bearer " + studentOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].occupancyFresh").value(true))
                .andExpect(jsonPath("$.data[0].occupancyRate").value(43.75));
    }

    @Test
    void pendingBookingsCanCoexistButOnlyOneCanBeApproved() throws Exception {
        LocalDate date = LocalDate.now().plusDays(30);
        String start = date + "T10:00:00+08:00";
        String end = date + "T12:00:00+08:00";

        long firstId = createBooking(studentOneToken, start, end);
        long secondId = createBooking(studentTwoToken, start, end);

        mockMvc.perform(put("/api/v1/bookings/admin/{id}/audit", firstId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approverId").isNumber());

        mockMvc.perform(put("/api/v1/bookings/admin/{id}/audit", secondId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BOOKING_CONFLICT_APPROVED"));

        mockMvc.perform(put("/api/v1/bookings/{id}/cancel", firstId)
                        .header("Authorization", "Bearer " + studentOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(put("/api/v1/bookings/admin/{id}/audit", secondId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/bookings/my?page=1&size=20")
                        .header("Authorization", "Bearer " + studentOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.items[0].roomNo").value("A101"));
    }

    @Test
    void publicRegistrationCannotCreateAdmin() throws Exception {
        String username = "role-" + UUID.randomUUID();
        String body = """
                {
                  "username": "%s",
                  "password": "student-pass",
                  "name": "越权测试",
                  "department": "计算机学院",
                  "role": "ADMIN"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"student-pass"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"));
    }

    private String registerAndLogin(String username, String password, String name) throws Exception {
        String body = """
                {
                  "username": "%s",
                  "password": "%s",
                  "name": "%s",
                  "department": "计算机学院"
                }
                """.formatted(username, password, name);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        return login(username, password);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private long createBooking(String token, String start, String end) throws Exception {
        String body = """
                {
                  "classroomId": 1,
                  "startTime": "%s",
                  "endTime": "%s",
                  "purpose": "接口测试",
                  "expectedAttendees": 8
                }
                """.formatted(start, end);
        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.bookingId");
        return id.longValue();
    }
}
