// pages/calendar/calendar.js
const { get } = require('../../utils/request');

Page({
  data: {
    currentYear: 2026,
    currentMonth: 3,
    selectedDate: '',
    selectedDateStr: '',
    calendarDays: [],
    selectedDateActivities: [],
    activities: []
  },

  onLoad() {
    const now = new Date();
    this.setData({
      currentYear: now.getFullYear(),
      currentMonth: now.getMonth() + 1
    });

    this.fetchCalendarData().then(() => {
      this.initCalendar();
      this.selectDate({ currentTarget: { dataset: { date: this.formatDate(now) } } });
    });
  },

  // 从后端获取日历活动数据（GET /api/activity/calendar?year=xxx&month=xxx）
  fetchCalendarData() {
    const { currentYear, currentMonth } = this.data;
    return get('/api/activity/calendar', { year: currentYear, month: currentMonth })
      .then((res) => {
        const data = (res && res.data) || {};
        const days = Array.isArray(data.days) ? data.days : [];
        const activities = days.flatMap((d) => (d.activities || []).map((a) => this.mapCalendarActivity(a, d.date)));
        this.setData({ activities });
      })
      .catch((err) => {
        console.warn('获取日历数据失败，检查 /api/activity/calendar 是否存在', err);
        this.setData({ activities: [] });
      });
  },

  mapCalendarActivity(a, date) {
    const t = a.eventTime || a.startTime;
    const timeStr = t ? (typeof t === 'string' ? t.replace('T', ' ').substring(0, 16) : '') : '';
    const statusText = (a.status === 0 && '待开始') || (a.status === 1 && '报名中') || (a.status === 2 && '进行中') || (a.status === 3 && '已结束') || '已结束';
    return {
      ...a,
      date,
      name: a.title || '活动',
      time: timeStr,
      location: a.address || a.location || '',
      statusText,
      currentPeople: a.currentParticipants != null ? a.currentParticipants : 0,
      totalPeople: a.maxParticipants != null ? a.maxParticipants : 0,
      cover: a.coverImage || '/images/default-avatar.png'
    };
  },

  // 初始化日历
  initCalendar() {
    const { currentYear, currentMonth } = this.data;
    const days = [];
    
    // 获取当月第一天是星期几
    const firstDay = new Date(currentYear, currentMonth - 1, 1).getDay();
    // 获取当月有多少天
    const daysInMonth = new Date(currentYear, currentMonth, 0).getDate();
    
    // 填充空白
    const startDay = firstDay === 0 ? 6 : firstDay - 1;
    for (let i = 0; i < startDay; i++) {
      days.push({ day: '', date: '' });
    }
    
    // 获取今天
    const today = new Date();
    const todayStr = this.formatDate(today);
    
    // 填充日期
    for (let day = 1; day <= daysInMonth; day++) {
      const date = `${currentYear}-${String(currentMonth).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const hasActivity = this.data.activities.some(a => a.date === date);
      
      days.push({
        day,
        date,
        isToday: date === todayStr,
        hasActivity
      });
    }
    
    this.setData({ calendarDays: days });
  },

  // 格式化日期
  formatDate(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  },

  // 上个月
  prevMonth() {
    let { currentYear, currentMonth } = this.data;
    if (currentMonth === 1) {
      currentMonth = 12;
      currentYear--;
    } else {
      currentMonth--;
    }
    this.setData({ currentYear, currentMonth });
    this.fetchCalendarData().then(() => this.initCalendar());
  },

  // 下个月
  nextMonth() {
    let { currentYear, currentMonth } = this.data;
    if (currentMonth === 12) {
      currentMonth = 1;
      currentYear++;
    } else {
      currentMonth++;
    }
    this.setData({ currentYear, currentMonth });
    this.fetchCalendarData().then(() => this.initCalendar());
  },

  // 选择日期
  selectDate(e) {
    const date = e.currentTarget.dataset.date;
    if (!date) return;
    
    // 更新选中状态
    const calendarDays = this.data.calendarDays.map(item => ({
      ...item,
      isSelected: item.date === date
    }));
    
    // 格式化显示
    const dateObj = new Date(date.replace(/-/g, '/'));
    const dateStr = `${dateObj.getMonth() + 1}月${dateObj.getDate()}日`;
    
    // 获取当天活动
    const activities = this.data.activities.filter(a => a.date === date);
    
    this.setData({
      selectedDate: date,
      selectedDateStr: dateStr,
      calendarDays,
      selectedDateActivities: activities
    });
  },

  goBack() {
    wx.navigateBack();
  },

  goToActivity(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/activity-detail/activity-detail?id=${id}`
    });
  }
});