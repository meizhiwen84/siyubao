(function () {
    const isJcef = typeof window.cefQuery === 'function';

    function invoke(method, params) {
        if (!isJcef) {
            return Promise.reject(new Error('JCEF bridge not available'));
        }
        return new Promise((resolve, reject) => {
            window.cefQuery({
                request: JSON.stringify({ method, params: params ?? {} }),
                onSuccess: (response) => {
                    try {
                        resolve(JSON.parse(response));
                    } catch (e) {
                        reject(e);
                    }
                },
                onFailure: (_code, message) => reject(new Error(message || 'bridge failure'))
            });
        });
    }

    async function httpJson(url, options) {
        const res = await fetch(url, options);
        const json = await res.json().catch(() => ({}));
        if (!res.ok) {
            const msg = json && (json.message || json.error) ? (json.message || json.error) : res.statusText;
            throw new Error(msg);
        }
        return json;
    }

    function formBody(obj) {
        const p = new URLSearchParams();
        Object.keys(obj).forEach((k) => {
            if (obj[k] !== undefined && obj[k] !== null) p.append(k, String(obj[k]));
        });
        return p;
    }

    function invokeOrHttp(method, params, http) {
        if (isJcef) return invoke(method, params);
        return http();
    }

    const routes = {
        list: () => invokeOrHttp('route.list', {}, () => httpJson('/route/api/list')),
        create: (routeName, routeValue) =>
            invokeOrHttp('route.create', { routeName, routeValue }, () =>
                httpJson('/route/api/create', { method: 'POST', body: formBody({ routeName, routeValue }) })
            ),
        update: (id, routeName, routeValue) =>
            invokeOrHttp('route.update', { id, routeName, routeValue }, () =>
                httpJson('/route/api/update', { method: 'POST', body: formBody({ id, routeName, routeValue }) })
            ),
        updateStatus: (id, status) =>
            invokeOrHttp('route.updateStatus', { id, status }, () =>
                httpJson(`/route/api/${id}/status`, { method: 'POST', body: formBody({ status }) })
            ),
        updateWelcomeMessage: (id, welcomeMessage) =>
            invokeOrHttp('route.updateWelcomeMessage', { id, welcomeMessage }, () =>
                httpJson(`/route/api/${id}/welcome-message`, { method: 'POST', body: formBody({ welcomeMessage }) })
            ),
        delete: (id) => invokeOrHttp('route.delete', { id }, () => httpJson(`/route/api/${id}`, { method: 'DELETE' })),
        uploadAvatar: (id, platform, file) =>
            isJcef
                ? new Promise((resolve, reject) => {
                      if (!file) {
                          reject(new Error('missing file'));
                          return;
                      }
                      const r = new FileReader();
                      r.onload = () => {
                          const dataUrl = r.result || '';
                          const comma = typeof dataUrl === 'string' ? dataUrl.indexOf(',') : -1;
                          const base64 = comma >= 0 ? String(dataUrl).slice(comma + 1) : '';
                          invoke('route.uploadAvatar', { id, platform, filename: file.name, contentBase64: base64 }).then(resolve, reject);
                      };
                      r.onerror = () => reject(new Error('read file failed'));
                      r.readAsDataURL(file);
                  })
                : (() => {
                      const formData = new FormData();
                      formData.append('file', file);
                      return httpJson(`/route/api/${id}/avatar/${platform}`, { method: 'POST', body: formData });
                  })(),
        clearAvatar: (id, platform) =>
            invokeOrHttp('route.clearAvatar', { id, platform }, () =>
                httpJson(`/route/api/${id}/avatar/${platform}`, { method: 'DELETE' })
            )
    };

    const auth = {
        me: () => invokeOrHttp('auth.me', {}, () => httpJson('/api/auth/me')),
        logout: () => invokeOrHttp('auth.logout', {}, () => httpJson('/api/auth/logout', { method: 'POST' }))
    };

    const ocr = {
        performDataUrl: (dataUrl, filename = 'image.png') =>
            isJcef
                ? (() => {
                      const comma = typeof dataUrl === 'string' ? dataUrl.indexOf(',') : -1;
                      const base64 = comma >= 0 ? String(dataUrl).slice(comma + 1) : '';
                      const contentType = comma >= 0 ? String(dataUrl).slice(5, String(dataUrl).indexOf(';')) : '';
                      return invoke('ocr.perform', { filename, contentType, contentBase64: base64 });
                  })()
                : (() => {
                      const res = new URL(dataUrl, window.location.href);
                      return fetch(res.href)
                          .then((r) => r.blob())
                          .then((blob) => {
                              const formData = new FormData();
                              formData.append('image', blob, filename);
                              return httpJson('/api/ocr', { method: 'POST', body: formData });
                          });
                  })()
    };

    const chat = {
        generate: (xianlu, platform, chatContent, xianshiname, editable) =>
            invokeOrHttp(
                'chat.generate',
                { xianlu, platform, chatContent, xianshiname, editable: !!editable },
                () =>
                    fetch('/api/jsbridge/invoke', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ method: 'chat.generate', params: { xianlu, platform, chatContent, xianshiname, editable: !!editable } })
                    }).then((r) => r.json())
            )
        ,
        regenerate: ({ xianlu, platform, xianshiname, userName, userAvatar, myAvatar, topTime, messageId, chatMessages, editable }) =>
            invokeOrHttp(
                'chat.regenerate',
                { xianlu, platform, xianshiname, userName, userAvatar, myAvatar, topTime, messageId, chatMessages, editable: !!editable },
                () =>
                    fetch('/api/jsbridge/invoke', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ method: 'chat.regenerate', params: { xianlu, platform, xianshiname, userName, userAvatar, myAvatar, topTime, messageId, chatMessages, editable: !!editable } })
                    }).then((r) => r.json())
            )
    };

    const upload = {
        image: ({ file, filename, contentBase64 }) =>
            invokeOrHttp(
                'upload.image',
                { filename, contentBase64 },
                () => {
                    if (!file) return Promise.resolve({ success: false, message: '缺少文件' });
                    const form = new FormData();
                    form.append('file', file, file.name || filename || 'image.png');
                    return fetch('/api/upload/image', { method: 'POST', body: form }).then((r) => r.json());
                }
            )
    };

    const history = {
        list: ({ line, platform, phone, page, size }) =>
            invokeOrHttp(
                'history.list',
                { line, platform, phone, page, size },
                () =>
                    httpJson(
                        `/api/history/list?line=${encodeURIComponent(line || '')}` +
                            `&platform=${encodeURIComponent(platform || '')}` +
                            `&phone=${encodeURIComponent(phone || '')}` +
                            `&page=${encodeURIComponent(page ?? 0)}` +
                            `&size=${encodeURIComponent(size ?? 10)}`
                    )
            )
    };

    window.SiyuBaoBackend = {
        isJcef,
        invoke,
        httpJson,
        routes,
        auth,
        ocr,
        upload,
        chat,
        history
    };
})();
