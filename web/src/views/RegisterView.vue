<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ToolOutlined } from '@ant-design/icons-vue'

import { api } from '../api'
import { appTitle } from '../appMeta'
import { setSession, type Role } from '../user'

import AnimatedPupil from '../components/AnimatedPupil.vue'
import AnimatedEyeBall from '../components/AnimatedEyeBall.vue'

const router = useRouter()

const loading = ref(false)
const errorText = ref('')

const form = reactive({
  account: '',
  password: '',
  confirmPassword: '',
  role: 'USER' as Role,
  remember: false
})

const mouseX = ref(0)
const mouseY = ref(0)

const isTyping = ref(false)
const isPurpleBlinking = ref(false)
const isBlackBlinking = ref(false)
const isLookingAtEachOther = ref(false)
const isPurplePeeking = ref(false)

const purpleRef = ref<HTMLElement | null>(null)
const blackRef = ref<HTMLElement | null>(null)
const yellowRef = ref<HTMLElement | null>(null)
const orangeRef = ref<HTMLElement | null>(null)

// Random blinking interval logic
let purpleBlinkTimer: number
const schedulePurpleBlink = () => {
  purpleBlinkTimer = window.setTimeout(() => {
    isPurpleBlinking.value = true
    window.setTimeout(() => {
      isPurpleBlinking.value = false
      schedulePurpleBlink()
    }, 150)
  }, Math.random() * 4000 + 3000)
}

let blackBlinkTimer: number
const scheduleBlackBlink = () => {
  blackBlinkTimer = window.setTimeout(() => {
    isBlackBlinking.value = true
    window.setTimeout(() => {
      isBlackBlinking.value = false
      scheduleBlackBlink()
    }, 150)
  }, Math.random() * 4000 + 3000)
}

// Sneaky purple peek
let peekTimer: number
const updatePurplePeeking = () => {
  if (form.password.length > 0 && showingPassword.value) {
    peekTimer = window.setTimeout(() => {
      isPurplePeeking.value = true
      window.setTimeout(() => {
        isPurplePeeking.value = false
        updatePurplePeeking()
      }, 800)
    }, Math.random() * 3000 + 2000)
  } else {
    isPurplePeeking.value = false
    clearTimeout(peekTimer)
  }
}

const onMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove)
  schedulePurpleBlink()
  scheduleBlackBlink()
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
  clearTimeout(purpleBlinkTimer)
  clearTimeout(blackBlinkTimer)
  clearTimeout(peekTimer)
  clearTimeout(lookTimer)
})

let lookTimer: number
const onFocusAccount = () => {
  isTyping.value = true
  isLookingAtEachOther.value = true
  clearTimeout(lookTimer)
  lookTimer = window.setTimeout(() => {
    isLookingAtEachOther.value = false
  }, 800)
}

const onBlurAccount = () => {
  isTyping.value = false
  isLookingAtEachOther.value = false
}

// Support for toggling password visibility
const showingPassword = ref(false)
const inputPasswordType = computed(() => showingPassword.value ? 'text' : 'password')

const togglePasswordVisibility = () => {
  showingPassword.value = !showingPassword.value
  updatePurplePeeking()
}

const onPasswordChange = () => {
  updatePurplePeeking()
}

const onFocusPassword = () => {
  isTyping.value = true
}

const onBlurPassword = () => {
  isTyping.value = false
}

const calculatePosition = (el: HTMLElement | null) => {
  if (!el) return { faceX: 0, faceY: 0, bodySkew: 0 }
  const rect = el.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 3

  const deltaX = mouseX.value - centerX
  const deltaY = mouseY.value - centerY

  const faceX = Math.max(-15, Math.min(15, deltaX / 20))
  const faceY = Math.max(-10, Math.min(10, deltaY / 30))
  const bodySkew = Math.max(-6, Math.min(6, -deltaX / 120))

  return { faceX, faceY, bodySkew }
}

const purplePos = computed(() => calculatePosition(purpleRef.value))
const blackPos = computed(() => calculatePosition(blackRef.value))
const yellowPos = computed(() => calculatePosition(yellowRef.value))
const orangePos = computed(() => calculatePosition(orangeRef.value))

async function onSubmit() {
  errorText.value = ''
  loading.value = true
  try {
    const account = form.account.trim()
    if (!account) throw new Error('请输入账号')
    if (!form.password) throw new Error('请输入密码')
    if (form.password.length < 6) throw new Error('密码至少 6 位')
    if (form.password !== form.confirmPassword) throw new Error('两次密码不一致')
    if (form.role === 'ADMIN') throw new Error('不允许注册管理员')

    const resp = await api.post('/auth/register', {
      account,
      password: form.password,
      role: form.role,
      displayName: account,
    })

    const data = resp.data?.data as { userId: number; role: Role; displayName?: string; token: string }
    setSession({ userId: data.userId, role: data.role, account, token: data.token })

    if (data.role === 'ADMIN') await router.replace('/admin')
    else if (data.role === 'WORKER') await router.replace('/worker')
    else await router.replace('/')
  } catch (e: any) {
    errorText.value = e?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrapper">
    <!-- Left Content Section -->
    <div class="hero-panel hidden-small">
      <div class="brand">
        <div class="logo">
          <ToolOutlined style="font-size: 16px" />
        </div>
        <span>{{ appTitle }}</span>
      </div>

      <div class="characters-container">
        <div class="characters">
          <!-- Purple tall rectangle character -->
          <div
            ref="purpleRef"
            class="char-purple"
            :style="{
              height: (isTyping || (form.password.length > 0 && !showingPassword)) ? '440px' : '400px',
              transform: (form.password.length > 0 && showingPassword)
                ? `skewX(0deg)`
                : (isTyping || (form.password.length > 0 && !showingPassword))
                  ? `skewX(${purplePos.bodySkew - 12}deg) translateX(40px)`
                  : `skewX(${purplePos.bodySkew}deg)`
            }"
          >
            <!-- Worker Safety Helmet (Yellow) -->
            <svg class="hard-hat" style="width: 170px; left: 10px; top: -35px;" viewBox="0 0 120 70" xmlns="http://www.w3.org/2000/svg">
              <path d="M 30 50 C 30 10, 90 10, 90 50 Z" fill="#FBBF24" />
              <rect x="54" y="12" width="12" height="38" rx="5" fill="#F59E0B" />
              <path d="M 15 50 Q 60 55 105 50" fill="none" stroke="#D97706" stroke-width="12" stroke-linecap="round" transform="translate(0, 3)" opacity="0.4" />
              <path d="M 15 50 Q 60 55 105 50" fill="none" stroke="#FBBF24" stroke-width="12" stroke-linecap="round" />
            </svg>
            <div
              class="eyes-container"
              :style="{
                left: (form.password.length > 0 && showingPassword) ? `20px` : isLookingAtEachOther ? `55px` : `${45 + purplePos.faceX}px`,
                top: (form.password.length > 0 && showingPassword) ? `35px` : isLookingAtEachOther ? `65px` : `${40 + purplePos.faceY}px`
              }"
            >
              <AnimatedEyeBall
                :size="18"
                :pupil-size="7"
                :max-distance="5"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isPurpleBlinking"
                :force-look-x="(form.password.length > 0 && showingPassword) ? (isPurplePeeking ? 4 : -4) : isLookingAtEachOther ? 3 : undefined"
                :force-look-y="(form.password.length > 0 && showingPassword) ? (isPurplePeeking ? 5 : -4) : isLookingAtEachOther ? 4 : undefined"
              />
              <AnimatedEyeBall
                :size="18"
                :pupil-size="7"
                :max-distance="5"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isPurpleBlinking"
                :force-look-x="(form.password.length > 0 && showingPassword) ? (isPurplePeeking ? 4 : -4) : isLookingAtEachOther ? 3 : undefined"
                :force-look-y="(form.password.length > 0 && showingPassword) ? (isPurplePeeking ? 5 : -4) : isLookingAtEachOther ? 4 : undefined"
              />
            </div>
          </div>

          <!-- Black tall rectangle character -->
          <div
            ref="blackRef"
            class="char-black"
            :style="{
              transform: (form.password.length > 0 && showingPassword)
                ? `skewX(0deg)`
                : isLookingAtEachOther
                  ? `skewX(${blackPos.bodySkew * 1.5 + 10}deg) translateX(20px)`
                  : (isTyping || (form.password.length > 0 && !showingPassword))
                    ? `skewX(${blackPos.bodySkew * 1.5}deg)`
                    : `skewX(${blackPos.bodySkew}deg)`
            }"
          >
            <!-- Worker Safety Helmet (Yellow) -->
            <svg class="hard-hat" style="width: 130px; left: -5px; top: -30px;" viewBox="0 0 120 70" xmlns="http://www.w3.org/2000/svg">
              <path d="M 30 50 C 30 10, 90 10, 90 50 Z" fill="#FBBF24" />
              <rect x="54" y="12" width="12" height="38" rx="5" fill="#F59E0B" />
              <path d="M 15 50 Q 60 55 105 50" fill="none" stroke="#D97706" stroke-width="12" stroke-linecap="round" transform="translate(0, 3)" opacity="0.4" />
              <path d="M 15 50 Q 60 55 105 50" fill="none" stroke="#FBBF24" stroke-width="12" stroke-linecap="round" />
            </svg>
            <div
              class="eyes-container"
              :style="{
                left: (form.password.length > 0 && showingPassword) ? `10px` : isLookingAtEachOther ? `32px` : `${26 + blackPos.faceX}px`,
                top: (form.password.length > 0 && showingPassword) ? `28px` : isLookingAtEachOther ? `12px` : `${32 + blackPos.faceY}px`,
                gap: '24px'
              }"
            >
              <AnimatedEyeBall
                :size="16"
                :pupil-size="6"
                :max-distance="4"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isBlackBlinking"
                :force-look-x="(form.password.length > 0 && showingPassword) ? -4 : isLookingAtEachOther ? 0 : undefined"
                :force-look-y="(form.password.length > 0 && showingPassword) ? -4 : isLookingAtEachOther ? -4 : undefined"
              />
              <AnimatedEyeBall
                :size="16"
                :pupil-size="6"
                :max-distance="4"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isBlackBlinking"
                :force-look-x="(form.password.length > 0 && showingPassword) ? -4 : isLookingAtEachOther ? 0 : undefined"
                :force-look-y="(form.password.length > 0 && showingPassword) ? -4 : isLookingAtEachOther ? -4 : undefined"
              />
            </div>
          </div>

          <!-- Orange semi-circle character -->
          <div
            ref="orangeRef"
            class="char-orange"
            :style="{
              transform: (form.password.length > 0 && showingPassword) ? `skewX(0deg)` : `skewX(${orangePos.bodySkew}deg)`
            }"
          >
            <!-- Worker Safety Helmet (Yellow, slightly tilted) -->
            <svg class="hard-hat" style="width: 170px; left: 35px; top: -30px; transform: rotate(-5deg);" viewBox="0 0 120 70" xmlns="http://www.w3.org/2000/svg">
              <path d="M 30 50 C 30 10, 90 10, 90 50 Z" fill="#FBBF24" />
              <rect x="54" y="12" width="12" height="38" rx="5" fill="#F59E0B" />
              <path d="M 15 50 Q 60 55 105 50" fill="none" stroke="#D97706" stroke-width="12" stroke-linecap="round" transform="translate(0, 3)" opacity="0.4" />
              <path d="M 15 50 Q 60 55 105 50" fill="none" stroke="#FBBF24" stroke-width="12" stroke-linecap="round" />
            </svg>
            <div
              class="eyes-container"
              :style="{
                left: (form.password.length > 0 && showingPassword) ? `50px` : `${82 + orangePos.faceX}px`,
                top: (form.password.length > 0 && showingPassword) ? `85px` : `${90 + orangePos.faceY}px`
              }"
            >
              <AnimatedPupil
                :size="12"
                :max-distance="5"
                pupil-color="#2D2D2D"
                :force-look-x="(form.password.length > 0 && showingPassword) ? -5 : undefined"
                :force-look-y="(form.password.length > 0 && showingPassword) ? -4 : undefined"
              />
              <AnimatedPupil
                :size="12"
                :max-distance="5"
                pupil-color="#2D2D2D"
                :force-look-x="(form.password.length > 0 && showingPassword) ? -5 : undefined"
                :force-look-y="(form.password.length > 0 && showingPassword) ? -4 : undefined"
              />
            </div>
          </div>

          <!-- Yellow tall rectangle character -->
          <div
            ref="yellowRef"
            class="char-yellow"
            :style="{
              transform: (form.password.length > 0 && showingPassword) ? `skewX(0deg)` : `skewX(${yellowPos.bodySkew}deg)`
            }"
          >
            <!-- Project Manager/Supervisor Helmet (White/Grey to contrast the yellow character) -->
            <svg class="hard-hat" style="width: 160px; left: -10px; top: -38px;" viewBox="0 0 120 70" xmlns="http://www.w3.org/2000/svg">
              <path d="M 30 50 C 30 10, 90 10, 90 50 Z" fill="#F8FAFC" />
              <rect x="54" y="12" width="12" height="38" rx="5" fill="#CBD5E1" />
              <path d="M 15 50 Q 60 55 105 50" fill="none" stroke="#94A3B8" stroke-width="12" stroke-linecap="round" transform="translate(0, 3)" opacity="0.4" />
              <path d="M 15 50 Q 60 55 105 50" fill="none" stroke="#F8FAFC" stroke-width="12" stroke-linecap="round" />
            </svg>
            <div
              class="eyes-container yellow-eyes"
              :style="{
                left: (form.password.length > 0 && showingPassword) ? `20px` : `${52 + yellowPos.faceX}px`,
                top: (form.password.length > 0 && showingPassword) ? `35px` : `${40 + yellowPos.faceY}px`
              }"
            >
              <AnimatedPupil
                :size="12"
                :max-distance="5"
                pupil-color="#2D2D2D"
                :force-look-x="(form.password.length > 0 && showingPassword) ? -5 : undefined"
                :force-look-y="(form.password.length > 0 && showingPassword) ? -4 : undefined"
              />
              <AnimatedPupil
                :size="12"
                :max-distance="5"
                pupil-color="#2D2D2D"
                :force-look-x="(form.password.length > 0 && showingPassword) ? -5 : undefined"
                :force-look-y="(form.password.length > 0 && showingPassword) ? -4 : undefined"
              />
            </div>
            <div
              class="yellow-mouth"
              :style="{
                left: (form.password.length > 0 && showingPassword) ? `10px` : `${40 + yellowPos.faceX}px`,
                top: (form.password.length > 0 && showingPassword) ? `88px` : `${88 + yellowPos.faceY}px`
              }"
            ></div>
          </div>
        </div>
      </div>

      <div class="footer-links">
        <a href="#">隐私政策</a>
        <a href="#">服务条款</a>
        <a href="#">联系我们</a>
      </div>

      <div class="decor overlay-grid"></div>
      <div class="decor glow-top"></div>
      <div class="decor glow-bottom"></div>
    </div>

    <!-- Right Login Section -->
    <div class="login-panel">
      <div class="login-box">
        <!-- Mobile Logo -->
        <div class="brand mobile-only">
          <div class="logo">
            <ToolOutlined style="font-size: 16px" />
          </div>
          <span>{{ appTitle }}</span>
        </div>

        <div class="header">
          <h1 class="title">创建账号</h1>
          <p class="subtitle">注册新账号并选择您的访问身份</p>
        </div>

        <form class="login-form" @submit.prevent="onSubmit">
          <div class="form-item">
            <label>账号</label>
            <input
              type="text"
              v-model="form.account"
              placeholder="手机号 / 用户名"
              required
              class="base-input"
              @focus="onFocusAccount"
              @blur="onBlurAccount"
            />
          </div>

          <div class="form-item">
            <label>密码</label>
            <div class="input-wrapper">
              <input
                :type="inputPasswordType"
                v-model="form.password"
                placeholder="请输入密码"
                required
                class="base-input pass-input"
                @focus="onFocusPassword"
                @blur="onBlurPassword"
                @input="onPasswordChange"
              />
              <button
                type="button"
                class="eye-btn"
                @click.prevent="togglePasswordVisibility"
              >
                <!-- Quick eye icon with SVG -->
                <svg v-if="showingPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-eye-off"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/><line x1="2" x2="22" y1="2" y2="22"/></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-eye"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
              </button>
            </div>
          </div>

          <div class="form-item">
            <label>确认密码</label>
            <div class="input-wrapper">
              <input
                :type="inputPasswordType"
                v-model="form.confirmPassword"
                placeholder="请再次输入密码"
                required
                class="base-input pass-input"
                @focus="onFocusPassword"
                @blur="onBlurPassword"
                @input="onPasswordChange"
              />
            </div>
          </div>
          
          <div class="form-item">
            <label>注册身份</label>
            <select v-model="form.role" class="base-input custom-select" required>
              <option value="USER">报修用户 (USER)</option>
              <option value="WORKER">维修人员 (WORKER)</option>
            </select>
          </div>

          <div v-if="errorText" class="error-box">
            {{ errorText }}
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '注册中...' : '注 册' }}
          </button>
        </form>

        <div class="signup-link">
          已有账号？
          <a @click.prevent="router.push('/login')">去登录</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Reset and base styles for the view */
.login-wrapper {
  height: 100%;
  min-height: calc(100vh - 22px);
  display: flex;
  background-color: #ffffff;
  color: #09090b;
  border-radius: 18px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 20px 25px -5px rgba(0,0,0,0.1);
  overflow: hidden;
}
@media (min-width: 1024px) {
  .login-wrapper {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* Left Hero Section */
.hero-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: linear-gradient(180deg, #eef4ff 0%, #f6f8ff 45%, #ffffff 100%);
  padding: 3rem;
  color: #09090b;
  overflow: hidden;
  border-top-left-radius: 18px;
  border-bottom-left-radius: 18px;
}

.hero-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url('../img/backround1.png') center / cover no-repeat;
  background-position: center 15%;
  opacity: 0.55;
  filter: grayscale(1) contrast(1.1) brightness(0.98);
  pointer-events: none;
  z-index: 1;
}

.hero-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(900px 520px at 22% 35%, rgba(255, 255, 255, 0.85), transparent 65%);
  pointer-events: none;
  z-index: 2;
}

.hero-panel > * {
  position: relative;
  z-index: 5;
}
@media (max-width: 1023px) {
  .hidden-small {
    display: none;
  }
}

.brand {
  position: relative;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.brand .logo {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.5rem;
  background-color: rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand.mobile-only {
  display: none;
  color: #09090b;
  margin-bottom: 3rem;
  justify-content: center;
}
.brand.mobile-only .logo {
  background-color: rgba(108, 63, 245, 0.1);
  color: #6c3ff5;
}
@media (max-width: 1023px) {
  .brand.mobile-only {
    display: flex;
  }
}

/* Characters Rendering */
.characters-container {
  position: relative;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: 500px;
  padding-bottom: 0px;
}
.characters {
  filter: drop-shadow(0 15px 25px rgba(0,0,0,0.1));
  position: relative;
  width: 550px;
  height: 400px;
}
.char-purple {
  position: absolute;
  bottom: 0;
  left: 70px;
  width: 180px;
  background-color: #09090b;
  border-radius: 10px 10px 0 0;
  z-index: 1;
  transition: all 0.7s ease-in-out;
  transform-origin: bottom center;
}
.char-black {
  position: absolute;
  bottom: 0;
  left: 240px;
  width: 120px;
  height: 310px;
  background-color: #1d4ed8;
  border-radius: 8px 8px 0 0;
  z-index: 2;
  transition: all 0.7s ease-in-out;
  transform-origin: bottom center;
}
.char-orange {
  position: absolute;
  bottom: 0;
  left: 0px;
  width: 240px;
  height: 200px;
  background-color: #FF9B6B;
  border-radius: 120px 120px 0 0;
  z-index: 3;
  transition: all 0.7s ease-in-out;
  transform-origin: bottom center;
}
.char-yellow {
  position: absolute;
  bottom: 0;
  left: 310px;
  width: 140px;
  height: 230px;
  background-color: #E8D754;
  border-radius: 70px 70px 0 0;
  z-index: 4;
  transition: all 0.7s ease-in-out;
  transform-origin: bottom center;
}

.hard-hat {
  position: absolute;
  z-index: 10;
  transition: all 0.3s ease;
  pointer-events: none;
}

.eyes-container {
  position: absolute;
  display: flex;
  gap: 32px;
  transition: all 0.7s ease-in-out;
}
.yellow-eyes {
  gap: 24px;
  transition: all 0.2s ease-out;
}
.yellow-mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  background-color: #2D2D2D;
  border-radius: 9999px;
  transition: all 0.2s ease-out;
}

.footer-links {
  position: relative;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 2rem;
  font-size: 0.875rem;
  color: rgba(0, 0, 0, 0.45);
}
.footer-links a {
  color: inherit;
  text-decoration: none;
  transition: color 0.2s;
}
.footer-links a:hover {
  color: #09090b;
}

.decor {
  position: absolute;
  pointer-events: none;
}
.overlay-grid {
  inset: 0;
  background-image: radial-gradient(rgba(0, 0, 0, 0.04) 1px, transparent 1px);
  background-size: 20px 20px;
}
.glow-top {
  top: 25%;
  right: 25%;
  width: 16rem;
  height: 16rem;
  background-color: rgba(0, 0, 0, 0.03);
  border-radius: 50%;
  filter: blur(48px);
}
.glow-bottom {
  bottom: 25%;
  left: 25%;
  width: 24rem;
  height: 24rem;
  background-color: rgba(0, 0, 0, 0.02);
  border-radius: 50%;
  filter: blur(48px);
}

/* Right Panel */
.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background-color: #ffffff;
}
.login-box {
  width: 100%;
  max-width: 420px;
}
.header {
  text-align: center;
  margin-bottom: 2.5rem;
  opacity: 0;
  animation: floatUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: 0.1s;
}
.title {
  font-size: 1.875rem;
  font-weight: 700;
  letter-spacing: -0.025em;
  margin-bottom: 0.5rem;
}
.subtitle {
  color: #71717a;
  font-size: 0.875rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  opacity: 0;
  animation: floatUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: 0.15s;
}
.form-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.form-item label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #3f3f46;
}
.input-wrapper {
  position: relative;
}
.base-input {
  width: 100%;
  height: 3rem;
  padding: 0 1rem;
  font-size: 0.875rem;
  border: 1px solid #d4d4d8;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  border-radius: 0.5rem;
  background-color: #fafafa;
  outline: none;
  transition: border-color 0.2s;
}
.base-input:focus {
  background-color: #ffffff;
  border-color: #09090b;
  box-shadow: 0 0 0 2px rgba(9, 9, 11, 0.1);
}
.pass-input {
  padding-right: 2.5rem;
}
.eye-btn {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #a1a1aa;
  padding: 0;
  display: flex;
  align-items: center;
}
.eye-btn:hover {
  color: #09090b;
}

.custom-select {
  appearance: none;
  cursor: pointer;
  background-image: url('data:image/svg+xml;charset=US-ASCII,%3Csvg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="%2371717a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"%3E%3Cpath d="m6 9 6 6 6-6"/%3E%3C/svg%3E');
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 1em;
  padding-right: 2.5rem;
}
.custom-select:hover {
  background-color: #fafafa;
}

.options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.remember {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  cursor: pointer;
}
.forgot {
  font-size: 0.875rem;
  font-weight: 500;
  color: #09090b;
  text-decoration: none;
}
.forgot:hover {
  text-decoration: underline;
}

.error-box {
  padding: 0.75rem;
  font-size: 0.875rem;
  color: #f87171;
  background-color: rgba(69, 10, 10, 0.2);
  border: 1px solid rgba(127, 29, 29, 0.3);
  border-radius: 0.5rem;
}

.submit-btn {
  width: 100%;
  height: 3rem;
  background-color: #09090b;
  color: #ffffff;
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);
}
.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 10px -1px rgba(0,0,0,0.15), 0 2px 4px -1px rgba(0,0,0,0.05);
  background-color: #18181b;
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.signup-link {
  text-align: center;
  font-size: 0.875rem;
  color: #71717a;
  margin-top: 2rem;
  opacity: 0;
  animation: floatUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: 0.2s;
}
.signup-link a {
  color: #09090b;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
}
.signup-link a:hover {
  text-decoration: underline;
}
@keyframes floatUp {
  0% { opacity: 0; transform: translateY(12px); }
  100% { opacity: 1; transform: translateY(0); }
}</style>
