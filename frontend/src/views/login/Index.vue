<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">实训成果智能核查与评价系统</h2>
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
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store'
import { login, getCaptcha } from '@/api/auth'
import type { LoginRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const showCaptcha = ref(false)
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
    captchaImage.value = 'data:image/png;base64,' + res.data.image
  } catch {
    // 验证码获取失败不影响登录
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (showCaptcha.value && !form.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const res = await login(form)
    userStore.setLogin(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    loginFailCount = 0

    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: unknown) {
    loginFailCount++
    // 失败3次后强制显示验证码
    if (loginFailCount >= 3 && !showCaptcha.value) {
      showCaptcha.value = true
      refreshCaptcha()
    }
    // 验证码用过后刷新
    if (showCaptcha.value) {
      refreshCaptcha()
      form.captchaCode = ''
    }
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);

  .login-title {
    text-align: center;
    margin-bottom: 32px;
    color: #303133;
    font-size: 22px;
  }

  .login-btn {
    width: 100%;
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
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      flex-shrink: 0;
    }
  }
}
</style>
