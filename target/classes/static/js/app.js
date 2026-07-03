/**
 * Expense Management System — app.js
 * Frontend utilities: sidebar toggle, file upload, form validation, method override
 */

// ── Sidebar Toggle ──────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {
    const sidebar  = document.getElementById('sidebar');
    const toggle   = document.getElementById('sidebarToggle');
    const mainArea = document.querySelector('.ems-main');

    if (toggle && sidebar) {
        // Restore desktop collapse state from localStorage
        if (window.innerWidth > 991 && localStorage.getItem('sidebarCollapsed') === 'true') {
            sidebar.classList.add('collapsed');
            if (mainArea) mainArea.classList.add('sidebar-collapsed');
        }

        toggle.addEventListener('click', function () {
            if (window.innerWidth <= 991) {
                // Mobile: slide in/out
                sidebar.classList.toggle('open');
            } else {
                // Desktop: collapse to icon-only
                const isCollapsed = sidebar.classList.toggle('collapsed');
                if (mainArea) mainArea.classList.toggle('sidebar-collapsed', isCollapsed);
                localStorage.setItem('sidebarCollapsed', isCollapsed);
            }
        });
    }

    // Close sidebar on outside click (mobile)
    document.addEventListener('click', function (e) {
        if (sidebar && window.innerWidth <= 991) {
            if (!sidebar.contains(e.target) && toggle && !toggle.contains(e.target)) {
                sidebar.classList.remove('open');
            }
        }
    });

    // Auto-dismiss flash messages after 5 seconds
    const alerts = document.querySelectorAll('.flash-messages .alert');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });

    // Initialize tooltips
    const tooltipEls = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipEls.forEach(function (el) {
        new bootstrap.Tooltip(el);
    });

    // File drag-and-drop enhancement
    initFileDragDrop();

    // Form submission loading state
    initFormLoadingState();

    // Select All checkbox for report form
    initSelectAll();
});

// ── Password Toggle ─────────────────────────────────────────────
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const icon = document.getElementById('eyeIcon-' + inputId);
    if (!input) return;

    if (input.type === 'password') {
        input.type = 'text';
        if (icon) { icon.classList.remove('bi-eye'); icon.classList.add('bi-eye-slash'); }
    } else {
        input.type = 'password';
        if (icon) { icon.classList.remove('bi-eye-slash'); icon.classList.add('bi-eye'); }
    }
}

// ── File Upload Preview ─────────────────────────────────────────
function handleFileSelect(input) {
    const preview = document.getElementById('filePreview');
    const fileNameEl = document.getElementById('fileName');
    const uploadArea = document.getElementById('fileUploadArea');

    if (input.files && input.files[0]) {
        const file = input.files[0];

        // Validate size (5MB)
        if (file.size > 5 * 1024 * 1024) {
            alert('File size exceeds 5MB limit. Please choose a smaller file.');
            input.value = '';
            return;
        }

        // Validate type
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'application/pdf'];
        if (!allowedTypes.includes(file.type)) {
            alert('Invalid file type. Please upload JPEG, PNG, or PDF.');
            input.value = '';
            return;
        }

        if (fileNameEl) fileNameEl.textContent = file.name + ' (' + formatFileSize(file.size) + ')';
        if (preview) preview.classList.remove('d-none');
        if (uploadArea) uploadArea.style.display = 'none';
    }
}

function clearFile() {
    const input = document.getElementById('receiptFile');
    const preview = document.getElementById('filePreview');
    const uploadArea = document.getElementById('fileUploadArea');
    if (input) input.value = '';
    if (preview) preview.classList.add('d-none');
    if (uploadArea) uploadArea.style.display = '';
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function initFileDragDrop() {
    const area = document.getElementById('fileUploadArea');
    if (!area) return;

    ['dragenter', 'dragover'].forEach(function (event) {
        area.addEventListener(event, function (e) {
            e.preventDefault();
            area.classList.add('drag-over');
        });
    });

    ['dragleave', 'drop'].forEach(function (event) {
        area.addEventListener(event, function (e) {
            e.preventDefault();
            area.classList.remove('drag-over');
        });
    });

    area.addEventListener('drop', function (e) {
        const fileInput = document.getElementById('receiptFile');
        if (fileInput && e.dataTransfer.files.length) {
            fileInput.files = e.dataTransfer.files;
            handleFileSelect(fileInput);
        }
    });
}

// ── Form Loading State ──────────────────────────────────────────
function initFormLoadingState() {
    const forms = document.querySelectorAll('form#expenseForm, form#loginForm, form#registerForm');
    forms.forEach(function (form) {
        form.addEventListener('submit', function () {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                const originalText = submitBtn.innerHTML;
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Processing...';
                // Re-enable after 10s as fallback
                setTimeout(function () {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalText;
                }, 10000);
            }
        });
    });
}

// ── Select All for Report Form ──────────────────────────────────
function initSelectAll() {
    const selectAll = document.getElementById('selectAll');
    if (!selectAll) return;

    selectAll.addEventListener('change', function () {
        const checkboxes = document.querySelectorAll('.expense-check');
        checkboxes.forEach(function (cb) {
            cb.checked = selectAll.checked;
        });
    });
}

function toggleAll(source) {
    const checkboxes = document.querySelectorAll('.expense-check');
    checkboxes.forEach(function (cb) {
        cb.checked = source.checked;
    });
}

// ── Amount Formatter ────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {
    const amountInput = document.getElementById('amount');
    if (amountInput) {
        amountInput.addEventListener('blur', function () {
            const val = parseFloat(this.value);
            if (!isNaN(val)) {
                this.value = val.toFixed(2);
            }
        });
    }
});
