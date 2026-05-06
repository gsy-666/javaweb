import { createRouter, createWebHistory } from 'vue-router'
import SituationView from './views/SituationView.vue'
import CreateWorkOrderView from './views/CreateWorkOrderView.vue'
import WorkOrdersView from './views/WorkOrdersView.vue'
import WorkOrderDetailView from './views/WorkOrderDetailView.vue'
import WorkerConsoleView from './views/WorkerConsoleView.vue'
import KpiView from './views/KpiView.vue'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import AdminView from './views/AdminView.vue'
import AdminOrdersView from './views/AdminOrdersView.vue'
import AdminUsersView from './views/AdminUsersView.vue'
import ProfileView from './views/ProfileView.vue'
import { isAuthed, role } from './user'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/register', component: RegisterView, meta: { public: true } },

    // User / repair end
    { path: '/', component: SituationView, meta: { roles: ['USER'] } },
    { path: '/create', component: CreateWorkOrderView, meta: { roles: ['USER'] } },
    { path: '/orders', component: WorkOrdersView, meta: { roles: ['USER'] } },
    { path: '/orders/:id', component: WorkOrderDetailView, meta: { roles: ['USER'] } },

    // Worker end
    { path: '/worker', component: WorkerConsoleView, meta: { roles: ['WORKER'] } },
    { path: '/worker/orders/:id', component: WorkOrderDetailView, meta: { roles: ['WORKER'] } },

    // Admin end
    { path: '/admin', component: AdminView, meta: { roles: ['ADMIN'] } },
    { path: '/admin/orders', component: AdminOrdersView, meta: { roles: ['ADMIN'] } },
    { path: '/admin/users', component: AdminUsersView, meta: { roles: ['ADMIN'] } },
    { path: '/admin/kpi', component: KpiView, meta: { roles: ['ADMIN'] } },
    { path: '/admin/orders/:id', component: WorkOrderDetailView, meta: { roles: ['ADMIN'] } },

    // Shared
    { path: '/me', component: ProfileView, meta: { roles: ['USER', 'WORKER', 'ADMIN'] } },

    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  const isPublic = Boolean(to.meta?.public)
  if (!isPublic && !isAuthed.value) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const allowedRoles = (to.meta?.roles as string[] | undefined) || null
  if (allowedRoles && !allowedRoles.includes(role.value)) {
    if (role.value === 'ADMIN') return { path: '/admin' }
    if (role.value === 'WORKER') return { path: '/worker' }
    return { path: '/' }
  }

  // Normalize cross-end entry points
  if (role.value === 'ADMIN') {
    if (to.path === '/') return { path: '/admin' }
    if (to.path === '/kpi') return { path: '/admin/kpi' }
  }
  if (role.value === 'WORKER') {
    if (to.path === '/') return { path: '/worker' }
    if (to.path === '/kpi') return { path: '/worker' }
  }

  return true
})

