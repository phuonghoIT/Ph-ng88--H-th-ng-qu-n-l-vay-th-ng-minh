// Bổ sung CSS cho Modal (cửa sổ pop-up)
const modalCss = `
.modal { display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; overflow: auto; background-color: rgba(0,0,0,0.4); }
.modal-content { background-color: #fefefe; margin: 10% auto; padding: 20px; border: 1px solid #888; width: 80%; max-width: 600px; border-radius: 8px; box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2); }
.modal-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; padding-bottom: 10px; }
.modal-header h2 { margin: 0; }
.close-button { color: #aaa; float: right; font-size: 28px; font-weight: bold; cursor: pointer; }
.close-button:hover, .close-button:focus { color: black; }
.detail-list { list-style: none; padding: 0; }
.detail-list li { padding: 10px 0; display: flex; justify-content: space-between; border-bottom: 1px solid #f0f0f0; }
.detail-list li:last-child { border-bottom: none; }
.detail-list strong { color: #555; }
.role-buttons .button { margin: 0 5px; }
`;
const styleSheet = document.createElement("style");
styleSheet.type = "text/css";
styleSheet.innerText = modalCss;
document.head.appendChild(styleSheet);


const API_BASE = window.location.origin === 'null' ? 'http://localhost:8080' : '';

const state = {
    authHeader: null,
    username: null,
    role: null,
    customerId: null,
    loanProducts: [],
    customerLoans: [],
    allLoans: [],
};

const view = document.getElementById('view');
const messageEl = document.getElementById('message');
const userBar = document.getElementById('userBar');
const userLabel = document.getElementById('userLabel');
const roleBadge = document.getElementById('roleBadge');
const logoutBtn = document.getElementById('logoutBtn');

// Hàm hiển thị Modal
function showModal(title, content) {
    let modal = document.getElementById('detailModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'detailModal';
        modal.className = 'modal';
        document.body.appendChild(modal);
    }
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h2>${title}</h2>
                <span class="close-button">&times;</span>
            </div>
            <div class="modal-body">
                ${content}
            </div>
        </div>
    `;
    modal.style.display = 'block';

    modal.querySelector('.close-button').onclick = () => {
        modal.style.display = 'none';
    };
    window.onclick = (event) => {
        if (event.target == modal) {
            modal.style.display = 'none';
        }
    };
}


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
                <button id="showRegisterBtn" class="button secondary">Đăng ký tài khoản mới</button>
            </div>
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

    document.getElementById('showRegisterBtn').addEventListener('click', renderRegister);
}

// HÀM MỚI: Vẽ giao diện đăng ký
function renderRegister() {
    showMessage('Vui lòng chọn vai trò bạn muốn đăng ký.', 'info');
    view.innerHTML = `
        <div class="card">
            <h1>Đăng ký tài khoản</h1>
            <p>Bạn là?</p>
            <div class="action-bar role-buttons">
                <button id="regCustomerBtn" class="button">Khách hàng</button>
                <button id="regStaffBtn" class="button">Nhân viên</button>
                <button id="regManagerBtn" class="button">Quản lý</button>
            </div>
            <div id="registerFormContainer" class="stack" style="margin-top: 20px;"></div>
            <div class="action-bar" style="margin-top: 20px;">
                <button id="backToLoginBtn" class="button secondary">Quay lại Đăng nhập</button>
            </div>
        </div>
    `;

    document.getElementById('regCustomerBtn').addEventListener('click', () => renderCustomerRegisterForm());
    document.getElementById('regStaffBtn').addEventListener('click', () => renderEmployeeRegisterForm('STAFF'));
    document.getElementById('regManagerBtn').addEventListener('click', () => renderEmployeeRegisterForm('MANAGER'));
    document.getElementById('backToLoginBtn').addEventListener('click', renderLogin);
}

// HÀM MỚI: Form đăng ký cho Customer và xử lý gửi dữ liệu
function renderCustomerRegisterForm() {
    const formContainer = document.getElementById('registerFormContainer');
    formContainer.innerHTML = `
        <h3>Thông tin Khách hàng</h3>
        <form id="customerRegisterForm" class="stack grid-two">
            <label>Họ và tên <input type="text" id="regFullName" required /></label>
            <label>Số CCCD <input type="text" id="regIdentityNumber" required /></label>
            <label>Địa chỉ <input type="text" id="regAddress" /></label>
            <label>Số điện thoại <input type="text" id="regSdt" /></label>
            <label>Nghề nghiệp <input type="text" id="regJob" /></label>
            <div></div>
            <label>Tên đăng nhập <input type="text" id="regUsername" required /></label>
            <label>Mật khẩu <input type="password" id="regPassword" required /></label>
        </form>
        <div class="action-bar">
            <button id="submitCustomerReg" class="button">Hoàn tất Đăng ký</button>
        </div>
    `;

    // Gắn sự kiện để gửi dữ liệu xuống Back-end
    document.getElementById('submitCustomerReg').addEventListener('click', async (e) => {
        e.preventDefault();
        const fullName = document.getElementById('regFullName').value.trim();
        const identityNumber = document.getElementById('regIdentityNumber').value.trim();
        const address = document.getElementById('regAddress').value.trim();
        const sdt = document.getElementById('regSdt').value.trim();
        const job = document.getElementById('regJob').value.trim();
        const username = document.getElementById('regUsername').value.trim();
        const password = document.getElementById('regPassword').value.trim();

        if (!fullName || !identityNumber || !username || !password) {
            showMessage('Vui lòng điền đầy đủ các thông tin bắt buộc (Họ tên, CCCD, Username, Password).', 'error');
            return;
        }

        // Đóng gói JSON đúng chuẩn Back-end yêu cầu
        const payload = {
            fullName,
            identityNumber,
            address,
            sdt,
            job,
            user: { username, password }
        };

        try {
            await apiFetch('/api/customers', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            showMessage('Đăng ký Khách hàng thành công! Vui lòng đăng nhập.', 'success');
            renderLogin();
        } catch (error) {
            showMessage(error.message || 'Đăng ký thất bại.', 'error');
        }
    });
}

// HÀM MỚI: Form đăng ký cho Employee và xử lý gửi dữ liệu
async function renderEmployeeRegisterForm(role) {
    const formContainer = document.getElementById('registerFormContainer');
    formContainer.innerHTML = `<p>Đang tải danh sách chi nhánh...</p>`;

    try {
        const branches = await apiFetch('/api/branches');
        const branchOptions = Array.isArray(branches)
            ? branches.map(b => `<option value="${b.branchId}">${b.branchName}</option>`).join('')
            : '';

        formContainer.innerHTML = `
            <h3>Thông tin ${role === 'STAFF' ? 'Nhân viên' : 'Quản lý'}</h3>
            <form id="employeeRegisterForm" class="stack grid-two">
                <input type="hidden" id="regRole" value="${role}" />
                <label>Họ và tên <input type="text" id="regFullName" required /></label>
                <label>Chi nhánh
                    <select id="regBranchId" required>
                        <option value="">Chọn chi nhánh</option>
                        ${branchOptions}
                    </select>
                </label>
                <label>Tên đăng nhập <input type="text" id="regUsername" required /></label>
                <label>Mật khẩu <input type="password" id="regPassword" required /></label>
            </form>
            <div class="action-bar">
                <button id="submitEmployeeReg" class="button">Hoàn tất Đăng ký</button>
            </div>
        `;

        // Gắn sự kiện để gửi dữ liệu xuống Back-end
        document.getElementById('submitEmployeeReg').addEventListener('click', async (e) => {
            e.preventDefault();
            const role = document.getElementById('regRole').value;
            const fullName = document.getElementById('regFullName').value.trim();
            const branchId = document.getElementById('regBranchId').value;
            const username = document.getElementById('regUsername').value.trim();
            const password = document.getElementById('regPassword').value.trim();

            if (!fullName || !branchId || !username || !password) {
                showMessage('Vui lòng điền đầy đủ thông tin bắt buộc.', 'error');
                return;
            }

            // Đóng gói JSON đúng chuẩn Back-end yêu cầu
            const payload = {
                fullName,
                role,
                branch: { branchId: Number(branchId) },
                user: { username, password }
            };

            try {
                await apiFetch('/api/employees', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showMessage(`Đăng ký ${role} thành công! Vui lòng đăng nhập.`, 'success');
                renderLogin();
            } catch (error) {
                showMessage(error.message || 'Đăng ký thất bại.', 'error');
            }
        });
    } catch (error) {
        formContainer.innerHTML = `<p class="error">Lỗi khi tải danh sách chi nhánh. Vui lòng thử lại.</p>`;
        showMessage(error.message, 'error');
    }
}


function renderDashboard() {
    if (!state.role) {
        renderLogin();
        return;
    }
    showMessage(`Chúng tôi nhận diện role của bạn là ${state.role}.`, 'success');
    if (state.role === 'CUSTOMER') {
        renderCustomerClient();
    } else if (state.role === 'STAFF' || state.role === 'MANAGER') {
        renderStaffClient();
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
                        <label class="input-with-button">
                            <span>Kỳ thanh toán</span>
                            <div style="display: flex; align-items: center;">
                                <select id="paymentScheduleSelect" style="flex-grow: 1;"></select>
                                <button id="viewScheduleDetailBtn" class="button secondary small" style="margin-left: 8px; white-space: nowrap;">Xem chi tiết</button>
                            </div>
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

    document.getElementById('viewScheduleDetailBtn').addEventListener('click', async () => {
        const scheduleId = document.getElementById('paymentScheduleSelect').value;
        if (!scheduleId) {
            showMessage('Vui lòng chọn một kỳ thanh toán để xem chi tiết.', 'error');
            return;
        }
        try {
            showMessage('Đang tải chi tiết...');
            const details = await apiFetch(`/api/repayment-schedules/${scheduleId}/detail`);
            const remainingAmount = details.requiredAmount - details.totalPaid;
            const content = `
                <ul class="detail-list">
                    <li><strong>Kỳ số:</strong> <span>${details.periodNumber}</span></li>
                    <li><strong>Ngày đáo hạn:</strong> <span>${details.dueDate}</span></li>
                    <li><strong>Trạng thái:</strong> <span>${details.status}</span></li>
                    <hr>
                    <li><strong>Tiền gốc phải trả:</strong> <span>${details.principalAmount.toLocaleString('vi-VN')} đ</span></li>
                    <li><strong>Tiền lãi phải trả:</strong> <span>${details.interestAmount.toLocaleString('vi-VN')} đ</span></li>
                    <li><strong>Tiền phạt (nếu có):</strong> <span>${details.penaltyAmount.toLocaleString('vi-VN')} đ</span></li>
                    <hr>
                    <li><strong>TỔNG CỘNG CẦN TRẢ:</strong> <strong>${details.requiredAmount.toLocaleString('vi-VN')} đ</strong></li>
                    <li><strong>Số tiền đã trả:</strong> <span>${details.totalPaid.toLocaleString('vi-VN')} đ</span></li>
                    <li><strong>SỐ TIỀN CÒN LẠI:</strong> <strong>${remainingAmount > 0 ? remainingAmount.toLocaleString('vi-VN') : 0} đ</strong></li>
                </ul>
            `;
            showModal('Chi tiết kỳ thanh toán', content);
            showMessage('');
        } catch (error) {
            showMessage(error.message || 'Không thể tải chi tiết kỳ thanh toán.', 'error');
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
            <p>Nhân viên có thể xem khoản vay, duyệt khoản vay và đăng ký tài sản thế chấp.</p>
            <div class="grid-three">
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
                <div class="card inner-card">
                    <h3>Đăng ký Tài sản Thế chấp</h3>
                    <form id="collateralForm" class="stack">
                        <label>Khoản vay (Thế chấp)
                            <select id="collateralLoanSelect" required></select>
                        </label>
                        <label>Loại tài sản
                            <input id="assetType" type="text" placeholder="Ví dụ: Sổ đỏ, Xe ô tô" required />
                        </label>
                        <label>Giá trị ước tính
                            <input id="estimatedValue" type="number" min="1000" required />
                        </label>
                        <label>Tỷ lệ chuyển đổi
                            <input id="conversionRate" type="number" min="0.1" max="1" step="0.1" placeholder="Ví dụ: 0.7" />
                        </label>
                        <button class="button" type="submit">Đăng ký tài sản</button>
                    </form>
                </div>
            </div>
        `)}
    `;

    document.getElementById('loadLoans').addEventListener('click', async () => {
        const content = document.getElementById('staffContent');
        content.innerHTML = '<p>Đang tải khoản vay...</p>';
        try {
            const data = await apiFetch('/api/loans');
            state.allLoans = Array.isArray(data) ? data : [];
            content.innerHTML = renderTable(state.allLoans);

            const pendingLoans = state.allLoans.filter(loan => loan.status === 'PENDING');
            const pendingLoanSelect = document.getElementById('pendingLoanSelect');
            pendingLoanSelect.innerHTML = pendingLoans.map(loan => `<option value="${loan.loanId}">${loan.loanId} - ${loan.amount} - ${loan.customer?.fullName || 'Khách hàng'}</option>`).join('');

            const collateralLoans = state.allLoans.filter(loan => loan.loanType === 'THE_CHAP' && loan.status === 'PENDING');
            const collateralLoanSelect = document.getElementById('collateralLoanSelect');
            collateralLoanSelect.innerHTML = `<option value="">Chọn khoản vay</option>${collateralLoans.map(loan => `<option value="${loan.loanId}">${loan.loanId} - ${loan.customer?.fullName || 'Khách hàng'}</option>`).join('')}`;

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

    document.getElementById('collateralForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        const loanId = Number(document.getElementById('collateralLoanSelect').value);
        const assetType = document.getElementById('assetType').value;
        const estimatedValue = Number(document.getElementById('estimatedValue').value);
        const conversionRate = Number(document.getElementById('conversionRate').value);

        if (!loanId || !assetType || !estimatedValue) {
            showMessage('Vui lòng nhập đầy đủ thông tin tài sản thế chấp.', 'error');
            return;
        }

        try {
            await apiFetch('/api/collaterals', {
                method: 'POST',
                body: JSON.stringify({
                    assetType,
                    estimatedValue,
                    conversionRate: conversionRate || null,
                    loan: { loanId: loanId }
                }),
            });
            showMessage('Đăng ký tài sản thế chấp thành công!', 'success');
            document.getElementById('collateralForm').reset();
        } catch (error) {
            showMessage(error.message || 'Đăng ký tài sản thất bại.', 'error');
        }
    });

    document.getElementById('loadLoans').click();
}

function renderManagerClient() {
    renderStaffClient();
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
