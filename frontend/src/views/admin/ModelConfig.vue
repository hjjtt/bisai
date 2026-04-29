<template>
  <div class="model-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模型配置</span>
          <el-tag type="success">当前接入: ModelScope (通义千问)</el-tag>
        </div>
      </template>

      <el-alert
        title="当前系统已接入 ModelScope AI 平台，使用通义千问大语言模型提供智能解析、核查和评分服务。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />

      <el-form :model="form" label-width="140px" style="max-width: 600px" v-loading="loading">
        <el-divider content-position="left">文本大模型（核心）</el-divider>
        <el-form-item label="API 地址">
          <el-input v-model="form.textModelApiUrl" placeholder="https://api-inference.modelscope.cn/v1" />
        </el-form-item>
        <el-form-item label="密钥">
          <el-input v-model="form.textModelApiKey" type="password" show-password placeholder="ms-xxxxx" />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="form.model" placeholder="Qwen/Qwen3.5-35B-A3B" />
        </el-form-item>
        <el-form-item label="超时时间(ms)">
          <el-input-number v-model="form.timeout" :min="5000" :max="120000" :step="5000" />
        </el-form-item>
        <el-form-item label="温度参数">
          <el-slider v-model="form.temperature" :min="0" :max="1" :step="0.1" :format-tooltip="(v: number) => v.toFixed(1)" style="width: 300px" />
        </el-form-item>
        <el-form-item label="最大 Token 数">
          <el-input-number v-model="form.maxTokens" :min="256" :max="8192" :step="256" />
        </el-form-item>

        <el-divider content-position="left">功能说明</el-divider>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="智能解析">读取学生提交文件内容，AI 提取关键信息和摘要</el-descriptions-item>
          <el-descriptions-item label="智能核查">AI 从完整性、规范性、原创性等维度核查成果</el-descriptions-item>
          <el-descriptions-item label="智能评分">根据评分模板指标，AI 自动打分并提供理由和证据</el-descriptions-item>
          <el-descriptions-item label="批量操作">对任务下所有提交批量触发 AI 解析和评分</el-descriptions-item>
        </el-descriptions>

        <el-form-item style="margin-top: 20px">
          <el-button type="primary" @click="handleTest" :loading="testing">测试连通性</el-button>
          <el-button type="success" @click="handleSave" :loading="saving">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, updateSystemConfig, testModelConnection } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)

const form = reactive({
  textModelApiUrl: 'https://api-inference.modelscope.cn/v1',
  textModelApiKey: '',
  model: 'Qwen/Qwen3.5-35B-A3B',
  timeout: 30000,
  temperature: 0.3,
  maxTokens: 4096,
})

async function loadConfig() {
  loading.value = true
  try {
    const res = await getSystemConfig()
    const data = res.data as any
    if (data.textModelApiUrl) form.textModelApiUrl = data.textModelApiUrl
    if (data.textModelApiKey) form.textModelApiKey = data.textModelApiKey
    if (data.model) form.model = data.model
    if (data.timeout) form.timeout = Number(data.timeout)
    if (data.temperature !== undefined) form.temperature = Number(data.temperature)
    if (data.maxTokens) form.maxTokens = Number(data.maxTokens)
  } catch {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

async function handleTest() {
  testing.value = true
  try {
    const res = await testModelConnection({ apiUrl: form.textModelApiUrl, apiKey: form.textModelApiKey })
    if (res.data.success) {
      ElMessage.success(res.data.message || '连通性测试通过')
    } else {
      ElMessage.error(res.data.message || '连通性测试失败')
    }
  } catch {
    ElMessage.error('连通性测试失败')
  } finally {
    testing.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateSystemConfig(form)
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
