<template>
  <AgreementPage title="用户协议" updated="最近更新日期：2026年3月14日" :sections="sections" />
</template>

<script setup>
import AgreementPage from '@/components/AgreementPage.vue';

const sections = [
  {
    subtitle: '一、总则',
    paragraphs: [
      '欢迎使用 Spark Link！本《用户协议》是您与 Spark Link 平台运营方之间关于注册、登录、使用 Spark Link 及相关服务所订立的协议。',
      '在使用本服务之前，请您务必仔细阅读并充分理解本协议的全部内容。',
      '当您点击「同意」或实际使用本服务时，即视为您已充分阅读、理解并接受本协议的全部内容。'
    ]
  },
  {
    subtitle: '二、账号注册与使用',
    paragraphs: [
      '您应当使用具有合法来源的账号进行登录，并保证所提供信息真实、准确、完整。',
      '您对登录后在本平台进行的所有操作行为承担责任，不得冒用他人身份或从事违法违规行为。',
      '如发现账号存在安全风险，请立即通知平台。'
    ]
  },
  {
    subtitle: '三、用户行为规范',
    paragraphs: [
      '您在使用服务过程中应遵守法律法规及社会公序良俗，不得发布、传播违法违规或侵害他人合法权益的内容。'
    ]
  },
  {
    subtitle: '四、服务变更、中止与终止',
    paragraphs: [
      '平台有权基于运营安排或安全原因，对全部或部分服务进行调整、暂停或终止。'
    ]
  },
  {
    subtitle: '五、免责声明',
    paragraphs: [
      '在法律允许的范围内，平台对因不可抗力、网络服务提供商原因等造成的服务中断或数据丢失不承担责任，但会协助您尽量减少损失。'
    ]
  },
  {
    subtitle: '六、法律适用与争议解决',
    paragraphs: [
      '本协议适用中华人民共和国大陆地区法律。争议应友好协商；协商不成的，可向平台运营方所在地有管辖权的人民法院提起诉讼。'
    ]
  }
];
</script>
