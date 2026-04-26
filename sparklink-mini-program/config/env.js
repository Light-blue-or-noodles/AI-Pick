// 环境配置
// 生产环境须使用 HTTPS 域名；正式/体验版请求域名需在「微信公众平台 → 开发 → 开发管理 → 服务器域名」中配置 request 合法域名（如 https://www.aipick.cloud），不可只填 IP。
const ENV = {
  // 开发者工具 / 开发版：与线上一致走远程 HTTPS（需在工具里勾选「不校验合法域名」或已配置 request 合法域名）
  development: {
    baseUrl: 'https://www.aipick.cloud'
  },
  production: {
    baseUrl: 'https://www.aipick.cloud'
  }
};

// 根据小程序环境自动判断
// __wxConfig.envVersion: develop(开发版), trial(体验版), release(正式版)
const isDev = typeof __wxConfig !== 'undefined' && __wxConfig.envVersion === 'develop';

const selected = isDev ? ENV.development : ENV.production;

module.exports = {
  ...selected,
  /** 微信登录失败时是否回退测试账号（真机正式登录请保持 false） */
  allowWechatLoginTestFallback: false
};
