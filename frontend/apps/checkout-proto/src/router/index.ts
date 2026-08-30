import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/views/HomePage.vue'
import PaymentFlow from '@/views/PaymentFlow.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/payment',
      name: 'payment',
      component: PaymentFlow,
    },
  ],
})

export default router
