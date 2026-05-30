<template>
  <div class="login-container">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-icon">
          <el-icon :size="40"><Monitor /></el-icon>
        </div>
        <h1 class="brand-title">实训成果智能核查与评价系统</h1>
        <p class="brand-subtitle">BisAI Evaluation Platform</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon :size="18"><Document /></el-icon>
            <span>AI 智能解析</span>
          </div>
          <div class="feature-item">
            <el-icon :size="18"><CircleCheck /></el-icon>
            <span>智能核查评分</span>
          </div>
          <div class="feature-item">
            <el-icon :size="18"><TrendCharts /></el-icon>
            <span>多维度评价</span>
          </div>
        </div>
      </div>
      <!-- 装饰圆 -->
      <div class="deco deco-1"></div>
      <div class="deco deco-2"></div>
      <div class="deco deco-3"></div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-form-area">
      <div class="login-card">
        <h2 class="login-title">欢迎登录</h2>
        <p class="login-desc">请输入您的账号信息</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item v-if="showCaptcha" prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                prefix-icon="Key"
                size="large"
                class="captcha-input"
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
            <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Monitor, Document, CircleCheck, TrendCharts } from '@element-plus/icons-vue'
import { useUserStore } from '@/store'
import { login, getCaptcha } from '@/api/auth'
import type { LoginRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const showCaptcha = ref(true)
const captchaImage = ref('')

const form = reactive<LoginRequest>({
  username: '',
  password: '',
  captchaUuid: '',
  captchaCode: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

let loginFailCount = 0

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    form.captchaUuid = res.data.uuid
    captchaImage.value = res.data.image.startsWith('data:')
      ? res.data.image
      : 'data:image/png;base64,' + res.data.image
  } catch {
    // 验证码获取失败不影响登录
  }
}

refreshCaptcha()

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (showCaptcha.value && !form.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const loginData: LoginRequest = {
      username: form.username,
      password: form.password,
      captchaUuid: form.captchaUuid,
      captchaCode: form.captchaCode,
    }
    const res = await login(loginData)
    userStore.setLogin(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    loginFailCount = 0

    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: unknown) {
    loginFailCount++
    refreshCaptcha()
    form.captchaCode = ''
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
$primary: #2563eb;
$primary-light: #3b82f6;
$primary-dark: #1d4ed8;
$text-main: #1e293b;
$text-muted: #64748b;
$bg-body: #f6f8fb;

.login-container {
  height: 100vh;
  display: flex;
  background: $bg-body;
}

/* ---- 左侧品牌区 ---- */
.login-brand {
  flex: 1;
  background: linear-gradient(160deg, $primary-dark 0%, $primary 55%, $primary-light 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  min-height: 100vh;

  .brand-content {
    position: relative;
    z-index: 2;
    text-align: center;
    color: #fff;
    padding: 0 48px;
  }

  .brand-icon {
    width: 72px;
    height: 72px;
    margin: 0 auto 24px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(8px);
  }

  .brand-title {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.4;
    margin-bottom: 8px;
    letter-spacing: 0.5px;
  }

  .brand-subtitle {
    font-size: 14px;
    opacity: 0.7;
    letter-spacing: 2px;
    margin-bottom: 48px;
  }

  .brand-features {
    display: flex;
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
    max-width: 260px;
    margin: 0 auto;

    .feature-item {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 15px;
      font-weight: 500;
      opacity: 0.9;
    }
  }

  /* 装饰圆 */
  .deco {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
  }
  .deco-1 {
    width: 400px;
    height: 400px;
    top: -120px;
    left: -100px;
  }
  .deco-2 {
    width: 300px;
    height: 300px;
    bottom: -80px;
    right: -60px;
  }
  .deco-3 {
    width: 160px;
    height: 160px;
    top: 50%;
    right: 10%;
    background: rgba(255, 255, 255, 0.04);
  }
}

/* ---- 右侧登录区 ---- */
.login-form-area {
  width: 520px;
  min-width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: 0 40px;

  .login-title {
    font-size: 26px;
    font-weight: 700;
    color: $text-main;
    margin-bottom: 8px;
  }

  .login-desc {
    font-size: 14px;
    color: $text-muted;
    margin-bottom: 36px;
  }

  .login-btn {
    width: 100%;
    height: 44px;
    font-size: 15px;
    font-weight: 600;
    border-radius: 8px;
    margin-top: 4px;
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
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      flex-shrink: 0;
      transition: border-color 0.2s;

      &:hover {
        border-color: $primary;
      }
    }
  }
}

/* ---- 响应式 ---- */
@media (max-width: 900px) {
  .login-container {
    flex-direction: column;
  }

  .login-brand {
    min-height: auto;
    padding: 48px 24px 32px;

    .brand-title {
      font-size: 22px;
    }

    .brand-subtitle {
      margin-bottom: 0;
    }

    .brand-features {
      display: none;
    }

    .deco {
      display: none;
    }
  }

  .login-form-area {
    width: 100%;
    min-width: unset;
    flex: 1;

    .login-card {
      max-width: 420px;
      padding: 32px 24px;
    }
  }
}
</style>
