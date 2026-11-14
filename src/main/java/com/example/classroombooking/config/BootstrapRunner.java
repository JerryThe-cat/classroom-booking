package com.example.classroombooking.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.classroombooking.entity.AiDevice;
import com.example.classroombooking.entity.Classroom;
import com.example.classroombooking.mapper.AiDeviceMapper;
import com.example.classroombooking.mapper.ClassroomMapper;
import com.example.classroombooking.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(1)
public class BootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final UserService userService;
    private final AiDeviceMapper aiDeviceMapper;
    private final ClassroomMapper classroomMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.admin-bootstrap.username:}")
    private String adminUsername;
    @Value("${app.admin-bootstrap.password:}")
    private String adminPassword;
    @Value("${app.admin-bootstrap.name:系统管理员}")
    private String adminName;
    @Value("${app.admin-bootstrap.department:教务处}")
    private String adminDepartment;

    @Value("${app.camera-bootstrap.device-id:}")
    private String cameraDeviceId;
    @Value("${app.camera-bootstrap.token:}")
    private String cameraToken;
    @Value("${app.camera-bootstrap.classroom-id:}")
    private Long cameraClassroomId;
    @Value("${app.camera.report-interval-seconds:30}")
    private int cameraIntervalSeconds;

    public BootstrapRunner(
            UserService userService,
            AiDeviceMapper aiDeviceMapper,
            ClassroomMapper classroomMapper,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.aiDeviceMapper = aiDeviceMapper;
        this.classroomMapper = classroomMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.ensureAdmin(adminUsername, adminPassword, adminName, adminDepartment);
        ensureCameraDevice();
    }

    private void ensureCameraDevice() {
        if (!StringUtils.hasText(cameraDeviceId)
                || !StringUtils.hasText(cameraToken)
                || cameraClassroomId == null) {
            log.info("摄像头设备未配置，AI 上报接口暂不绑定设备");
            return;
        }

        Classroom classroom = classroomMapper.selectById(cameraClassroomId);
        if (classroom == null) {
            log.error("摄像头设备初始化失败：教室 {} 不存在", cameraClassroomId);
            return;
        }

        LambdaQueryWrapper<AiDevice> query = new LambdaQueryWrapper<>();
        query.eq(AiDevice::getDeviceId, cameraDeviceId);
        AiDevice device = aiDeviceMapper.selectOne(query);
        if (device == null) {
            device = new AiDevice();
            device.setDeviceId(cameraDeviceId);
            device.setClassroomId(cameraClassroomId);
            device.setTokenHash(passwordEncoder.encode(cameraToken));
            device.setStatus("ACTIVE");
            device.setReportIntervalSeconds(cameraIntervalSeconds);
            aiDeviceMapper.insert(device);
        } else {
            device.setClassroomId(cameraClassroomId);
            device.setTokenHash(passwordEncoder.encode(cameraToken));
            device.setStatus("ACTIVE");
            device.setReportIntervalSeconds(cameraIntervalSeconds);
            aiDeviceMapper.updateById(device);
        }
        log.info("摄像头设备 {} 已绑定教室 {}", cameraDeviceId, classroom.getRoomNo());
    }
}
