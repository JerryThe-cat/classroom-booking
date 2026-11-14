(function () {
    'use strict';

    var user = Api.requireAuth('ADMIN');
    if (!user) {
        return;
    }
    Api.fillUser(user);
    Api.bindLogout();

    var form = document.getElementById('classroomForm');
    var table = document.getElementById('classroomTable');
    var cancelEdit = document.getElementById('cancelEdit');
    var submitButton = document.getElementById('saveClassroom');

    function resetForm() {
        form.reset();
        document.getElementById('classroomId').value = '';
        document.getElementById('status').value = 'ACTIVE';
        submitButton.textContent = '新增教室';
        cancelEdit.style.display = 'none';
        document.getElementById('disableCurrent').style.display = 'none';
    }

    function load() {
        Api.get('/classrooms', { status: 'ALL' }).then(function (list) {
            table.innerHTML = (list || []).map(function (item) {
                var status = item.status === 'ACTIVE'
                    ? '<span class="tag tag-approved">启用</span>'
                    : '<span class="tag tag-gray">停用</span>';
                return '<tr>' +
                    '<td>' + Api.escapeHtml(item.roomNo) + '</td>' +
                    '<td>' + Api.escapeHtml(item.building) + '</td>' +
                    '<td>' + item.capacity + '</td>' +
                    '<td>' + item.seatCount + '</td>' +
                    '<td>' + Api.escapeHtml(item.equipment || '—') + '</td>' +
                    '<td>' + status + '</td>' +
                    '<td><div class="actions">' +
                    '<button class="btn btn-secondary btn-sm" type="button" data-edit="' + item.id + '">编辑</button>' +
                    (item.status === 'ACTIVE'
                        ? '<button class="btn btn-danger btn-sm" type="button" data-disable="' +
                            item.id + '">停用</button>'
                        : '') +
                    '</div></td></tr>';
            }).join('');
            window.__classrooms = list || [];
        });
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var id = document.getElementById('classroomId').value;
        var body = {
            roomNo: document.getElementById('roomNo').value.trim(),
            building: document.getElementById('building').value.trim(),
            capacity: Number(document.getElementById('capacity').value),
            seatCount: Number(document.getElementById('seatCount').value),
            equipment: document.getElementById('equipment').value.trim(),
            status: document.getElementById('status').value
        };
        var request = id
            ? Api.put('/classrooms/' + id, body)
            : Api.post('/classrooms', body);
        request.then(function () {
            window.alert('[系统提示] 教室信息已保存');
            resetForm();
            load();
        });
    });

    document.addEventListener('click', function (event) {
        var edit = event.target.closest('button[data-edit]');
        var disable = event.target.closest('button[data-disable]');
        if (edit) {
            var item = (window.__classrooms || []).find(function (classroom) {
                return String(classroom.id) === edit.getAttribute('data-edit');
            });
            if (!item) {
                return;
            }
            document.getElementById('classroomId').value = item.id;
            document.getElementById('roomNo').value = item.roomNo;
            document.getElementById('building').value = item.building;
            document.getElementById('capacity').value = item.capacity;
            document.getElementById('seatCount').value = item.seatCount;
            document.getElementById('equipment').value = item.equipment || '';
            document.getElementById('status').value = item.status;
            submitButton.textContent = '保存修改';
            cancelEdit.style.display = 'inline-block';
            document.getElementById('disableCurrent').style.display = 'inline-block';
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
        if (disable && window.confirm('确认停用该教室？已有历史记录会保留。')) {
            var id = disable.getAttribute('data-disable');
            Api.delete('/classrooms/' + id).then(function () {
                load();
            });
        }
    });

    document.getElementById('disableCurrent').addEventListener('click', function () {
        var id = document.getElementById('classroomId').value;
        if (!id || !window.confirm('确认停用当前教室？')) {
            return;
        }
        Api.delete('/classrooms/' + id).then(function () {
            resetForm();
            load();
        });
    });

    cancelEdit.addEventListener('click', resetForm);
    load();
})();
