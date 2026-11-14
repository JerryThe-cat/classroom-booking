(function () {
    'use strict';

    var existingUser = Api.getUser();
    if (window.localStorage.getItem('token') && existingUser) {
        window.location.replace(existingUser.role === 'ADMIN' ? '/admin/pending' : '/classrooms');
        return;
    }

    var form = document.getElementById('loginForm');
    var errorEl = document.getElementById('loginError');
    var btn = document.getElementById('loginBtn');

    function showError(message) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var username = document.getElementById('username').value.trim();
        var password = document.getElementById('password').value;
        if (!username || !password) {
            showError('请输入账号和密码');
            return;
        }

        errorEl.style.display = 'none';
        btn.disabled = true;
        btn.textContent = '登录中...';
        Api.post('/auth/login', { username: username, password: password })
            .then(function (data) {
                window.localStorage.setItem('token', data.token);
                window.localStorage.setItem('user', JSON.stringify(data.user));
                window.location.href = data.user.role === 'ADMIN' ? '/admin/pending' : '/classrooms';
            })
            .catch(function () {
                btn.disabled = false;
                btn.textContent = '登录';
            });
    });
})();
