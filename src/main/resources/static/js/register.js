(function () {
    'use strict';

    var form = document.getElementById('registerForm');
    var errorEl = document.getElementById('registerError');
    var btn = document.getElementById('registerBtn');

    function showError(message) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var username = document.getElementById('username').value.trim();
        var name = document.getElementById('name').value.trim();
        var password = document.getElementById('password').value;
        var confirmPassword = document.getElementById('confirmPassword').value;
        var department = document.getElementById('department').value.trim();

        if (!username || !name || !password || !department) {
            showError('请填写完整注册信息');
            return;
        }
        if (password.length < 8) {
            showError('密码至少需要8个字符');
            return;
        }
        if (password !== confirmPassword) {
            showError('两次输入的密码不一致');
            return;
        }

        errorEl.style.display = 'none';
        btn.disabled = true;
        btn.textContent = '注册中...';
        Api.post('/auth/register', {
            username: username,
            password: password,
            name: name,
            department: department
        }).then(function () {
            window.alert('[系统提示] 注册成功，请登录');
            window.location.href = '/login';
        }).catch(function () {
            btn.disabled = false;
            btn.textContent = '注册';
        });
    });
})();
