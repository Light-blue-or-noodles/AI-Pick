// 环境配置
const ENV = {
  development: {
    baseUrl: 'http://localhost:8080'
  },
  production: {
    baseUrl: 'http://59.110.0.107:8080'
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
