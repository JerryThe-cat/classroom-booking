(function () {
    'use strict';

    var user = Api.requireAuth('ADMIN');
    if (!user) {
        return;
    }
    Api.fillUser(user);
    Api.bindLogout();

    var topTable = document.getElementById('topTable');
    var classroomSelect = document.getElementById('classroomSelect');
    var occupancyTable = document.getElementById('occupancyTable');

    function renderSummary(data) {
        var items = [
            ['总预约数', data.totalBookings],
            ['待审批', data.pendingBookings],
            ['已通过', data.approvedBookings],
            ['已释放', data.releasedBookings],
            ['已取消', data.cancelledBookings],
            ['已驳回', data.rejectedBookings]
        ];
        document.getElementById('statGrid').innerHTML = items.map(function (item) {
            return '<div class="card stat-card"><div class="stat-num">' + item[1] +
                '</div><div class="stat-label">' + item[0] + '</div></div>';
        }).join('');

        var tops = data.topClassrooms || [];
        topTable.innerHTML = tops.length ? tops.map(function (item) {
            return '<tr><td>' + Api.escapeHtml(item.roomNo) + '</td><td>' +
                item.bookingCount + ' 次</td></tr>';
        }).join('') : '<tr><td colspan="2" class="hint">当前区间暂无预约</td></tr>';
    }

    function renderOccupancy(data) {
        var points = data.points || [];
        occupancyTable.innerHTML = points.length ? points.map(function (point) {
            return '<tr><td>' + Api.escapeHtml(Api.formatDateTime(point.bucketStart)) + '</td><td>' +
                point.avgHeadCount + '</td><td>' + point.maxHeadCount + '</td><td>' +
                point.avgOccupancyRate + '%</td></tr>';
        }).join('') : '<tr><td colspan="4" class="hint">暂无历史上报数据</td></tr>';
    }

    function loadBase() {
        Api.get('/admin/dashboard/summary').then(renderSummary);
        Api.get('/classrooms', { status: 'ALL' }).then(function (list) {
            classroomSelect.innerHTML = (list || []).map(function (item) {
                return '<option value="' + item.id + '">' +
                    Api.escapeHtml(item.roomNo + ' · ' + item.building) + '</option>';
            }).join('');
        });
    }

    document.getElementById('loadOccupancy').addEventListener('click', function () {
        if (!classroomSelect.value) {
            return;
        }
        Api.get('/admin/dashboard/occupancy-series', {
            classroomId: classroomSelect.value,
            bucketMinutes: 60
        }).then(renderOccupancy);
    });

    loadBase();
})();
