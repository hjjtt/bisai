<template>
  <div class="template-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评价模板管理</span>
          <div class="header-actions">
            <el-select
              v-model="filterStatus"
              placeholder="按状态筛选"
              clearable
              style="width: 150px; margin-right: 12px"
              @change="handleFilter"
            >
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
            <el-button type="primary" @click="handleCreate">新建模板</el-button>
          </div>
        </div>
      </template>

      <el-table :data="templates" stripe v-loading="loading">
        <el-table-column prop="name" label="模板名称" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="总分" width="80" align="center">
          <template #default="{ row }">
            {{ row.totalScore }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="loadTemplates"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑模板' : '新建模板'"
      width="800px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入模板描述" />
        </el-form-item>

        <el-divider content-position="left">评价指标</el-divider>

        <div class="indicator-section">
          <el-button type="primary" size="small" @click="addIndicator" style="margin-bottom: 12px">
            添加指标
          </el-button>
          <el-table :data="form.indicators" border size="small">
            <el-table-column label="指标名称" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="指标名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="权重" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.weight" :min="0" :max="100" :precision="1" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="满分" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.maxScore" :min="0" :precision="1" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="评分规则" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.scoreRule" placeholder="评分规则" size="small" type="textarea" :rows="2" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeIndicator($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getTemplateList, getTemplate, createTemplate, updateTemplate, deleteTemplate } from '@/api/course'
import type { EvaluationTemplate, Indicator } from '@/types'

const loading = ref(false)
const saving = ref(false)
const templates = ref<EvaluationTemplate[]>([])
const filterStatus = ref('')
const pagination = reactive({ page: 1, size: 20, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  status: 'ENABLED' as string,
  description: '',
  indicators: [] as Partial<Indicator>[],
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { max: 100, message: '模板名称不能超过100个字符', trigger: 'blur' },
  ],
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

async function loadTemplates() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: pagination.page, size: pagination.size }
    if (filterStatus.value) {
      params.status = filterStatus.value
    }
    const res = await getTemplateList(params)
    templates.value = res.data.items
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

function handleFilter() {
  pagination.page = 1
  loadTemplates()
}

function handleCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

async function handleEdit(row: EvaluationTemplate) {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.status = row.status
  form.description = row.description || ''
  form.indicators = []

  // 加载模板详情（含指标）
  try {
    const res = await getTemplate(row.id)
    form.indicators = res.data.indicators || []
  } catch {
    ElMessage.error('加载模板详情失败')
    return
  }

  dialogVisible.value = true
}

async function handleDelete(row: EvaluationTemplate) {
  try {
    await ElMessageBox.confirm(`确定删除模板"${row.name}"？删除后不可恢复。`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteTemplate(row.id)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch {
    // 用户取消
  }
}

function resetForm() {
  form.id = 0
  form.name = ''
  form.status = 'ENABLED'
  form.description = ''
  form.indicators = []
  formRef.value?.clearValidate()
}

function addIndicator() {
  form.indicators.push({
    name: '',
    weight: 0,
    maxScore: 0,
    scoreRule: '',
  })
}

function removeIndicator(index: number) {
  form.indicators.splice(index, 1)
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const data = {
      name: form.name,
      status: form.status,
      description: form.description,
      indicators: form.indicators.map((ind) => ({
        name: ind.name,
        weight: ind.weight,
        maxScore: ind.maxScore,
        scoreRule: ind.scoreRule,
      })),
    }

    if (isEdit.value) {
      await updateTemplate(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await createTemplate(data)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadTemplates()
  } finally {
    saving.value = false
  }
}

onMounted(loadTemplates)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.indicator-section {
  margin-top: 8px;
}
</style>
