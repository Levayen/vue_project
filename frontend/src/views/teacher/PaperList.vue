<script setup lang="ts">
/**
 * 试卷列表页（SPEC-exam-paper）
 * 支持手动组卷入口、随机组卷（规则预览）、草稿编辑/发布、详情查看与删除。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { paperApi, type DrawRule, type Paper } from '@/api/paper'
import type { QuestionType } from '@/api/question'
import { courseApi, type Course } from '@/api'

const router = useRouter()

const TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', FILL: '填空题',
  SHORT_ANSWER: '简答题', ESSAY: '论述题'
}

const courses = ref<Course[]>([])
const list = ref<Paper[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({ courseId: undefined as number | undefined, page: 1, size: 10 })

async function fetchCourses() {
  courses.value = await courseApi.getAll().then(res => res.data)
}

async function fetchList() {
  loading.value = true
  try {
    const res = await paperApi.page({
      courseId: query.courseId || undefined,
      page: query.page - 1,
      size: query.size
    })
    list.value = res.content
    total.value = res.totalElements
  } catch {
    ElMessage.error('加载试卷失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  fetchList()
}

// ---------- 随机组卷 ----------

const randomVisible = ref(false)
const randomSaving = ref(false)
const randomForm = reactive({
  name: '',
  courseId: undefined as number | undefined,
  rules: [] as DrawRule[]
})

const randomPreview = computed(() => {
  const count = randomForm.rules.reduce((sum, r) => sum + (r.count || 0), 0)
  const score = randomForm.rules.reduce((sum, r) => sum + (r.count || 0) * (r.scorePerQuestion || 0), 0)
  return { count, score: Math.round(score * 10) / 10 }
})

function openRandom() {
  randomForm.name = ''
  randomForm.courseId = query.courseId || courses.value[0]?.id
  randomForm.rules = [{ type: 'SINGLE', difficulty: null, count: 10, scorePerQuestion: 2 }]
  randomVisible.value = true
}

function addRule() {
  randomForm.rules.push({ type: 'SINGLE', difficulty: null, count: 5, scorePerQuestion: 2 })
}

function removeRule(index: number) {
  randomForm.rules.splice(index, 1)
}

function validateRandom(): string | null {
  if (!randomForm.name.trim()) return '请输入试卷名称'
  if (!randomForm.courseId) return '请选择归属课程'
  if (!randomForm.rules.length) return '请至少设置一条抽题规则'
  for (const [i, r] of randomForm.rules.entries()) {
    if (!r.count || r.count < 1) return `第 ${i + 1} 条规则抽题数量必须大于 0`
    if (!r.scorePerQuestion || r.scorePerQuestion <= 0) return `第 ${i + 1} 条规则每题分值必须大于 0`
  }
  const types = randomForm.rules.map(r => r.type)
  if (new Set(types).size !== types.length) return '同一题型只能设置一条抽题规则'
  return null
}

async function handleRandomSave() {
  const error = validateRandom()
  if (error) {
    ElMessage.warning(error)
    return
  }
  randomSaving.value = true
  try {
    await paperApi.createRandom({
      name: randomForm.name.trim(),
      courseId: randomForm.courseId!,
      rules: randomForm.rules
    })
    ElMessage.success('随机组卷成功')
    randomVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '随机组卷失败')
  } finally {
    randomSaving.value = false
  }
}

// ---------- 详情查看 ----------

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailPaper = ref<Paper | null>(null)

async function openDetail(paper: Paper) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailPaper.value = await paperApi.get(paper.id!)
  } catch {
    ElMessage.error('加载试卷详情失败')
  } finally {
    detailLoading.value = false
  }
}

function answerDisplay(row: { answer?: string; referenceAnswer?: string | null; type?: QuestionType }): string {
  // 主观题展示参考答案
  if (row.type === 'SHORT_ANSWER' || row.type === 'ESSAY') {
    return row.referenceAnswer ? `参考答案：${row.referenceAnswer}` : '未提供参考答案'
  }
  const answer = row.answer
  if (!answer) return ''
  if (row.type === 'JUDGE') return answer === 'T' ? '正确' : '错误'
  if (row.type === 'FILL') return answer.split('||').join(' 或 ')
  return answer
}

// ---------- 发布 / 删除 ----------

async function handlePublish(paper: Paper) {
  try {
    await ElMessageBox.confirm(`确定发布试卷「${paper.name}」吗？发布后不可再编辑。`, '发布确认', {
      confirmButtonText: '发布', cancelButtonText: '取消', type: 'warning'
    })
    await paperApi.publish(paper.id!)
    ElMessage.success('试卷已发布')
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel' && e?.response) {
      ElMessage.error(e.response?.data?.message ?? '发布失败')
    }
  }
}

async function handleDelete(paper: Paper) {
  try {
    await ElMessageBox.confirm(`确定删除试卷「${paper.name}」吗？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await paperApi.remove(paper.id!)
    ElMessage.success('试卷已删除')
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel' && e?.response) {
      ElMessage.error(e.response?.data?.message ?? '删除失败')
    }
  }
}

onMounted(() => {
  fetchCourses()
  fetchList()
})
</script>

<template>
  <div class="page-container">
    <!-- 查询栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="课程">
          <el-select v-model="query.courseId" placeholder="全部课程" clearable style="width: 180px">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id!" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button type="success" @click="router.push('/papers/new')">新建试卷</el-button>
          <el-button type="warning" @click="openRandom">随机组卷</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 试卷列表 -->
    <el-card v-loading="loading">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="试卷名称" prop="name" min-width="180" show-overflow-tooltip />
        <el-table-column label="课程" prop="courseName" width="120" />
        <el-table-column label="总分" width="80">
          <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="组卷方式" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.generateType === 'MANUAL' ? 'primary' : 'warning'">
              {{ row.generateType === 'MANUAL' ? '手动' : '随机' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ (row.createTime ?? '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary"
              @click="router.push(`/papers/${row.id}/edit`)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="success" @click="handlePublish(row)">发布</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="(p: number) => { query.page = p; fetchList() }"
      />
    </el-card>

    <!-- 随机组卷弹窗 -->
    <el-dialog v-model="randomVisible" title="随机组卷" width="760px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="试卷名称" required>
          <el-input v-model="randomForm.name" placeholder="输入试卷名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="归属课程" required>
          <el-select v-model="randomForm.courseId" placeholder="选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="抽题规则" required>
          <div class="rules-area">
            <div v-for="(rule, idx) in randomForm.rules" :key="idx" class="rule-row">
              <span class="rule-label">题型</span>
              <el-select v-model="rule.type" style="width: 110px">
                <el-option label="单选题" value="SINGLE" />
                <el-option label="多选题" value="MULTI" />
                <el-option label="判断题" value="JUDGE" />
                <el-option label="填空题" value="FILL" />
                <el-option label="简答题" value="SHORT_ANSWER" />
                <el-option label="论述题" value="ESSAY" />
              </el-select>
              <span class="rule-label">难度</span>
              <el-select v-model="rule.difficulty" style="width: 100px" placeholder="不限">
                <el-option label="不限" :value="null" />
                <el-option label="易" :value="1" />
                <el-option label="中" :value="2" />
                <el-option label="难" :value="3" />
              </el-select>
              <span class="rule-label">数量</span>
              <el-input-number v-model="rule.count" :min="1" :max="200" controls-position="right" />
              <span class="rule-label">每题分</span>
              <el-input-number v-model="rule.scorePerQuestion" :min="0.5" :max="100" :step="0.5"
                controls-position="right" />
              <el-button link type="danger" @click="removeRule(idx)">移除</el-button>
            </div>
            <el-button size="small" @click="addRule">+ 添加规则</el-button>
            <span class="rule-tip">同一题型只能设置一条规则；题量不足时组卷会整体失败</span>
          </div>
        </el-form-item>
        <el-form-item label="规则预览">
          <el-tag type="info" effect="plain">
            预计共 {{ randomPreview.count }} 题，总分 {{ randomPreview.score }} 分
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="randomVisible = false">取消</el-button>
        <el-button type="primary" :loading="randomSaving" @click="handleRandomSave">生成试卷</el-button>
      </template>
    </el-dialog>

    <!-- 试卷详情弹窗 -->
    <el-dialog v-model="detailVisible" title="试卷详情" width="760px" append-to-body>
      <div v-loading="detailLoading">
        <template v-if="detailPaper">
          <el-descriptions :column="3" border size="small" class="detail-meta">
            <el-descriptions-item label="试卷名称">{{ detailPaper.name }}</el-descriptions-item>
            <el-descriptions-item label="课程">{{ detailPaper.courseName }}</el-descriptions-item>
            <el-descriptions-item label="总分">{{ detailPaper.totalScore }} 分</el-descriptions-item>
            <el-descriptions-item label="组卷方式">
              {{ detailPaper.generateType === 'MANUAL' ? '手动' : '随机' }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ detailPaper.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-descriptions-item>
            <el-descriptions-item label="题目数">{{ detailPaper.items?.length ?? 0 }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="detailPaper.items ?? []" stripe size="small" max-height="420">
            <el-table-column label="#" prop="seq" width="50" />
            <el-table-column label="题型" width="80">
              <template #default="{ row }">
                <el-tag size="small">{{ TYPE_LABELS[row.type as QuestionType] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="题干" prop="content" min-width="220" show-overflow-tooltip />
            <el-table-column label="分值" prop="score" width="70" />
            <el-table-column label="答案" width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ answerDisplay(row) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.rules-area {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rule-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rule-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
}

.rule-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.detail-meta {
  margin-bottom: 14px;
}
</style>
