(function () {
    'use strict';

    var user = Api.requireAuth();
    if (!user) {
        return;
    }
    Api.fillUser(user);
    Api.bindLogout();

    var form = document.getElementById('bookingForm');
    if (!form) {
        return;
    }

    var errorEl = document.getElementById('bookingError');
    var btn = document.getElementById('bookingBtn');
    var select = document.getElementById('classroomSelect');
    var attendees = document.getElementById('expectedAttendees');
    var classroomById = {};

    function showError(message) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }

    function formatTime(value) {
        return value.length === 16 ? value + ':00+08:00' : value + '+08:00';
    }

    function updateMaxAttendees() {
        var classroom = classroomById[select.value];
        attendees.max = classroom ? classroom.capacity : '';
        if (classroom) {
            attendees.placeholder = '1 ~ ' + classroom.capacity;
        }
    }

    Api.get('/classrooms').then(function (list) {
        (list || []).forEach(function (classroom) {
            classroomById[classroom.id] = classroom;
            var option = document.createElement('option');
            option.value = classroom.id;
            option.textContent = classroom.roomNo + '（' + classroom.building +
                ' · 容纳 ' + classroom.capacity + ' 人）';
            select.appendChild(option);
        });
        var query = new URLSearchParams(window.location.search);
        if (query.get('classroomId') && classroomById[query.get('classroomId')]) {
            select.value = query.get('classroomId');
        }
        updateMaxAttendees();
    }).catch(function () {
        showError('教室列表加载失败');
    });

    select.addEventListener('change', updateMaxAttendees);

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var startTime = document.getElementById('startTime').value;
        var endTime = document.getElementById('endTime').value;
        var purpose = document.getElementById('purpose').value.trim();
        var expectedAttendees = attendees.value;
        var classroom = classroomById[select.value];

        if (!select.value || !startTime || !endTime || !purpose || !expectedAttendees) {
            showError('请填写完整的预约信息');
            return;
        }
        if (new Date(endTime) <= new Date(startTime)) {
            showError('结束时间必须晚于开始时间');
            return;
        }
        if (classroom && Number(expectedAttendees) > classroom.capacity) {
            showError('预计人数不能超过教室容纳人数');
            return;
        }

        errorEl.style.display = 'none';
        btn.disabled = true;
        btn.textContent = '提交中...';
        Api.post('/bookings', {
            classroomId: Number(select.value),
            startTime: formatTime(startTime),
            endTime: formatTime(endTime),
            purpose: purpose,
            expectedAttendees: Number(expectedAttendees)
        }).then(function (data) {
            var message = '预约申请已提交，请等待审批';
            if (data.conflictWithPending) {
                message += '。当前有其他待审批申请选择同一时段，管理员将综合审批。';
            }
            window.alert('[系统提示] ' + message);
            window.location.href = '/my-bookings';
        }).catch(function () {
            btn.disabled = false;
            btn.textContent = '提交预约';
        });
    });
})();
