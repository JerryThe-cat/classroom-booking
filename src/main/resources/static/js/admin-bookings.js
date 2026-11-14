(function () {
    'use strict';

    var user = Api.requireAuth('ADMIN');
    if (!user) {
        return;
    }
    Api.fillUser(user);
    Api.bindLogout();

    var table = document.getElementById('bookingTable');
    var statusFilter = document.getElementById('statusFilter');

    function render(page) {
        var list = page.items || [];
        table.innerHTML = list.length ? list.map(function (booking) {
            var releasable = ['PENDING', 'APPROVED'].includes(booking.status);
            return '<tr>' +
                '<td>' + booking.id + '</td>' +
                '<td>' + Api.escapeHtml(booking.userName || ('用户#' + booking.userId)) + '</td>' +
                '<td>' + Api.escapeHtml(booking.roomNo || '—') + '</td>' +
                '<td>' + Api.escapeHtml(Api.formatDateTime(booking.startTime)) + '<br>' +
                    Api.escapeHtml(Api.formatDateTime(booking.endTime)) + '</td>' +
                '<td>' + Api.escapeHtml(booking.purpose) + '</td>' +
                '<td><span class="tag ' + Api.statusClass(booking.status) + '">' +
                    Api.statusText(booking.status) + '</span></td>' +
                '<td>' + Api.escapeHtml(booking.approverName || '—') + '</td>' +
                '<td>' + Api.escapeHtml(booking.rejectReason || booking.releaseReason || '—') + '</td>' +
                '<td>' + (releasable
                    ? '<button class="btn btn-danger btn-sm" type="button" data-release="' +
                        booking.id + '">释放</button>'
                    : '—') + '</td></tr>';
        }).join('') : '<tr><td colspan="9" class="hint">暂无预约记录</td></tr>';
    }

    function load() {
        Api.get('/bookings/admin', {
            status: statusFilter.value,
            page: 1,
            size: 100
        }).then(render);
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('button[data-release]');
        if (!button) {
            return;
        }
        var reason = window.prompt('请输入人工释放原因');
        if (reason === null) {
            return;
        }
        if (!reason.trim()) {
            window.alert('释放原因不能为空');
            return;
        }
        Api.put('/bookings/admin/' + button.getAttribute('data-release') + '/release', {
            reason: reason.trim()
        }).then(function () {
            window.alert('[系统提示] 预约已人工释放');
            load();
        });
    });

    statusFilter.addEventListener('change', load);
    load();
})();
