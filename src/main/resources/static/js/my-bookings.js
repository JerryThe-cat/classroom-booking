(function () {
    'use strict';

    var user = Api.requireAuth();
    if (!user) {
        return;
    }
    Api.fillUser(user);
    Api.bindLogout();

    var table = document.getElementById('myTable');

    function canCancel(booking) {
        return ['PENDING', 'APPROVED'].includes(booking.status)
            && new Date(booking.startTime) > new Date();
    }

    function render(page) {
        var list = page.items || [];
        if (!list.length) {
            table.innerHTML = '<tr><td colspan="7" class="hint">暂无预约记录</td></tr>';
            return;
        }
        table.innerHTML = list.map(function (booking) {
            var action = canCancel(booking)
                ? '<button class="btn btn-danger btn-sm" type="button" data-cancel="' +
                    booking.id + '">取消</button>'
                : '—';
            return '<tr>' +
                '<td>' + Api.escapeHtml(booking.roomNo || ('教室#' + booking.classroomId)) + '</td>' +
                '<td>' + Api.escapeHtml(Api.formatDateTime(booking.startTime)) + '</td>' +
                '<td>' + Api.escapeHtml(Api.formatDateTime(booking.endTime)) + '</td>' +
                '<td><span class="tag ' + Api.statusClass(booking.status) + '">' +
                    Api.statusText(booking.status) + '</span></td>' +
                '<td>' + Api.escapeHtml(booking.purpose) + '</td>' +
                '<td>' + Api.escapeHtml(booking.rejectReason || booking.releaseReason || '—') + '</td>' +
                '<td>' + action + '</td></tr>';
        }).join('');
    }

    function load() {
        Api.get('/bookings/my', { page: 1, size: 100 })
            .then(render)
            .catch(function () {
                table.innerHTML = '<tr><td colspan="7" class="hint">预约加载失败</td></tr>';
            });
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('button[data-cancel]');
        if (!button || !window.confirm('确认取消该预约？')) {
            return;
        }
        Api.put('/bookings/' + button.getAttribute('data-cancel') + '/cancel')
            .then(function () {
                window.alert('[系统提示] 取消成功');
                load();
            });
    });

    load();
})();
