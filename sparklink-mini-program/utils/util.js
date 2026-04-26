// utils/util.js
const formatTime = date => {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hour = date.getHours();
  const minute = date.getMinutes();
  const second = date.getSeconds();

  return `${[year, month, day].map(formatNumber).join('/')} ${[hour, minute, second].map(formatNumber).join(':')}`;
};

const formatNumber = n => {
  n = n.toString();
  return n[1] ? n : `0${n}`;
};

// 格式化相对时间
const formatRelativeTime = (timestamp) => {
  const now = Date.now();
  const diff = now - new Date(timestamp).getTime();
  
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  
  if (diff < minute) {
    return '刚刚';
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前';
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前';
  } else if (diff < 7 * day) {
    return Math.floor(diff / day) + '天前';
  } else {
    const date = new Date(timestamp);
    return `${date.getMonth() + 1}-${date.getDate()}`;
  }
};

// 格式化日期
const formatDate = (date, separator = '-') => {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  
  return `${year}${separator}${formatNumber(month)}${separator}${formatNumber(day)}`;
};

// 验证手机号
const validatePhone = (phone) => {
  return /^1[3-9]\d{9}$/.test(phone);
};

// 验证邮箱
const validateEmail = (email) => {
  return /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/.test(email);
};

// 防抖
const debounce = (fn, delay = 300) => {
  let timer = null;
  return function (...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
};

// 节流
const throttle = (fn, delay = 300) => {
  let last = 0;
  return function (...args) {
    const now = Date.now();
    if (now - last > delay) {
      last = now;
      fn.apply(this, args);
    }
  };
};

module.exports = {
  formatTime,
  formatRelativeTime,
  formatDate,
  validatePhone,
  validateEmail,
  debounce,
  throttle
};