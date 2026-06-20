# Tài liệu hướng dẫn mã nguồn Frontend (`app.js`)

File `app.js` là trái tim của giao diện frontend trong dự án của bạn, hoạt động như một ứng dụng một trang (Single Page Application - SPA) thu nhỏ. Dưới đây là giải thích chi tiết về vai trò và cách hoạt động của từng hàm và biến trong file này.

## 1. Trạng thái ứng dụng (State)

```javascript
const state = {
    authHeader: null,
    username: null,
    role: null,
    customerId: null,
};
```
* **Vai trò:** Lưu trữ trạng thái xác thực hiện tại của người dùng trong bộ nhớ tạm của trình duyệt khi đang duyệt web. 
* Khi đăng nhập thành công, token và thông tin người dùng sẽ được lưu vào đây để các hàm khác (như gọi API) có thể lấy ra sử dụng dễ dàng.

## 2. Các hàm Tiện ích (Utilities)

### `showMessage(text, type)`
* **Vai trò:** Hiển thị thông báo (thành công, lỗi) lên màn hình cho người dùng.
* **Tham số:** 
  * `text`: Nội dung câu thông báo.
  * `type`: Loại thông báo (ví dụ: 'success' màu xanh, 'error' màu đỏ).

### `parseJwtPayload(token)`
* **Vai trò:** Giải mã phần ruột (payload) của chuỗi JWT Token mà backend trả về.
* **Cơ chế:** Cắt chuỗi token, dùng hàm `atob()` của JavaScript để giải mã Base64 và chuyển nó thành một đối tượng JSON (Object) để có thể đọc được thông tin (như username, role, customerId) ẩn bên trong.

### `getRoleFromToken(payload)`
* **Vai trò:** Trích xuất quyền (Role) của người dùng từ payload đã giải mã.
* **Cơ chế:** Tìm kiếm các trường như `roles`, `role`, hoặc `authorities`, sau đó loại bỏ tiền tố `ROLE_` (nếu có) để trả về tên quyền thuần túy (như `CUSTOMER`, `STAFF`, `MANAGER`).

### `updateAuthUI()`
* **Vai trò:** Cập nhật lại giao diện thanh điều hướng (Header).
* **Cơ chế:** Nếu người dùng đã đăng nhập (biến `state.authHeader` có dữ liệu), nó sẽ hiện tên, badge Role và nút Đăng xuất. Nếu chưa đăng nhập, nó sẽ ẩn các nút này đi.

### `apiFetch(path, options)`
* **Vai trò:** Đây là hàm cốt lõi dùng để giao tiếp với Backend. Là một lớp bọc (wrapper) an toàn cho hàm `fetch()` mặc định của trình duyệt.
* **Cơ chế:**
  * Tự động thêm thẻ Header `Content-Type: application/json`.
  * Tự động đính kèm Token xác thực (`Authorization`) vào mọi request nếu người dùng đã đăng nhập.
  * Tự động bắt lỗi nếu Backend trả về mã lỗi (như 400, 401, 403, 500) và ném ra Exception để các hàm khác xử lý.

### `createSection(title, content)` & `renderTable(data)`
* **Vai trò:** Các hàm hỗ trợ vẽ HTML (Render).
* `createSection`: Bọc nội dung vào trong một thẻ `<section>` có class `card`.
* `renderTable`: Hàm vẽ bảng siêu việt. Nhận vào một mảng dữ liệu JSON bất kỳ từ Backend, tự động quét các thuộc tính (keys) để làm tiêu đề cột, và trải dữ liệu (values) ra các hàng tương ứng.

## 3. Các hàm Xử lý Luồng (Flow & Views)

### `renderLogin()`
* **Vai trò:** Vẽ giao diện trang Đăng nhập và bắt sự kiện click nút "Đăng nhập".
* **Cơ chế:** 
  * Khi click, nó thu thập username và password.
  * Gọi API `POST /api/auth/login`.
  * Nếu thành công: Lưu token và thông tin vào biến `state` và `localStorage` (để không bị mất khi F5 tải lại trang). Sau đó gọi `updateAuthUI()` và chuyển hướng sang Dashboard.

### `renderDashboard()`
* **Vai trò:** Trạm kiểm soát không lưu (Router).
* **Cơ chế:** Dựa vào `state.role` (CUSTOMER, STAFF, hay MANAGER), nó sẽ quyết định gọi hàm render giao diện nào tương ứng. Nếu không có role, nó tống cổ về lại trang `renderLogin()`.

### `renderCustomerClient()`, `renderStaffClient()`, `renderManagerClient()`
* **Vai trò:** Vẽ giao diện bảng điều khiển dành riêng cho từng Role.
* **Cơ chế chung:**
  * Chứa các nút bấm tương ứng với nghiệp vụ của Role đó (Ví dụ: Khách hàng chỉ có nút "Tải khoản vay của tôi").
  * Gắn sự kiện lắng nghe (Event Listener) vào các nút. Khi bấm, nó sẽ gọi `apiFetch()` đến các endpoint (URL) backend tương ứng, lấy dữ liệu về, dùng `renderTable()` để vẽ thành bảng và nhét vào màn hình.

## 4. Các hàm Khởi tạo (Initialization)

### `restoreAuthState()`
* **Vai trò:** Phục hồi trí nhớ cho trình duyệt.
* **Cơ chế:** Khi người dùng F5 tải lại trang, biến `state` sẽ bị xóa sạch. Hàm này lập tức chui vào `localStorage` (ổ cứng tạm của trình duyệt), lấy lại token và thông tin cũ gán ngược lại vào `state` để giữ trạng thái đăng nhập.

### `init()`
* **Vai trò:** Hàm chạy đầu tiên khi load file `app.js`.
* **Cơ chế:** Chạy hàm `restoreAuthState()`. Nếu thấy có Token hợp lệ thì phi thẳng vào `renderDashboard()`, nếu không thì hiện trang `renderLogin()`.
