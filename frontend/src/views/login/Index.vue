<template>
  <div class="login-container">
    <div class="login-card">
      <!-- Logo & Title -->
      <div class="login-header">
        <div class="logo">
          <svg viewBox="0 0 32 32" width="28" height="28" fill="none">
            <rect width="32" height="32" rx="8" fill="#2563eb" />
            <path d="M8 16.5l4 4 12-12" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span class="logo-text">实训成果智能核查与评价系统</span>
        </div>
        <!-- Tab 切换 -->
        <div class="auth-tabs">
          <button :class="['tab-btn', { active: mode === 'login' }]" @click="switchMode('login')">登录</button>
          <button :class="['tab-btn', { active: mode === 'register' }]" @click="switchMode('register')">注册</button>
        </div>
      </div>

      <!-- ========== 登录表单 ========== -->
      <template v-if="mode === 'login'">
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0" @submit.prevent="handleLogin" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              prefix-icon="User"
              size="large"
              class="form-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              prefix-icon="Lock"
              size="large"
              show-password
              class="form-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item v-if="showCaptcha" prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captchaCode"
                placeholder="验证码"
                prefix-icon="Key"
                size="large"
                class="form-input captcha-input"
                @keyup.enter="handleLogin"
              />
              <img
                :src="captchaImage"
                alt="验证码"
                class="captcha-img"
                @click="refreshCaptcha"
                title="点击刷新"
              />
            </div>
          </el-form-item>
          <el-form-item>
            <button type="button" class="login-btn" :disabled="loading" @click="handleLogin">
              <span v-if="!loading">登 录</span>
              <span v-else class="btn-loading">
                <svg class="spinner" viewBox="0 0 24 24" width="18" height="18">
                  <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="31.4 31.4" />
                </svg>
                登录中…
              </span>
            </button>
          </el-form-item>
        </el-form>
      </template>

      <!-- ========== 注册表单 ========== -->
      <template v-else>
        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0" @submit.prevent="handleRegister" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="用户名"
              prefix-icon="User"
              size="large"
              class="form-input"
            />
          </el-form-item>
          <el-form-item prop="realName">
            <el-input
              v-model="registerForm.realName"
              placeholder="真实姓名"
              prefix-icon="UserFilled"
              size="large"
              class="form-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="密码（至少8位，含字母和数字）"
              prefix-icon="Lock"
              size="large"
              show-password
              class="form-input"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="确认密码"
              prefix-icon="Lock"
              size="large"
              show-password
              class="form-input"
            />
          </el-form-item>
          <el-form-item prop="role">
            <el-select v-model="registerForm.role" placeholder="选择角色" size="large" class="form-input" style="width: 100%">
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="registerForm.role === 'STUDENT'" prop="classId">
            <el-select v-model="registerForm.classId" placeholder="选择班级" size="large" class="form-input" style="width: 100%">
              <el-option v-for="cls in classList" :key="cls.id" :label="cls.name" :value="cls.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <button type="button" class="login-btn" :disabled="loading" @click="handleRegister">
              <span v-if="!loading">注 册</span>
              <span v-else class="btn-loading">
                <svg class="spinner" viewBox="0 0 24 24" width="18" height="18">
                  <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="31.4 31.4" />
                </svg>
                注册中…
              </span>
            </button>
          </el-form-item>
        </el-form>
      </template>

      <div class="login-footer">
        <span>实训成果智能核查与评价系统</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store'
import { login, register, getCaptcha, getClassesForRegister } from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)

// ========== 登录 ==========
const loginFormRef = ref<FormInstance>()
const showCaptcha = ref(true)
const captchaImage = ref('')

const loginForm = reactive<LoginRequest>({
  username: '',
  password: '',
  captchaUuid: '',
  captchaCode: '',
})

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    loginForm.captchaUuid = res.data.uuid
    captchaImage.value = res.data.image.startsWith('data:')
      ? res.data.image
      : 'data:image/png;base64,' + res.data.image
  } catch {
    // 验证码获取失败不影响登录
  }
}

refreshCaptcha()

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (showCaptcha.value && !loginForm.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const loginData: LoginRequest = {
      username: loginForm.username,
      password: loginForm.password,
      captchaUuid: loginForm.captchaUuid,
      captchaCode: loginForm.captchaCode,
    }
    const res = await login(loginData)
    userStore.setLogin(res.data.token, res.data.user)
    ElMessage.success('登录成功')

    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: unknown) {
    refreshCaptcha()
    loginForm.captchaCode = ''
  } finally {
    loading.value = false
  }
}

// ========== 注册 ==========
const registerFormRef = ref<FormInstance>()
const classList = ref<{ id: number; name: string }[]>([])

const registerForm = reactive<RegisterRequest & { confirmPassword: string }>({
  username: '',
  password: '',
  realName: '',
  role: 'STUDENT',
  classId: undefined,
  confirmPassword: '',
})

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 个字符', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)/, message: '密码需包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== registerForm.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  classId: [
    {
      validator: (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
        if (registerForm.role === 'STUDENT' && !value) {
          callback(new Error('学生请选择班级'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

async function loadClasses() {
  try {
    const res = await getClassesForRegister()
    classList.value = res.data
  } catch {
    // 班级列表加载失败不阻塞
  }
}

function switchMode(m: 'login' | 'register') {
  mode.value = m
  if (m === 'register' && classList.value.length === 0) {
    loadClasses()
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      realName: registerForm.realName,
      role: registerForm.role,
      classId: registerForm.role === 'STUDENT' ? registerForm.classId : undefined,
    })
    ElMessage.success('注册成功，请登录')
    // 自动填充用户名，切回登录
    loginForm.username = registerForm.username
    loginForm.password = ''
    mode.value = 'login'
    refreshCaptcha()
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
$primary: #2563eb;
$primary-hover: #1d4ed8;
$primary-light: #eff6ff;
$text-main: #111827;
$text-muted: #6b7280;
$text-faint: #9ca3af;
$border: #e5e7eb;
$bg-page: #f9fafb;
$bg-input: #f9fafb;

/* ==================== 容器 ==================== */
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-page;
}

/* ==================== 卡片 ==================== */
.login-card {
  width: 100%;
  max-width: 400px;
  padding: 48px 40px 40px;
  background: #fff;
  border: 1px solid $border;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* ==================== 头部 ==================== */
.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;

  .logo-text {
    font-size: 16px;
    font-weight: 600;
    color: $text-main;
    letter-spacing: 0;
  }
}

/* ==================== Tab 切换 ==================== */
.auth-tabs {
  display: flex;
  gap: 0;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 3px;
}

.tab-btn {
  flex: 1;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  color: $text-muted;
  cursor: pointer;
  transition: all 0.2s ease;

  &.active {
    background: #fff;
    color: $primary;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  }

  &:hover:not(.active) {
    color: $text-main;
  }
}

/* ==================== 表单 ==================== */
.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 10px !important;
    padding: 4px 14px !important;
    box-shadow: 0 0 0 1px $border inset !important;
    background: $bg-input;
    transition: all 0.2s ease !important;

    &:hover {
      box-shadow: 0 0 0 1px #d1d5db inset !important;
    }

    &.is-focus {
      box-shadow: 0 0 0 1.5px $primary inset !important;
      background: #fff;
    }
  }

  :deep(.el-input__prefix .el-icon) {
    color: $text-faint;
    font-size: 16px;
  }
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
  align-items: center;

  .captcha-input {
    flex: 1;
  }

  .captcha-img {
    height: 40px;
    width: 120px;
    cursor: pointer;
    border: 1px solid $border;
    border-radius: 10px;
    flex-shrink: 0;
    transition: border-color 0.2s;
    background: $bg-input;

    &:hover {
      border-color: $primary;
    }
  }
}

/* ==================== 按钮 ==================== */
.login-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: $primary;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 4px;

  &:hover:not(:disabled) {
    background: $primary-hover;
  }

  &:active:not(:disabled) {
    transform: scale(0.99);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  .btn-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  .spinner {
    animation: spin 0.8s linear infinite;
  }
}

/* ==================== 底部 ==================== */
.login-footer {
  margin-top: 32px;
  text-align: center;
  font-size: 12px;
  color: $text-faint;
}

/* ==================== 动画 ==================== */
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ==================== 响应式 ==================== */
@media (max-width: 480px) {
  .login-container {
    padding: 16px;
  }

  .login-card {
    padding: 36px 24px 32px;
    border-radius: 12px;
    border: none;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  }
}
</style>
