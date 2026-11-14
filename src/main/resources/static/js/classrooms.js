(function () {
    'use strict';

    var user = Api.requireAuth();
    if (!user) {
        return;
    }
    Api.fillUser(user);
    Api.bindLogout();

    var classGrid = document.getElementById('classGrid');
    var buildingFilter = document.getElementById('buildingFilter');
    var allClassrooms = [];

    function rateDisplay(item) {
        if (!item.occupancyFresh) {
            return '暂无实时数据' + (item.occupancyUpdatedAt
                ? '（最后更新：' + Api.escapeHtml(Api.formatDateTime(item.occupancyUpdatedAt)) + '）'
                : '');
        }
        return Number(item.occupancyRate || 0).toFixed(2) + '%';
    }

    function render() {
        var selectedBuilding = buildingFilter.value;
        var list = selectedBuilding === 'ALL'
            ? allClassrooms
            : allClassrooms.filter(function (item) { return item.building === selectedBuilding; });
        if (!list.length) {
            classGrid.innerHTML = '<p class="hint">暂无教室数据</p>';
            return;
        }

        classGrid.innerHTML = list.map(function (item) {
            var rate = Number(item.occupancyRate || 0);
            var width = item.occupancyFresh ? Math.max(0, Math.min(100, rate)) : 0;
            var equipment = (item.equipment || '').split(',').filter(Boolean)
                .map(function (tag) {
                    return '<span class="tag tag-gray">' + Api.escapeHtml(tag) + '</span>';
                }).join('');
            var freshness = item.occupancyFresh
                ? '<span class="tag tag-approved">实时</span>'
                : '<span class="tag tag-gray">无实时数据</span>';
            return '<div class="card class-card">' +
                '<p class="title">' + Api.escapeHtml(item.roomNo) +
                ' <span class="tag tag-gray">' + Api.escapeHtml(item.building) + '</span> ' +
                freshness + '</p>' +
                '<p class="meta">容量：' + item.capacity + ' 人 · 座位：' + item.seatCount +
                ' · 当前占用率：' + rateDisplay(item) + '</p>' +
                '<div class="progress"><div class="fill" style="width:' + width + '%"></div></div>' +
                '<div class="equip">' + equipment + '</div>' +
                '<div class="actions" style="margin-top:12px;">' +
                '<a class="btn btn-secondary btn-sm" href="/classrooms/' + item.id + '">详情</a>' +
                '<a class="btn btn-primary btn-sm" href="/booking?classroomId=' + item.id + '">预约</a>' +
                '</div></div>';
        }).join('');
    }

    function populateBuildings(list) {
        var current = buildingFilter.value;
        var buildings = Array.from(new Set(list.map(function (item) { return item.building; })));
        buildingFilter.innerHTML = '<option value="ALL">全部教学楼</option>' +
            buildings.map(function (building) {
                return '<option value="' + Api.escapeHtml(building) + '">' +
                    Api.escapeHtml(building) + '</option>';
            }).join('');
        if (buildings.includes(current)) {
            buildingFilter.value = current;
        }
    }

    function load() {
        Api.get('/classrooms').then(function (list) {
            allClassrooms = list || [];
            populateBuildings(allClassrooms);
            render();
        }).catch(function () {
            classGrid.innerHTML = '<p class="hint">教室列表加载失败</p>';
        });
    }

    buildingFilter.addEventListener('change', render);
    load();
    window.setInterval(load, 30000);
})();
