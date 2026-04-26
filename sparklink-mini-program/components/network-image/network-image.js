// 网络图片：真机下对远程 URL 使用 wx.downloadFile；统一经 utils/mediaUrl 解析，避免各处 URL 规则不一致
const { resolveMediaUrl } = require('../../utils/mediaUrl.js');

Component({
  properties: {
    url: { type: String, value: '' },
    defaultSrc: { type: String, value: '/images/default-avatar.png' },
    mode: { type: String, value: 'aspectFill' },
    customClass: { type: String, value: '' },
    /** 历史属性；开发者工具现统一直链，不再走 downloadFile，该字段可忽略 */
    preferDownloadInDevtools: { type: Boolean, value: false },
    /** 与 mediaUrl.resolveMediaUrl 一致：avatar | cover | general */
    mediaKind: { type: String, value: 'general' },
    /** 为 false 时不做 URL 解析（已传入完整可访问地址时） */
    resolveUrl: { type: Boolean, value: true }
  },
  data: {
    displaySrc: '',
    isDevtools: false
  },
  observers: {
    url: function (url) {
      this.setDisplaySrc(url);
    }
  },
  lifetimes: {
    attached() {
      try {
        const sys = wx.getSystemInfoSync && wx.getSystemInfoSync();
        const isDevtools = !!(sys && sys.platform === 'devtools');
        this.setData({ isDevtools });
      } catch (e) {
        // ignore
      }
      this.setDisplaySrc(this.properties.url);
    }
  },
  methods: {
    setDisplaySrc(url) {
      const defaultSrc = this.properties.defaultSrc;
      const kind = this.properties.mediaKind || 'general';
      if (url == null || (typeof url === 'string' && url.trim() === '')) {
        this.setData({ displaySrc: defaultSrc });
        return;
      }
      let t = String(url).trim();
      if (this.properties.resolveUrl !== false) {
        t = resolveMediaUrl(t, { kind });
      }

      if (t.startsWith('/images/') || t.startsWith('data:')) {
        this.setData({ displaySrc: t });
        return;
      }
      if (!t.startsWith('http://') && !t.startsWith('https://')) {
        this.setData({ displaySrc: t || defaultSrc });
        return;
      }
      // 开发者工具中 wx.downloadFile 走「下载合法域名」；与 request 不同，本机/局域网常未配导致失败 → 白块。
      // 模拟器用直链 <image> 可正常拉取 http 局域网，真机仍走 downloadFile 符合平台规则。
      if (this.data.isDevtools) {
        this.setData({ displaySrc: t });
        return;
      }
      wx.downloadFile({
        url: t,
        success: (res) => {
          if (res.statusCode === 200 && res.tempFilePath) {
            this.setData({ displaySrc: res.tempFilePath });
          } else {
            this.setData({ displaySrc: defaultSrc });
          }
        },
        fail: () => {
          this.setData({ displaySrc: defaultSrc });
        }
      });
    },
    onError() {
      this.setData({ displaySrc: this.properties.defaultSrc });
    }
  }
});
