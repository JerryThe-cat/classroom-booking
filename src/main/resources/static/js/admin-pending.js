(function () {
    'use strict';

    var user = Api.requireAuth('ADMIN');
    if (!user) {
        return;
    }
    Api.fillUser(user);
    Api.bindLogout();

    var table = document.getElementById('pendingTable');

    function render(page) {
        var list = page.items || [];
        if (!list.length) {
            table.innerHTML = '<tr><td colspan="9" class="hint">暂无待审批申请</td></tr>';
            return;
        }
        table.innerHTML = list.map(function (booking) {
            var conflicts = booking.conflictingBookingIds || [];
            var conflictText = conflicts.length
                ? '<span class="tag ' + (booking.conflictWithApproved ? 'tag-rejected' : 'tag-pending') + '">' +
                    (booking.conflictWithApproved ? '与已通过预约冲突' : '存在同时候选') + '</span>'
                : '<span class="tag tag-approved">无冲突</span>';
            return '<tr>' +
                '<td>' + booking.id + '</td>' +
                '<td>' + Api.escapeHtml(booking.userName || ('用户#' + booking.userId)) + '</td>' +
                '<td>' + Api.escapeHtml(booking.roomNo || '—') + '</td>' +
                '<td>' + Api.escapeHtml(Api.formatDateTime(booking.startTime)) + '<br>' +
                    Api.escapeHtml(Api.formatDateTime(booking.endTime)) + '</td>' +
                '<td>' + Api.escapeHtml(booking.purpose) + '</td>' +
                '<td>' + booking.expectedAttendees + ' 人</td>' +
                '<td><span class="tag ' + Api.statusClass(booking.status) + '">' +
                    Api.statusText(booking.status) + '</span></td>' +
                '<td>' + conflictText + '</td>' +
                '<td><div class="actions">' +
                '<button class="btn btn-primary btn-sm" type="button" data-audit="' +
                    booking.id + '" data-decision="APPROVE">通过</button>' +
                '<button class="btn btn-danger btn-sm" type="button" data-audit="' +
                    booking.id + '" data-decision="REJECT">驳回</button>' +
                '</div></td></tr>';
        }).join('');
    }

    function load() {
        Api.get('/bookings/admin/pending', { page: 1, size: 100 })
            .then(render)
            .catch(function () {
                table.innerHTML = '<tr><td colspan="9" class="hint">加载失败</td></tr>';
            });
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('button[data-audit]');
        if (!button) {
            return;
        }
        var decision = button.getAttribute('data-decision');
        var reason = null;
        if (decision === 'REJECT') {
            reason = window.prompt('请输入驳回理由');
            if (reason === null) {
                return;
            }
            if (!reason.trim()) {
                window.alert('驳回理由不能为空');
                return;
            }
        } else if (!window.confirm('确认通过该预约？')) {
            return;
        }

        var body = { decision: decision };
        if (reason) {
            body.reason = reason.trim();
        }
        Api.put('/bookings/admin/' + button.getAttribute('data-audit') + '/audit', body)
            .then(function () {
                window.alert('[系统提示] 审批完成');
                load();
            });
    });

    load();
})();
