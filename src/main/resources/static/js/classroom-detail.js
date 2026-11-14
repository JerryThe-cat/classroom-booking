(function () {
    'use strict';

    var user = Api.requireAuth();
    if (!user) {
        return;
    }
    Api.fillUser(user);
    Api.bindLogout();

    var id = window.location.pathname.split('/').pop();
    var panel = document.getElementById('detailPanel');

    function render(classroom) {
        var rate = Number(classroom.occupancyRate || 0);
        var width = classroom.occupancyFresh ? Math.max(0, Math.min(100, rate)) : 0;
        var freshText = classroom.occupancyFresh
            ? '实时数据'
            : '暂无实时数据';
        panel.innerHTML =
            '<div class="detail-hero">' +
            '<div><p class="detail-kicker">' + Api.escapeHtml(classroom.building) + '</p>' +
            '<h2>' + Api.escapeHtml(classroom.roomNo) + '</h2>' +
            '<p class="meta">容纳 ' + classroom.capacity + ' 人 · 座位 ' +
            classroom.seatCount + ' 个</p></div>' +
            '<span class="tag ' + (classroom.occupancyFresh ? 'tag-approved' : 'tag-gray') +
            '">' + freshText + '</span></div>' +
            '<div class="occupancy-number">' +
            (classroom.occupancyFresh ? rate.toFixed(2) + '%' : '—') + '</div>' +
            '<div class="progress"><div class="fill" style="width:' + width + '%"></div></div>' +
            '<p class="hint">当前人数：' +
            (classroom.occupancyFresh ? classroom.currentHeadCount + ' 人' : '暂无') +
            ' · 最后更新：' +
            Api.escapeHtml(Api.formatDateTime(classroom.occupancyUpdatedAt)) + '</p>' +
            '<p><strong>设备：</strong>' +
            Api.escapeHtml(classroom.equipment || '暂无设备信息') + '</p>' +
            '<div class="actions" style="margin-top:18px;">' +
            '<a class="btn btn-primary" href="/booking?classroomId=' + classroom.id + '">预约此教室</a>' +
            '<a class="btn btn-secondary" href="/classrooms">返回列表</a></div>';
    }

    function load() {
        Api.get('/classrooms/' + id).then(render).catch(function () {
            panel.innerHTML = '<p class="hint">教室详情加载失败</p>';
        });
    }

    load();
    window.setInterval(load, 30000);
})();
