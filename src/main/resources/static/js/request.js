(function (window) {
    'use strict';

    var API_BASE = '/api/v1';

    function showError(message) {
        window.alert('[系统提示] ' + message);
    }

    function clearLoginAndRedirect() {
        window.localStorage.removeItem('token');
        window.localStorage.removeItem('user');
        if (window.location.pathname !== '/login') {
            window.location.replace('/login');
        }
    }

    var instance = window.axios.create({
        baseURL: API_BASE,
        timeout: 10000
    });

    instance.interceptors.request.use(function (config) {
        var token = window.localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = 'Bearer ' + token;
        }
        return config;
    });

    instance.interceptors.response.use(function (response) {
        var body = response.data;
        if (body && body.code >= 200 && body.code < 300) {
            return body.data;
        }
        var message = (body && body.message) || '请求失败';
        showError(message);
        return Promise.reject({
            errorCode: body && body.errorCode,
            message: message,
            response: response
        });
    }, function (error) {
        var status = error.response && error.response.status;
        var body = error.response && error.response.data;
        if (status === 401) {
            clearLoginAndRedirect();
            return Promise.reject(error);
        }
        var message = (body && body.message)
            || (error.message ? '网络异常：' + error.message : '网络异常，请稍后重试');
        showError(message);
        error.errorCode = body && body.errorCode;
        return Promise.reject(error);
    });

    function statusText(status) {
        var map = {
            PENDING: '待审批',
            APPROVED: '已通过',
            REJECTED: '已驳回',
            CANCELLED: '已取消',
            RELEASED: '已释放'
        };
        return map[status] || '未知状态';
    }

    function statusClass(status) {
        var map = {
            PENDING: 'tag-pending',
            APPROVED: 'tag-approved',
            REJECTED: 'tag-rejected',
            CANCELLED: 'tag-gray',
            RELEASED: 'tag-gray'
        };
        return map[status] || 'tag-gray';
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatDateTime(value) {
        if (!value) {
            return '—';
        }
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }
        return date.toLocaleString('zh-CN', {
            hour12: false,
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function getUser() {
        try {
            return JSON.parse(window.localStorage.getItem('user') || 'null');
        } catch (error) {
            return null;
        }
    }

    function requireAuth(requiredRole) {
        var token = window.localStorage.getItem('token');
        var user = getUser();
        if (!token || !user) {
            clearLoginAndRedirect();
            return null;
        }
        if (requiredRole && user.role !== requiredRole) {
            window.location.replace(user.role === 'ADMIN' ? '/admin/pending' : '/classrooms');
            return null;
        }
        return user;
    }

    function fillUser(user, elementId) {
        var element = document.getElementById(elementId || 'userName');
        if (element && user) {
            element.textContent = (user.role === 'ADMIN' ? '管理员' : '学生') + ' · ' + user.name;
        }
    }

    function bindLogout() {
        var logoutLink = document.getElementById('logoutLink');
        if (logoutLink) {
            logoutLink.addEventListener('click', function (event) {
                event.preventDefault();
                clearLoginAndRedirect();
            });
        }
    }

    window.Api = {
        get: function (url, params) {
            return instance.get(url, { params: params });
        },
        post: function (url, data) {
            return instance.post(url, data);
        },
        put: function (url, data) {
            return instance.put(url, data);
        },
        delete: function (url) {
            return instance.delete(url);
        },
        getUser: getUser,
        requireAuth: requireAuth,
        fillUser: fillUser,
        bindLogout: bindLogout,
        logout: clearLoginAndRedirect,
        statusText: statusText,
        statusClass: statusClass,
        escapeHtml: escapeHtml,
        formatDateTime: formatDateTime
    };
})(window);
