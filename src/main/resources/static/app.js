const API_BASE = window.location.origin === 'null' ? 'http://localhost:8080' : '';

const state = {
    authHeader: null,
    username: null,
    role: null,
    customerId: null,
    loanProducts: [],
    customerLoans: [],
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
    state.loanProducts = [];
    state.customerLoans = [];
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

    try {
        const response = await fetch(url, {
            ...options,
            headers,
            mode: 'cors',
        });

        if (!response.ok) {
            const errorText = await response.text();
            const message = errorText || `${response.status} ${response.statusText}`;
            const fetchError = new Error(message);
            fetchError.status = response.status;
            fetchError.statusText = response.statusText;
            fetchError.url = url;
            fetchError.method = method;
            throw fetchError;
        }

        const text = await response.text();
        if (!text) {
            return null;
        }
        try {
            return JSON.parse(text);
        } catch {
            return text;
        }
    } catch (error) {
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
        view.innerHTML = '<div class="card"><h1>Role không xác định</h1><p>Token hiện chưa được giải mã chính xác.</p></div>';
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
    const rowsData = Array.isArray(data) ? data : data ? [data] : [];
    if (rowsData.length === 0) {
        return '<p>Không có dữ liệu.</p>';
    }
    const columns = Array.from(new Set(rowsData.flatMap(item => Object.keys(item))));
    const rows = rowsData.map(item => {
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
            <p>Khách hàng có thể đăng ký khoản vay mới, đóng tiền theo kỳ hạn và xem danh sách khoản vay.</p>
            <div class="grid-two">
                <div class="card inner-card">
                    <h3>Đăng ký khoản vay</h3>
                    <form id="loanForm" class="stack">
                        <label>Số tiền vay
                            <input id="loanAmount" type="number" min="100000" step="100000" required />
                        </label>
                        <label>Ngày vay
                            <input id="loanDate" type="date" required />
                        </label>
                        <label>Loại vay
                            <select id="loanType">
                                <option value="TIN_CHAP">Tín chấp</option>
                                <option value="THE_CHAP">Thế chấp</option>
                            </select>
                        </label>
                        <label>Gói vay
                            <select id="loanProductSelect" required></select>
                        </label>
                        <button class="button" type="submit">Đăng ký khoản vay</button>
                    </form>
                </div>
                <div class="card inner-card">
                    <h3>Đóng tiền khoản vay</h3>
                    <div class="stack">
                        <label>Khoản vay
                            <select id="paymentLoanSelect"></select>
                        </label>
                        <label>Kỳ thanh toán
                            <select id="paymentScheduleSelect"></select>
                        </label>
                        <label>Số tiền
                            <input id="paymentAmount" type="number" min="1000" step="1000" required />
                        </label>
                        <label>Phương thức
                            <select id="paymentMethod">
                                <option value="Tiền mặt">Tiền mặt</option>
                                <option value="Chuyển khoản">Chuyển khoản</option>
                            </select>
                        </label>
                        <label>Ngày thanh toán
                            <input id="paymentDate" type="date" required />
                        </label>
                        <button class="button" id="payLoanBtn">Đóng tiền</button>
                    </div>
                </div>
            </div>
            <div class="card inner-card">
                <div class="action-bar">
                    <h3 style="margin:0">Khoản vay của tôi</h3>
                    <button class="button secondary" id="refreshCustomerLoans">Tải lại</button>
                </div>
                <div id="customerContent"></div>
            </div>
        `)}
    `;

    const today = new Date().toISOString().slice(0, 10);
    document.getElementById('loanDate').value = today;
    document.getElementById('paymentDate').value = today;

    document.getElementById('loanForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        const amount = Number(document.getElementById('loanAmount').value);
        const loanDate = document.getElementById('loanDate').value;
        const loanType = document.getElementById('loanType').value;
        const selectedProductId = Number(document.getElementById('loanProductSelect').value);

        if (!selectedProductId || !amount || !loanDate) {
            showMessage('Vui lòng nhập đầy đủ thông tin khoản vay.', 'error');
            return;
        }

        try {
            await apiFetch('/api/loans', {
                method: 'POST',
                body: JSON.stringify({
                    amount,
                    loanDate,
                    loanType,
                    loanProduct: { loanProductId: selectedProductId },
                }),
            });
            showMessage('Đăng ký khoản vay thành công!', 'success');
            await loadCustomerData();
        } catch (error) {
            showMessage(error.message || 'Đăng ký khoản vay thất bại.', 'error');
        }
    });

    document.getElementById('refreshCustomerLoans').addEventListener('click', loadCustomerData);
    document.getElementById('paymentLoanSelect').addEventListener('change', async (event) => {
        const loanId = event.target.value;
        const scheduleSelect = document.getElementById('paymentScheduleSelect');
        scheduleSelect.innerHTML = '<option value="">Chọn kỳ hạn</option>';
        if (!loanId) {
            return;
        }
        try {
            const schedules = await apiFetch(`/api/repayment-schedules?loanId=${loanId}`);
            const list = Array.isArray(schedules) ? schedules : [];
            scheduleSelect.innerHTML = `<option value="">Chọn kỳ hạn</option>${list.map(schedule => `<option value="${schedule.scheduleId}">${schedule.periodNumber} - ${schedule.dueDate} - ${schedule.status}</option>`).join('')}`;
        } catch (error) {
            scheduleSelect.innerHTML = '<option value="">Không thể tải kỳ hạn</option>';
            showMessage(error.message || 'Không thể tải lịch thanh toán.', 'error');
        }
    });

    document.getElementById('payLoanBtn').addEventListener('click', async () => {
        const scheduleId = Number(document.getElementById('paymentScheduleSelect').value);
        const amount = Number(document.getElementById('paymentAmount').value);
        const paymentDate = document.getElementById('paymentDate').value;
        const paymentMethod = document.getElementById('paymentMethod').value;

        if (!scheduleId || !amount || !paymentDate) {
            showMessage('Vui lòng chọn kỳ hạn và nhập số tiền.', 'error');
            return;
        }

        try {
            await apiFetch('/api/payments', {
                method: 'POST',
                body: JSON.stringify({
                    amount,
                    paymentDate,
                    paymentMethod,
                    repaymentSchedule: { scheduleId },
                }),
            });
            showMessage('Đóng tiền thành công!', 'success');
            await loadCustomerData();
        } catch (error) {
            showMessage(error.message || 'Đóng tiền thất bại.', 'error');
        }
    });

    loadCustomerData();
}

async function loadCustomerData() {
    try {
        const loanProductSelect = document.getElementById('loanProductSelect');
        if (loanProductSelect) {
            try {
                const products = await apiFetch('/api/loan-products');
                state.loanProducts = Array.isArray(products) ? products : [];
                loanProductSelect.innerHTML = state.loanProducts.map(product => `<option value="${product.loanProductId}">${product.name || product.productName || product.loanProductId}</option>`).join('');
                loanProductSelect.disabled = false;
            } catch (error) {
                state.loanProducts = [];
                loanProductSelect.innerHTML = '<option value="">Không thể tải gói vay</option>';
                loanProductSelect.disabled = true;
            }
        }

        if (loanProductSelect && !loanProductSelect.options.length) {
            loanProductSelect.innerHTML = '<option value="">Chưa có gói vay</option>';
            loanProductSelect.disabled = true;
        }

        const loans = await apiFetch('/api/loans/my-loans');
        state.customerLoans = Array.isArray(loans) ? loans : [];
        const customerContent = document.getElementById('customerContent');
        if (customerContent) {
            customerContent.innerHTML = renderTable(state.customerLoans);
        }
        const paymentLoanSelect = document.getElementById('paymentLoanSelect');
        if (paymentLoanSelect) {
            paymentLoanSelect.innerHTML = `<option value="">Chọn khoản vay</option>${state.customerLoans.map(loan => `<option value="${loan.loanId}">${loan.loanId} - ${loan.amount} - ${loan.status}</option>`).join('')}`;
        }
    } catch (error) {
        const customerContent = document.getElementById('customerContent');
        if (customerContent) {
            renderFetchError(customerContent, error, 'Lỗi khi tải dữ liệu khách hàng.');
        }
    }
}

function renderStaffClient() {
    view.innerHTML = `
        ${createSection('Trang nhân viên (Staff)', `
            <p>Nhân viên có thể xem khoản vay hiện có và duyệt các khoản vay đang chờ phê duyệt.</p>
            <div class="grid-two">
                <div class="card inner-card">
                    <h3>Danh sách khoản vay</h3>
                    <div class="action-bar">
                        <button class="button" id="loadLoans">Tải khoản vay</button>
                        <button class="button" id="loadPayments">Tải thanh toán</button>
                    </div>
                    <div id="staffContent"></div>
                </div>
                <div class="card inner-card">
                    <h3>Duyệt khoản vay</h3>
                    <div class="stack">
                        <label>Khoản vay chờ duyệt
                            <select id="pendingLoanSelect"></select>
                        </label>
                        <button class="button" id="approveLoanBtn">Duyệt khoản vay</button>
                    </div>
                </div>
            </div>
        `)}
    `;

    document.getElementById('loadLoans').addEventListener('click', async () => {
        const content = document.getElementById('staffContent');
        content.innerHTML = '<p>Đang tải khoản vay...</p>';
        try {
            const data = await apiFetch('/api/loans');
            content.innerHTML = renderTable(data);
            const pendingLoans = Array.isArray(data) ? data.filter(loan => loan.status === 'PENDING') : [];
            const pendingLoanSelect = document.getElementById('pendingLoanSelect');
            pendingLoanSelect.innerHTML = pendingLoans.map(loan => `<option value="${loan.loanId}">${loan.loanId} - ${loan.amount} - ${loan.customer?.fullName || 'Khách hàng'}</option>`).join('');
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

    document.getElementById('approveLoanBtn').addEventListener('click', async () => {
        const loanId = document.getElementById('pendingLoanSelect').value;
        if (!loanId) {
            showMessage('Không có khoản vay nào để duyệt.', 'error');
            return;
        }
        try {
            await apiFetch(`/api/loans/${loanId}/status?status=ACTIVE`, { method: 'PATCH' });
            showMessage('Đã duyệt khoản vay thành công.', 'success');
            document.getElementById('loadLoans').click();
        } catch (error) {
            showMessage(error.message || 'Duyệt khoản vay thất bại.', 'error');
        }
    });

    document.getElementById('loadLoans').click();
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
