<script setup lang="ts">
/**
 * 试卷编辑页（SPEC-exam-paper）
 * 手动组卷：左侧课程题库选题，右侧调整顺序与分值；总分实时汇总，保存为草稿。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { paperApi } from '@/api/paper'
import { questionApi, type Question, type QuestionType } from '@/api/question'
import { courseApi, type Course } from '@/api'

const route = useRoute()
const router = useRouter()

const TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空',
  SHORT_ANSWER: '简答', ESSAY: '论述'
}

const courses = ref<Course[]>([])
const pool = ref<Question[]>([])
const poolLoading = ref(false)

const paperId = route.params.id ? Number(route.params.id) : null
const isEdit = paperId !== null
const saving = ref(false)
const loaded = ref(!isEdit)

const meta = reactive({
  name: '',
  courseId: undefined as number | undefined
})

const query = reactive({
  type: '' as QuestionType | '',
  difficulty: '' as number | '',
  keyword: ''
})

/** 已选题目：questionId + 分值，顺序即题目顺序 */
const items = ref<{ questionId: number; score: number; question: Question }[]>([])

const totalScore = computed(() =>
  Math.round(items.value.reduce((sum, it) => sum + (it.score || 0), 0) * 10) / 10
)

const inPaperIds = computed(() => new Set(items.value.map(it => it.questionId)))

async function fetchCourses() {
  courses.value = await courseApi.getAll().then(res => res.data)
}

async function fetchPool() {
  if (!meta.courseId) return
  poolLoading.value = true
  try {
    const res = await questionApi.page({
      courseId: meta.courseId,
      type: query.type || undefined,
      difficulty: query.difficulty === '' ? undefined : Number(query.difficulty),
      keyword: query.keyword || undefined,
      page: 0,
      size: 50
    })
    pool.value = res.content
  } catch {
    ElMessage.error('加载题库失败')
  } finally {
    poolLoading.value = false
  }
}

async function loadPaper() {
  try {
    const paper = await paperApi.get(paperId!)
    if (paper.status === 'PUBLISHED') {
      ElMessage.warning('已发布试卷不可编辑')
      router.replace('/papers')
      return
    }
    meta.name = paper.name
    meta.courseId = paper.courseId
    items.value = (paper.items ?? []).map(it => ({
      questionId: it.questionId!,
      score: it.score ?? 0,
      question: {
        id: it.questionId,
        courseId: paper.courseId,
        type: it.type as QuestionType,
        content: it.content ?? '',
        options: it.options ?? [],
        answer: it.answer ?? '',
        score: it.score ?? 0,
        difficulty: 1
      }
    }))
    loaded.value = true
    fetchPool()
  } catch {
    ElMessage.error('加载试卷失败')
    router.replace('/papers')
  }
}

function addToPaper(question: Question) {
  if (inPaperIds.value.has(question.id!)) {
    ElMessage.warning('该题目已在试卷中')
    return
  }
  items.value.push({ questionId: question.id!, score: Number(question.score) || 5, question })
}

function removeFromPaper(index: number) {
  items.value.splice(index, 1)
}

function moveUp(index: number) {
  if (index <= 0) return
  const [it] = items.value.splice(index, 1)
  items.value.splice(index - 1, 0, it)
}

function moveDown(index: number) {
  if (index >= items.value.length - 1) return
  const [it] = items.value.splice(index, 1)
  items.value.splice(index + 1, 0, it)
}

function validate(): string | null {
  if (!meta.name.trim()) return '请输入试卷名称'
  if (!meta.courseId) return '请选择归属课程'
  if (!items.value.length) return '请至少选择一道题目'
  if (items.value.some(it => !it.score || it.score <= 0)) return '每题分值必须大于 0'
  return null
}

async function handleSave() {
  const error = validate()
  if (error) {
    ElMessage.warning(error)
    return
  }
  saving.value = true
  try {
    const payload = {
      name: meta.name.trim(),
      courseId: meta.courseId!,
      items: items.value.map((it, idx) => ({
        questionId: it.questionId,
        seq: idx + 1,
        score: it.score
      }))
    }
    if (isEdit) {
      await paperApi.update(paperId!, payload)
      ElMessage.success('试卷已保存')
    } else {
      await paperApi.createManual(payload)
      ElMessage.success('试卷创建成功')
    }
    router.push('/papers')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '保存失败')
  } finally {
    saving.value = false
  }
}

function onCourseChange() {
  // 切换课程后题库变化，已选题目若不属于新课程会被后端拒绝，这里直接清空并提示
  if (items.value.length) {
    ElMessage.warning('已清空已选题目（题目只能来自试卷归属课程）')
    items.value = []
  }
  fetchPool()
}

onMounted(async () => {
  await fetchCourses()
  if (isEdit) {
    loadPaper()
  } else {
    meta.courseId = courses.value[0]?.id
    fetchPool()
  }
})
</script>

<template>
  <div class="page-container" v-if="loaded">
    <!-- 试卷信息 -->
    <el-card class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="试卷名称">
          <el-input v-model="meta.name" placeholder="输入试卷名称" maxlength="128" style="width: 260px" />
        </el-form-item>
        <el-form-item label="归属课程">
          <el-select
            v-model="meta.courseId"
            placeholder="选择课程"
            style="width: 180px"
            :disabled="isEdit"
            @change="onCourseChange"
          >
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id!" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-tag type="info" effect="plain">已选 {{ items.length }} 题，总分 {{ totalScore }} 分</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button @click="router.push('/papers')">返回</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存草稿</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="edit-layout">
      <!-- 左侧：题库选题 -->
      <el-card class="pool-card" v-loading="poolLoading">
        <template #header>
          <div class="card-header">
            <span>题库选题</span>
            <div class="pool-filter">
              <el-select v-model="query.type" placeholder="题型" clearable size="small" style="width: 90px"
                @change="fetchPool">
                <el-option label="单选" value="SINGLE" />
                <el-option label="多选" value="MULTI" />
                <el-option label="判断" value="JUDGE" />
                <el-option label="填空" value="FILL" />
              </el-select>
              <el-select v-model="query.difficulty" placeholder="难度" clearable size="small" style="width: 80px"
                @change="fetchPool">
                <el-option label="易" :value="1" />
                <el-option label="中" :value="2" />
                <el-option label="难" :value="3" />
              </el-select>
              <el-input v-model="query.keyword" placeholder="题干关键词" size="small" clearable style="width: 130px"
                @keyup.enter="fetchPool" @clear="fetchPool" />
              <el-button size="small" type="primary" @click="fetchPool">筛选</el-button>
            </div>
          </div>
        </template>

        <el-empty v-if="!meta.courseId" description="请先选择课程" />
        <el-table v-else :data="pool" stripe size="small" max-height="520">
          <el-table-column label="题型" width="70">
            <template #default="{ row }">
              <el-tag size="small">{{ TYPE_LABELS[row.type as QuestionType] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="题干" prop="content" min-width="200" show-overflow-tooltip />
          <el-table-column label="难度" width="60">
            <template #default="{ row }">{{ ['易', '中', '难'][row.difficulty - 1] }}</template>
          </el-table-column>
          <el-table-column label="默认分" prop="score" width="70" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!inPaperIds.has(row.id)" link type="primary" @click="addToPaper(row)">加入</el-button>
              <el-tag v-else size="small" type="success">已选</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 右侧：已选题目 -->
      <el-card class="selected-card">
        <template #header>
          <span>已选题目（按顺序组卷）</span>
        </template>
        <el-empty v-if="!items.length" description="从左侧题库加入题目" />
        <div v-else class="selected-list">
          <div v-for="(it, idx) in items" :key="it.questionId" class="selected-row">
            <span class="seq">{{ idx + 1 }}</span>
            <el-tag size="small" class="type-tag">{{ TYPE_LABELS[it.question.type] }}</el-tag>
            <span class="content" :title="it.question.content">{{ it.question.content }}</span>
            <el-input-number v-model="it.score" :min="0.5" :max="100" :step="0.5" size="small"
              controls-position="right" class="score-input" />
            <div class="row-actions">
              <el-button link size="small" :disabled="idx === 0" @click="moveUp(idx)">↑</el-button>
              <el-button link size="small" :disabled="idx === items.length - 1" @click="moveDown(idx)">↓</el-button>
              <el-button link size="small" type="danger" @click="removeFromPaper(idx)">移除</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.edit-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.pool-filter {
  display: flex;
  align-items: center;
  gap: 6px;
}

.selected-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 520px;
  overflow-y: auto;
}

.selected-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid rgba(0, 255, 255, 0.12);
  border-radius: 6px;
  background: rgba(0, 255, 255, 0.02);
}

.seq {
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background: rgba(0, 255, 255, 0.12);
  color: #00ffff;
  font-size: 12px;
  flex-shrink: 0;
}

.type-tag {
  flex-shrink: 0;
}

.content {
  flex: 1;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.score-input {
  width: 110px;
  flex-shrink: 0;
}

.row-actions {
  display: flex;
  flex-shrink: 0;
}
</style>
