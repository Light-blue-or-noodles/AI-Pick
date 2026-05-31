import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const MainLayout = () => import('@/layouts/MainLayout.vue');

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/login/wechat-callback',
    name: 'wechat-callback',
    component: () => import('@/views/WechatCallbackView.vue'),
    meta: { public: true, title: '微信登录' }
  },
  {
    path: '/agreement/user',
    name: 'agreement-user',
    component: () => import('@/views/AgreementUserView.vue'),
    meta: { public: true, title: '用户协议' }
  },
  {
    path: '/agreement/privacy',
    name: 'agreement-privacy',
    component: () => import('@/views/AgreementPrivacyView.vue'),
    meta: { public: true, title: '隐私政策' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { tab: 'home', title: '首页' }
      },
      {
        path: 'partner',
        name: 'partner',
        component: () => import('@/views/PartnerView.vue'),
        meta: { tab: 'partner', title: '搭子' }
      },
      {
        path: 'message',
        name: 'message',
        component: () => import('@/views/MessageView.vue'),
        meta: { tab: 'message', title: '消息', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { tab: 'profile', title: '我的', requiresAuth: true }
      }
    ]
  },
  {
    path: '/partner/filter',
    name: 'partner-filter',
    component: () => import('@/views/PartnerFilterView.vue'),
    meta: { title: '搭子筛选' }
  },
  {
    path: '/partner/:id',
    name: 'partner-detail',
    component: () => import('@/views/PartnerDetailView.vue'),
    meta: { title: '搭子详情' }
  },
  {
    path: '/partner-publish',
    name: 'partner-publish',
    component: () => import('@/views/PartnerPublishView.vue'),
    meta: { title: '发布搭子', requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { title: '聊天', requiresAuth: true }
  },
  {
    path: '/ai-chat',
    name: 'ai-chat',
    component: () => import('@/views/AiChatView.vue'),
    meta: { title: 'AI 助手' }
  },
  {
    path: '/profile-edit',
    name: 'profile-edit',
    component: () => import('@/views/ProfileEditView.vue'),
    meta: { title: '编辑资料', requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { title: '设置', requiresAuth: true }
  },
  {
    path: '/settings/account-security',
    name: 'account-security',
    component: () => import('@/views/AccountSecurityView.vue'),
    meta: { title: '账号与安全', requiresAuth: true }
  },
  {
    path: '/settings/privacy',
    name: 'privacy-settings',
    component: () => import('@/views/PrivacySettingsView.vue'),
    meta: { title: '隐私设置', requiresAuth: true }
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('@/views/AboutView.vue'),
    meta: { title: '关于我们' }
  },
  {
    path: '/my/partners',
    name: 'my-partners',
    component: () => import('@/views/MyPartnersView.vue'),
    meta: { title: '我发布的搭子', requiresAuth: true }
  },
  {
    path: '/my/joined-partners',
    name: 'my-joined-partners',
    component: () => import('@/views/MyJoinedPartnersView.vue'),
    meta: { title: '我参加的搭子', requiresAuth: true }
  },
  {
    path: '/my/following',
    name: 'my-following',
    component: () => import('@/views/MyFollowingView.vue'),
    meta: { title: '我的关注', requiresAuth: true }
  },
  {
    path: '/my/followers',
    name: 'my-followers',
    component: () => import('@/views/MyFollowersView.vue'),
    meta: { title: '我的粉丝', requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'user-profile',
    component: () => import('@/views/UserProfileView.vue'),
    meta: { title: '用户主页' }
  },
  {
    path: '/join/company',
    name: 'join-company',
    component: () => import('@/views/CompanyJoinView.vue'),
    meta: { title: '我的公司', requiresAuth: true }
  },
  {
    path: '/join/school',
    name: 'join-school',
    component: () => import('@/views/SchoolJoinView.vue'),
    meta: { title: '我的学校', requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.meta.title) {
    document.title = `${to.meta.title} - Spark Link`;
  }
  if (to.meta.public) {
    return true;
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (auth.isLoggedIn && !auth.userInfo?.id && !auth.userInfo?.nickname) {
    await auth.fetchUserInfo();
  }
  return true;
});

export default router;
