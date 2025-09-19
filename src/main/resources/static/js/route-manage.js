// 线路管理JavaScript功能
class RouteManager {
    constructor() {
        this.routes = [];
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadRoutes();
    }

    bindEvents() {
        // 新增线路按钮
        document.getElementById('addRouteBtn').addEventListener('click', () => {
            this.showAddModal();
        });

        // 刷新按钮
        document.getElementById('refreshBtn').addEventListener('click', () => {
            this.refreshData();
        });

        // 关闭模态框
        document.getElementById('closeModalBtn').addEventListener('click', () => {
            this.hideAddModal();
        });

        document.getElementById('cancelBtn').addEventListener('click', () => {
            this.hideAddModal();
        });

        // 关闭头像上传模态框
        document.getElementById('closeAvatarModalBtn').addEventListener('click', () => {
            this.hideAvatarModal();
        });

        document.getElementById('cancelAvatarBtn').addEventListener('click', () => {
            this.hideAvatarModal();
        });

        // 新增线路表单提交
        document.getElementById('addRouteForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleAddRoute();
        });

        // 头像上传表单提交
        document.getElementById('avatarUploadForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleAvatarUpload();
        });

        // 文件预览
        this.bindFilePreview('douyinFile', 'douyinPreview');
        this.bindFilePreview('shipinFile', 'shipinPreview');
        this.bindFilePreview('xiaohongshuFile', 'xiaohongshuPreview');
        this.bindFilePreview('avatarFile', 'avatarFilePreview');

        // 点击模态框背景关闭
        document.getElementById('addRouteModal').addEventListener('click', (e) => {
            if (e.target.id === 'addRouteModal') {
                this.hideAddModal();
            }
        });

        document.getElementById('avatarUploadModal').addEventListener('click', (e) => {
            if (e.target.id === 'avatarUploadModal') {
                this.hideAvatarModal();
            }
        });
    }

    bindFilePreview(inputId, previewId) {
        document.getElementById(inputId).addEventListener('change', (e) => {
            const file = e.target.files[0];
            const preview = document.getElementById(previewId);
            
            if (file) {
                const reader = new FileReader();
                reader.onload = (e) => {
                    preview.querySelector('img').src = e.target.result;
                    preview.classList.remove('hidden');
                };
                reader.readAsDataURL(file);
            } else {
                preview.classList.add('hidden');
            }
        });
    }

    async loadRoutes() {
        try {
            const response = await fetch('/route/api/list');
            const result = await response.json();
            
            if (result.success) {
                this.routes = result.data;
                this.renderRoutes();
            } else {
                this.showMessage('加载线路列表失败: ' + result.message, 'error');
            }
        } catch (error) {
            console.error('加载线路列表失败:', error);
            this.showMessage('加载线路列表失败', 'error');
        }
    }

    async refreshData() {
        // 添加刷新动画效果
        const refreshBtn = document.getElementById('refreshBtn');
        const refreshIcon = refreshBtn.querySelector('i');
        
        // 禁用按钮并添加旋转动画
        refreshBtn.disabled = true;
        refreshIcon.classList.add('fa-spin');
        
        try {
            // 重新加载数据
            await this.loadRoutes();
            this.showMessage('数据刷新成功', 'success');
        } catch (error) {
            console.error('刷新数据失败:', error);
            this.showMessage('刷新数据失败', 'error');
        } finally {
            // 恢复按钮状态
            setTimeout(() => {
                refreshBtn.disabled = false;
                refreshIcon.classList.remove('fa-spin');
            }, 500); // 延迟500ms以确保用户能看到刷新效果
        }
    }

    renderRoutes() {
        const tbody = document.getElementById('routeTableBody');
        tbody.innerHTML = '';

        this.routes.forEach(route => {
            const row = this.createRouteRow(route);
            tbody.appendChild(row);
        });
    }

    createRouteRow(route) {
        const row = document.createElement('tr');
        row.className = 'hover:bg-gray-50';
        
        row.innerHTML = `
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                ${route.routeName}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                ${route.routeValue}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                ${this.createAvatarCell(route, 'douyin', route.douyinAvatar)}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                ${this.createAvatarCell(route, 'shipin', route.shipinAvatar)}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                ${this.createAvatarCell(route, 'xiaohongshu', route.xiaohongshuAvatar)}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                ${this.createStatusToggle(route)}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <button onclick="routeManager.deleteRoute(${route.id})" 
                        class="text-red-600 hover:text-red-900 ml-2">
                    删除
                </button>
            </td>
        `;

        return row;
    }

    createAvatarCell(route, platform, avatarPath) {
        if (avatarPath) {
            return `
                <div class="flex items-center space-x-2">
                    <img src="${avatarPath}" alt="${platform}头像" class="avatar-preview">
                    <button onclick="routeManager.showAvatarUpload(${route.id}, '${platform}')" 
                            class="text-blue-600 hover:text-blue-900 flex items-center justify-center w-6 h-6 rounded-full border border-blue-600 hover:bg-blue-50" 
                            title="上传头像">
                        <i class="fas fa-upload text-sm"></i>
                    </button>
                    <button onclick="routeManager.clearAvatar(${route.id}, '${platform}')" 
                            class="text-red-600 hover:text-red-900 ml-1">
                        清除
                    </button>
                </div>
            `;
        } else {
            return `
                <button onclick="routeManager.showAvatarUpload(${route.id}, '${platform}')" 
                        class="bg-gray-100 border-2 border-dashed border-gray-300 rounded-lg p-2 text-center hover:bg-gray-50 w-16 h-16 flex items-center justify-center" 
                        title="上传头像">
                    <i class="fas fa-upload text-gray-400 text-xl"></i>
                </button>
            `;
        }
    }

    createStatusToggle(route) {
        return `
            <label class="status-toggle">
                <input type="checkbox" ${route.status ? 'checked' : ''} 
                       onchange="routeManager.toggleStatus(${route.id}, this.checked)">
                <span class="slider"></span>
            </label>
        `;
    }

    showAddModal() {
        document.getElementById('addRouteModal').classList.add('show');
        this.resetAddForm();
    }

    hideAddModal() {
        document.getElementById('addRouteModal').classList.remove('show');
    }

    showAvatarUpload(routeId, platform) {
        document.getElementById('uploadRouteId').value = routeId;
        document.getElementById('uploadPlatform').value = platform;
        document.getElementById('avatarUploadModal').classList.add('show');
        this.resetAvatarForm();
    }

    hideAvatarModal() {
        document.getElementById('avatarUploadModal').classList.remove('show');
    }

    resetAddForm() {
        document.getElementById('addRouteForm').reset();
        document.getElementById('douyinPreview').classList.add('hidden');
        document.getElementById('shipinPreview').classList.add('hidden');
        document.getElementById('xiaohongshuPreview').classList.add('hidden');
    }

    resetAvatarForm() {
        document.getElementById('avatarUploadForm').reset();
        document.getElementById('avatarFilePreview').classList.add('hidden');
    }

    async handleAddRoute() {
        const formData = new FormData();
        const routeName = document.getElementById('routeName').value;
        const routeValue = document.getElementById('routeValue').value;

        formData.append('routeName', routeName);
        formData.append('routeValue', routeValue);

        try {
            // 首先创建线路
            const response = await fetch('/route/api/create', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (result.success) {
                const routeId = result.data.id;

                // 上传头像文件
                await this.uploadRouteAvatars(routeId);

                this.showMessage('线路创建成功', 'success');
                this.hideAddModal();
                this.loadRoutes();
            } else {
                this.showMessage('创建失败: ' + result.message, 'error');
            }
        } catch (error) {
            console.error('创建线路失败:', error);
            this.showMessage('创建线路失败', 'error');
        }
    }

    async uploadRouteAvatars(routeId) {
        const files = [
            { input: 'douyinFile', platform: 'douyin' },
            { input: 'shipinFile', platform: 'shipin' },
            { input: 'xiaohongshuFile', platform: 'xiaohongshu' }
        ];

        for (const fileInfo of files) {
            const fileInput = document.getElementById(fileInfo.input);
            const file = fileInput.files[0];

            if (file) {
                const formData = new FormData();
                formData.append('file', file);

                try {
                    await fetch(`/route/api/${routeId}/avatar/${fileInfo.platform}`, {
                        method: 'POST',
                        body: formData
                    });
                } catch (error) {
                    console.error(`上传${fileInfo.platform}头像失败:`, error);
                }
            }
        }
    }

    async handleAvatarUpload() {
        const routeId = document.getElementById('uploadRouteId').value;
        const platform = document.getElementById('uploadPlatform').value;
        const fileInput = document.getElementById('avatarFile');
        const file = fileInput.files[0];

        if (!file) {
            this.showMessage('请选择文件', 'error');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch(`/route/api/${routeId}/avatar/${platform}`, {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (result.success) {
                this.showMessage('头像上传成功', 'success');
                this.hideAvatarModal();
                this.loadRoutes();
            } else {
                this.showMessage('上传失败: ' + result.message, 'error');
            }
        } catch (error) {
            console.error('上传头像失败:', error);
            this.showMessage('上传头像失败', 'error');
        }
    }

    async toggleStatus(routeId, status) {
        try {
            const formData = new FormData();
            formData.append('status', status);

            const response = await fetch(`/route/api/${routeId}/status`, {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (result.success) {
                this.showMessage('状态更新成功', 'success');
                // 更新本地数据
                const route = this.routes.find(r => r.id === routeId);
                if (route) {
                    route.status = status;
                }
            } else {
                this.showMessage('状态更新失败: ' + result.message, 'error');
                // 恢复原状态
                this.loadRoutes();
            }
        } catch (error) {
            console.error('更新状态失败:', error);
            this.showMessage('更新状态失败', 'error');
            this.loadRoutes();
        }
    }

    async clearAvatar(routeId, platform) {
        if (!confirm('确定要清除这个头像吗？')) {
            return;
        }

        try {
            const response = await fetch(`/route/api/${routeId}/avatar/${platform}`, {
                method: 'DELETE'
            });

            const result = await response.json();

            if (result.success) {
                this.showMessage('头像清除成功', 'success');
                this.loadRoutes();
            } else {
                this.showMessage('清除失败: ' + result.message, 'error');
            }
        } catch (error) {
            console.error('清除头像失败:', error);
            this.showMessage('清除头像失败', 'error');
        }
    }

    async deleteRoute(routeId) {
        if (!confirm('确定要删除这条线路吗？')) {
            return;
        }

        try {
            const response = await fetch(`/route/api/${routeId}`, {
                method: 'DELETE'
            });

            const result = await response.json();

            if (result.success) {
                this.showMessage('删除成功', 'success');
                this.loadRoutes();
            } else {
                this.showMessage('删除失败: ' + result.message, 'error');
            }
        } catch (error) {
            console.error('删除线路失败:', error);
            this.showMessage('删除线路失败', 'error');
        }
    }

    showMessage(message, type = 'info') {
        // 创建消息提示
        const messageDiv = document.createElement('div');
        messageDiv.className = `fixed top-4 right-4 px-4 py-2 rounded-lg text-white z-50 ${
            type === 'success' ? 'bg-green-500' : 
            type === 'error' ? 'bg-red-500' : 'bg-blue-500'
        }`;
        messageDiv.textContent = message;

        document.body.appendChild(messageDiv);

        // 3秒后自动移除
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.parentNode.removeChild(messageDiv);
            }
        }, 3000);
    }
}

// 初始化
let routeManager;
document.addEventListener('DOMContentLoaded', () => {
    routeManager = new RouteManager();
});