const API_BASE = window.location.origin === 'null' ? 'http://localhost:8080' : '';

const state = {
    authHeader: null,
    username: null,
    role: null,
    customerId: null,
};

const view = document.getElementById('view');
const messageEl = document.getElementById('message');
const userBar = document.getElementById('userBar');
const userLabel = document.getElementById('userLabel');
const roleBadge = document.getElementById('roleBadge');
const logoutBtn = document.getElementById('logoutBtn');

logoutBtn.addEventListener('click', () => {
    localStorage.clear();
    state.authHeader = null;
    state.role = null;
    state.username = null;
    state.customerId = null;
    updateAuthUI();
    renderLogin();
});

function showMessage(text, type = 'success') {
    if (!text) {
        messageEl.className = 'message hidden';
        messageEl.textContent = '';
        return;
    }
    messageEl.textContent = text;
    messageEl.className = `message ${type}`;
}

function renderFetchError(content, error, summary = 'Lỗi khi tải dữ liệu.') {
    const status = error.status ? ` (${error.status} ${error.statusText || ''})` : '';
    const url = error.url ? `\nURL: ${error.url}` : '';
    const detail = `${error.message || 'Unknown error'}${status}${url}`;
    content.innerHTML = `
        <p>${summary}</p>
        <pre class="error-detail">${detail}</pre>
    `;
    showMessage(detail, 'error');
}

function parseJwtPayload(token) {
    try {
        const payload = token.split('.')[1];
        const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
        const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
        const decoded = atob(padded);
        return JSON.parse(decoded);
    } catch (e) {
        return null;
    }
}

function getRoleFromToken(payload) {
    if (!payload) return null;
    const roles = payload.roles || payload.role || payload.authorities;
    if (!roles) return null;
    const raw = Array.isArray(roles) ? roles : `${roles}`;
    const first = raw.split(',')[0].trim();
    return first.replace(/^ROLE_/, '');
}

function updateAuthUI() {
    if (state.authHeader) {
        userBar.classList.remove('hidden');
        roleBadge.classList.remove('hidden');
        userLabel.textContent = `Xin chào, ${state.username}`;
        roleBadge.textContent = state.role || 'USER';
    } else {
        userBar.classList.add('hidden');
        roleBadge.classList.add('hidden');
    }
}

async function apiFetch(path, options = {}) {
    const headers = { ...options.headers };
    const method = (options.method || 'GET').toUpperCase();
    const hasBody = options.body != null;

    if (hasBody && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }
    if (state.authHeader) {
        headers['Authorization'] = state.authHeader;
    }

    const url = path.startsWith('/') ? `${API_BASE}${path}` : path;
    console.debug('apiFetch request', { 
        method, 
        url, 
        hasAuth: !!state.authHeader,
        authHeaderPresent: !!headers['Authorization'],
        authHeaderValue: headers['Authorization'] ? headers['Authorization'].substring(0, 20) + '...' : 'NONE',
        body: options.body, 
        origin: window.location.origin 
    });

    try {
        const response = await fetch(url, {
            ...options,
            headers,
            mode: 'cors',
        });
        console.debug('apiFetch response', { url, status: response.status, statusText: response.statusText });
        
        if (!response.ok) {
            const errorText = await response.text();
            const message = errorText || `${response.status} ${response.statusText}`;
            console.error('apiFetch failed response', { url, status: response.status, statusText: response.statusText, responseBody: errorText });
            const fetchError = new Error(message);
            fetchError.status = response.status;
            fetchError.statusText = response.statusText;
            fetchError.url = url;
            fetchError.method = method;
            throw fetchError;
        }
        return response.json();
    } catch (error) {
        console.error('apiFetch failed', { url, method, error });
        if (!(error instanceof Error)) {
            error = new Error(String(error));
        }
        error.url = error.url || url;
        error.method = error.method || method;
        throw error;
    }
}

function renderLogin() {
    showMessage('Hãy đăng nhập bằng tài khoản hiện có của bạn.', 'success');
    view.innerHTML = `
        <div class="card">
            <h1>Đăng nhập</h1>
            <div class="input-group">
                <label>Username<input id="username" type="text" placeholder="Tên đăng nhập" /></label>
                <label>Password<input id="password" type="password" placeholder="Mật khẩu" /></label>
            </div>
            <div class="action-bar">
                <button id="loginBtn" class="button">Đăng nhập</button>
            </div>
            <p>Nếu chưa có tài khoản, backend hiện hỗ trợ đăng ký tại <code>/api/auth/register</code>.</p>
        </div>
    `;

    document.getElementById('loginBtn').addEventListener('click', async () => {
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value.trim();
        if (!username || !password) {
            showMessage('Vui lòng nhập username và password.', 'error');
            return;
        }

        try {
            const result = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
            });
            if (!result.ok) {
                const errorText = await result.text();
                throw new Error(errorText || 'Đăng nhập thất bại');
            }
            const data = await result.json();
            const payload = parseJwtPayload(data.accessToken);

            state.authHeader = `${data.tokenType} ${data.accessToken}`;
            state.username = payload.sub;
            state.role = getRoleFromToken(payload);
            state.customerId = payload.customerId;

            console.debug('Token payload:', payload);
            console.debug('Extracted role:', state.role);
            console.debug('State after login:', { username: state.username, role: state.role, customerId: state.customerId });

            localStorage.setItem('authHeader', state.authHeader);
            localStorage.setItem('username', state.username);
            localStorage.setItem('role', state.role);
            localStorage.setItem('customerId', state.customerId);

            updateAuthUI();
            showMessage('Đăng nhập thành công!', 'success');
            renderDashboard();
        } catch (error) {
            showMessage(error.message || 'Đăng nhập không thành công.', 'error');
        }
    });
}

function renderDashboard() {
    if (!state.role) {
        renderLogin();
        return;
    }
    showMessage(`Chúng tôi nhận diện role của bạn là ${state.role}.`, 'success');
    if (state.role === 'CUSTOMER') {
        renderCustomerClient();
    } else if (state.role === 'STAFF') {
        renderStaffClient();
    } else if (state.role === 'MANAGER') {
        renderManagerClient();
    } else {
        view.innerHTML = `<div class="card"><h1>Role không xác định</h1><p>Token hiện chưa được giải mã chính xác.</p></div>`;
    }
}

function createSection(title, content) {
    return `
        <section class="card">
            <h2>${title}</h2>
            ${content}
        </section>
    `;
}

function renderTable(data) {
    if (!Array.isArray(data) || data.length === 0) {
        return '<p>Không có dữ liệu.</p>';
    }
    const columns = Array.from(new Set(data.flatMap(item => Object.keys(item))));
    const rows = data.map(item => {
        const cells = columns.map(key => {
            const value = item[key];
            const display = typeof value === 'object' && value !== null ? JSON.stringify(value) : value;
            return `<td>${display ?? ''}</td>`;
        });
        return `<tr>${cells.join('')}</tr>`;
    });
    const header = columns.map(col => `<th>${col}</th>`).join('');
    return `<div class="table-wrapper"><table><thead><tr>${header}</tr></thead><tbody>${rows.join('')}</tbody></table></div>`;
}

function renderCustomerClient() {
    view.innerHTML = `
        ${createSection('Trang khách hàng', `
            <p>Khách hàng có thể xem các khoản vay hiện tại của mình.</p>
            <div class="action-bar">
                <button class="button" id="loadMyLoans">Tải khoản vay của tôi</button>
            </div>
            <div id="customerContent"></div>
        `)}
    `;

    document.getElementById('loadMyLoans').addEventListener('click', async () => {
        const content = document.getElementById('customerContent');
        content.innerHTML = '<p>Đang tải...</p>';
        try {
            // No need to pass customerId, backend will get it from the token
            const loans = await apiFetch('/api/loans/my-loans');
            content.innerHTML = renderTable(loans);
            showMessage('Đã tải danh sách khoản vay của bạn.', 'success');
        } catch (error) {
            renderFetchError(content, error, 'Lỗi khi tải khoản vay.');
        }
    });
}

function renderStaffClient() {
    view.innerHTML = `
        ${createSection('Trang nhân viên (Staff)', `
            <p>Staff có thể xem danh sách tài sản thế chấp, khoản vay và thanh toán.</p>
            <div class="action-bar">
                <button class="button" id="loadCollaterals">Tải tài sản thế chấp</button>
                <button class="button" id="loadLoans">Tải khoản vay</button>
                <button class="button" id="loadPayments">Tải thanh toán</button>
            </div>
            <div id="staffContent"></div>
        `)}
    `;

    document.getElementById('loadCollaterals').addEventListener('click', async () => {
        const content = document.getElementById('staffContent');
        content.innerHTML = '<p>Đang tải tài sản thế chấp...</p>';
        try {
            const data = await apiFetch('/api/collaterals');
            content.innerHTML = renderTable(data);
            showMessage('Đã tải danh sách tài sản thế chấp.', 'success');
        } catch (error) {
            content.innerHTML = '<p>Lỗi khi tải tài sản.</p>';
            showMessage(error.message, 'error');
        }
    });

    document.getElementById('loadLoans').addEventListener('click', async () => {
        const content = document.getElementById('staffContent');
        content.innerHTML = '<p>Đang tải khoản vay...</p>';
        try {
            const data = await apiFetch('/api/loans');
            content.innerHTML = renderTable(data);
            showMessage('Đã tải danh sách khoản vay.', 'success');
        } catch (error) {
            content.innerHTML = '<p>Lỗi khi tải khoản vay.</p>';
            showMessage(error.message, 'error');
        }
    });

    document.getElementById('loadPayments').addEventListener('click', async () => {
        const content = document.getElementById('staffContent');
        content.innerHTML = '<p>Đang tải thanh toán...</p>';
        try {
            const data = await apiFetch('/api/payments');
            content.innerHTML = renderTable(data);
            showMessage('Đã tải danh sách thanh toán.', 'success');
        } catch (error) {
            content.innerHTML = '<p>Lỗi khi tải thanh toán.</p>';
            showMessage(error.message, 'error');
        }
    });
}

function renderManagerClient() {
    view.innerHTML = `
        ${createSection('Trang quản lý (Manager)', `
            <p>Manager có thể xem sản phẩm vay và chi nhánh.</p>
            <div class="action-bar">
                <button class="button" id="loadLoanProducts">Tải sản phẩm vay</button>
                <button class="button" id="loadBranches">Tải chi nhánh</button>
            </div>
            <div id="managerContent"></div>
        `)}
    `;

    document.getElementById('loadLoanProducts').addEventListener('click', async () => {
        const content = document.getElementById('managerContent');
        content.innerHTML = '<p>Đang tải sản phẩm vay...</p>';
        try {
            const data = await apiFetch('/api/loan-products');
            content.innerHTML = renderTable(data);
            showMessage('Đã tải danh sách sản phẩm vay.', 'success');
        } catch (error) {
            content.innerHTML = '<p>Lỗi khi tải sản phẩm vay.</p>';
            showMessage(error.message, 'error');
        }
    });

    document.getElementById('loadBranches').addEventListener('click', async () => {
        const content = document.getElementById('managerContent');
        content.innerHTML = '<p>Đang tải chi nhánh...</p>';
        try {
            const data = await apiFetch('/api/branches');
            content.innerHTML = renderTable(data);
            showMessage('Đã tải danh sách chi nhánh.', 'success');
        } catch (error) {
            content.innerHTML = '<p>Lỗi khi tải chi nhánh.</p>';
            showMessage(error.message, 'error');
        }
    });
}

function restoreAuthState() {
    const authHeader = localStorage.getItem('authHeader');
    const username = localStorage.getItem('username');
    const role = localStorage.getItem('role');
    const customerId = localStorage.getItem('customerId');

    if (authHeader && username && role) {
        state.authHeader = authHeader;
        state.username = username;
        state.role = role;
        state.customerId = customerId;
    }
    updateAuthUI();
}

function init() {
    restoreAuthState();
    if (state.authHeader && state.role) {
        renderDashboard();
    } else {
        renderLogin();
    }
}

init();
