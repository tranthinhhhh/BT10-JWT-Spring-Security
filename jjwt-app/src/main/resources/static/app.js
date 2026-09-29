const tokenKey = "bt10.jwt.token";

function message(text, isError = false) {
    const element = document.querySelector("#message");
    if (!element) return;
    element.textContent = text;
    element.className = `message show${isError ? " error" : ""}`;
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    const token = localStorage.getItem(tokenKey);
    if (token) headers.set("Authorization", `Bearer ${token}`);

    const response = await fetch(path, { ...options, headers });
    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const error = new Error(body.message || `HTTP ${response.status}`);
        error.status = response.status;
        throw error;
    }
    return body;
}

const loginForm = document.querySelector("#login-form");
if (loginForm) {
    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = loginForm.querySelector("button");
        button.disabled = true;
        try {
            const result = await api("/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    email: document.querySelector("#login-email").value,
                    password: document.querySelector("#login-password").value
                })
            });
            localStorage.setItem(tokenKey, result.token);
            window.location.assign("/profile.html");
        } catch (error) {
            message(error.message, true);
        } finally {
            button.disabled = false;
        }
    });
}

const signupForm = document.querySelector("#signup-form");
if (signupForm) {
    signupForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = signupForm.querySelector("button");
        button.disabled = true;
        try {
            await api("/auth/signup", {
                method: "POST",
                body: JSON.stringify({
                    fullName: document.querySelector("#signup-name").value,
                    email: document.querySelector("#signup-email").value,
                    password: document.querySelector("#signup-password").value
                })
            });
            document.querySelector("#login-email").value =
                document.querySelector("#signup-email").value;
            document.querySelector("#login-password").focus();
            signupForm.reset();
            message("Đăng ký thành công. Hãy đăng nhập bằng tài khoản vừa tạo.");
        } catch (error) {
            message(error.message, true);
        } finally {
            button.disabled = false;
        }
    });
}

function renderProfile(user) {
    document.querySelector("#profile-card").innerHTML = `
        <div class="profile-grid">
            <div class="datum"><small>Họ tên</small><strong>${escapeHtml(user.fullName)}</strong></div>
            <div class="datum"><small>Email</small><strong>${escapeHtml(user.email)}</strong></div>
            <div class="datum"><small>Quyền</small><span class="badge">${escapeHtml(user.role)}</span></div>
            <div class="datum"><small>Ngày tạo</small><strong>${new Date(user.createdAt).toLocaleString("vi-VN")}</strong></div>
        </div>`;
}

function renderUsers(users) {
    const rows = users.map((user) => `
        <tr>
            <td>${user.id}</td>
            <td>${escapeHtml(user.fullName)}</td>
            <td>${escapeHtml(user.email)}</td>
            <td>${escapeHtml(user.role)}</td>
        </tr>`).join("");
    document.querySelector("#users-result").innerHTML = `
        <table>
            <thead><tr><th>ID</th><th>Họ tên</th><th>Email</th><th>Quyền</th></tr></thead>
            <tbody>${rows}</tbody>
        </table>`;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

const profileCard = document.querySelector("#profile-card");
if (profileCard) {
    if (!localStorage.getItem(tokenKey)) {
        window.location.replace("/login.html");
    } else {
        api("/users/me")
            .then(renderProfile)
            .catch((error) => {
                if (error.status === 401) localStorage.removeItem(tokenKey);
                message(error.message, true);
                profileCard.innerHTML = '<p class="muted">Không thể tải hồ sơ.</p>';
            });
    }
}

document.querySelector("#load-users")?.addEventListener("click", async (event) => {
    event.currentTarget.disabled = true;
    try {
        renderUsers(await api("/users"));
    } catch (error) {
        message(error.status === 403
            ? "Bạn đã xác thực nhưng không có quyền ADMIN (HTTP 403)."
            : error.message, true);
    } finally {
        event.currentTarget.disabled = false;
    }
});

document.querySelector("#logout")?.addEventListener("click", () => {
    localStorage.removeItem(tokenKey);
    window.location.assign("/login.html");
});
