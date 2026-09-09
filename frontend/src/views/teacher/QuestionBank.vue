<script setup lang="ts">
/**
 * 题库管理页（SPEC-question-bank）
 * 教师按课程维护四类客观题；表单按题型动态渲染。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionApi, isSubjective } from '@/api/question'
import type { Question, QuestionOption, QuestionType } from '@/api/question'
import { courseApi, type Course } from '@/api'

const TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', FILL: '填空题',
  SHORT_ANSWER: '简答题', ESSAY: '论述题'
}
const DIFFICULTY_LABELS: Record<number, string> = { 1: '易', 2: '中', 3: '难' }
const DIFFICUNT_TAG_TYPE: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'danger' }
const OPTION_KEYS = 'ABCDEFGHIJ'

const courses = ref<Course[]>([])
const list = ref<Question[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  courseId: undefined as number | undefined,
  type: '' as QuestionType | '',
  difficulty: '' as number | '',
  keyword: '',
  page: 1,
  size: 10
})

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

interface QuestionForm {
  courseId: number | undefined
  type: QuestionType
  content: string
  options: QuestionOption[]
  score: number
  difficulty: number
  analysis: string
  singleAnswer: string
  judgeAnswer: string
  multiAnswers: string[]
  fillAnswers: string[]
  /** 主观题参考答案（M7） */
  referenceAnswer: string
}

const defaultForm = (): QuestionForm => ({
  courseId: undefined,
  type: 'SINGLE',
  content: '',
  options: [
    { key: 'A', text: '' },
    { key: 'B', text: '' }
  ],
  score: 5,
  difficulty: 1,
  analysis: '',
  singleAnswer: '',
  judgeAnswer: 'T',
  multiAnswers: [],
  fillAnswers: [''],
  referenceAnswer: ''
})

const form = reactive<QuestionForm>(defaultForm())

const isChoice = computed(() => form.type === 'SINGLE' || form.type === 'MULTI')
/** 主观题（简答/论述）：无选项无客观答案，人工阅卷 */
const isSubjectiveForm = computed(() => isSubjective(form.type))

async function fetchCourses() {
  courses.value = await courseApi.getAll().then(res => res.data)
}

async function fetchList() {
  loading.value = true
  try {
    const res = await questionApi.page({
      courseId: query.courseId || undefined,
      type: query.type || undefined,
      difficulty: query.difficulty === '' ? undefined : Number(query.difficulty),
      keyword: query.keyword || undefined,
      page: query.page - 1,
      size: query.size
    })
    list.value = res.content
    total.value = res.totalElements
  } catch {
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  fetchList()
}

function resetForm() {
  Object.assign(form, defaultForm())
}

function openCreate() {
  resetForm()
  editingId.value = null
  form.courseId = query.courseId || courses.value[0]?.id
  dialogVisible.value = true
}

function openEdit(q: Question) {
  resetForm()
  editingId.value = q.id!
  form.courseId = q.courseId
  form.type = q.type
  form.content = q.content
  form.options = q.options?.length ? q.options.map(o => ({ ...o })) : defaultForm().options
  form.score = q.score
  form.difficulty = q.difficulty
  form.analysis = q.analysis ?? ''
  if (q.type === 'SINGLE') form.singleAnswer = q.answer
  if (q.type === 'MULTI') form.multiAnswers = q.answer.split('')
  if (q.type === 'JUDGE') form.judgeAnswer = q.answer
  if (q.type === 'FILL') form.fillAnswers = q.answer.split('||')
  // 主观题：参考答案由后端通过 answer 字段下发
  if (isSubjective(q.type)) form.referenceAnswer = q.answer ?? ''
  dialogVisible.value = true
}

function onTypeChange() {
  form.singleAnswer = ''
  form.multiAnswers = []
  form.judgeAnswer = 'T'
  form.fillAnswers = ['']
  form.referenceAnswer = ''
  form.options = [
    { key: 'A', text: '' },
    { key: 'B', text: '' }
  ]
}

function addOption() {
  if (form.options.length >= OPTION_KEYS.length) return
  form.options.push({ key: OPTION_KEYS[form.options.length], text: '' })
}

function removeOption(index: number) {
  if (form.options.length <= 2) {
    ElMessage.warning('选择题至少保留 2 个选项')
    return
  }
  const removed = form.options[index].key
  form.options.splice(index, 1)
  // 重排 key
  form.options.forEach((o, i) => (o.key = OPTION_KEYS[i]))
  form.multiAnswers = form.multiAnswers.filter(k => k !== removed && form.options.some(o => o.key === k))
  if (form.singleAnswer === removed) form.singleAnswer = ''
}

function addFill() {
  form.fillAnswers.push('')
}

function removeFill(index: number) {
  if (form.fillAnswers.length <= 1) {
    ElMessage.warning('填空题至少保留 1 个答案')
    return
  }
  form.fillAnswers.splice(index, 1)
}

function buildAnswer(): string {
  switch (form.type) {
    case 'SINGLE':
      return form.singleAnswer
    case 'MULTI':
      return [...form.multiAnswers].sort().join('')
    case 'JUDGE':
      return form.judgeAnswer
    case 'FILL':
      return form.fillAnswers.map(a => a.trim()).filter(Boolean).join('||')
    case 'SHORT_ANSWER':
    case 'ESSAY':
      // 主观题：answer 承载参考答案（可空）
      return form.referenceAnswer.trim()
  }
}

function validate(): string | null {
  if (!form.courseId) return '请选择所属课程'
  if (!form.content.trim()) return '请输入题干'
  if (form.score <= 0 || form.score > 100) return '分值需在 0~100 之间'
  if (form.type === 'SINGLE' || form.type === 'MULTI') {
    if (form.options.length < 2) return '选择题至少 2 个选项'
    if (form.options.some(o => !o.text.trim())) return '请填写所有选项内容'
    if (form.type === 'SINGLE' && !form.singleAnswer) return '请选择正确答案'
    if (form.type === 'MULTI' && form.multiAnswers.length < 2) return '多选题至少选择 2 个正确答案'
  }
  if (form.type === 'JUDGE' && !form.judgeAnswer) return '请选择正确或错误'
  if (form.type === 'FILL' && !buildAnswer()) return '请填写至少一个可接受答案'
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
      courseId: form.courseId!,
      type: form.type,
      content: form.content.trim(),
      options: isChoice.value ? form.options.map(o => ({ key: o.key, text: o.text.trim() })) : [],
      answer: buildAnswer(),
      score: Number(form.score),
      difficulty: Number(form.difficulty),
      analysis: form.analysis.trim() || undefined
    }
    if (editingId.value) {
      await questionApi.update(editingId.value, payload)
      ElMessage.success('题目更新成功')
    } else {
      await questionApi.create(payload)
      ElMessage.success('题目添加成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(q: Question) {
  try {
    await ElMessageBox.confirm(`确定删除题目「${q.content.slice(0, 20)}...」吗？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await questionApi.remove(q.id!)
    ElMessage.success('题目已删除')
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel' && e?.response) {
      ElMessage.error(e.response?.data?.message ?? '删除失败')
    }
  }
}

function answerDisplay(q: Question): string {
  if (isSubjective(q.type)) return q.answer ? `参考答案：${q.answer}` : '未提供参考答案'
  if (q.type === 'JUDGE') return q.answer === 'T' ? '正确' : '错误'
  if (q.type === 'FILL') return q.answer.split('||').join(' 或 ')
  return q.answer
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
        <el-form-item label="题型">
          <el-select v-model="query.type" placeholder="全部题型" clearable style="width: 130px">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="填空题" value="FILL" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="query.difficulty" placeholder="全部难度" clearable style="width: 120px">
            <el-option label="易" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="题干关键词" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button type="success" @click="openCreate">新增题目</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 题目列表 -->
    <el-card v-loading="loading">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="课程" prop="courseName" width="120" />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ TYPE_LABELS[row.type as QuestionType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题干" prop="content" min-width="220" show-overflow-tooltip />
        <el-table-column label="难度" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="DIFFICUNT_TAG_TYPE[row.difficulty]">
              {{ DIFFICULTY_LABELS[row.difficulty] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分值" prop="score" width="70" />
        <el-table-column label="答案" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ answerDisplay(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑题目' : '新增题目'"
      width="640px"
      append-to-body
    >
      <el-form label-width="90px">
        <el-form-item label="所属课程" required>
          <el-select v-model="form.courseId" placeholder="选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" required>
          <el-radio-group v-model="form.type" @change="onTypeChange">
            <el-radio value="SINGLE">单选题</el-radio>
            <el-radio value="MULTI">多选题</el-radio>
            <el-radio value="JUDGE">判断题</el-radio>
            <el-radio value="FILL">填空题</el-radio>
            <el-radio value="SHORT_ANSWER">简答题</el-radio>
            <el-radio value="ESSAY">论述题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="form.content" type="textarea" :rows="2" placeholder="输入题干" />
        </el-form-item>

        <!-- 单选/多选：选项 + 正确答案 -->
        <template v-if="isChoice">
          <el-form-item label="选项与答案" required>
            <div class="options-area">
              <div v-for="(opt, idx) in form.options" :key="idx" class="option-row">
                <el-radio v-if="form.type === 'SINGLE'" v-model="form.singleAnswer" :value="opt.key">
                  {{ opt.key }}
                </el-radio>
                <el-checkbox v-else v-model="form.multiAnswers" :value="opt.key">{{ opt.key }}</el-checkbox>
                <el-input v-model="opt.text" :placeholder="'选项 ' + opt.key + ' 内容'" class="option-input" />
                <el-button link type="danger" @click="removeOption(idx)">删除</el-button>
              </div>
              <el-button size="small" @click="addOption">+ 添加选项</el-button>
              <span class="option-tip">
                {{ form.type === 'SINGLE' ? '点左侧单选标记正确答案' : '勾选多个正确答案（至少 2 个）' }}
              </span>
            </div>
          </el-form-item>
        </template>

        <!-- 判断：正确/错误 -->
        <el-form-item v-else-if="form.type === 'JUDGE'" label="正确答案" required>
          <el-radio-group v-model="form.judgeAnswer">
            <el-radio value="T">正确</el-radio>
            <el-radio value="F">错误</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 填空：多个可接受答案 -->
        <el-form-item v-else-if="form.type === 'FILL'" label="可接受答案" required>
          <div class="options-area">
            <div v-for="(_, idx) in form.fillAnswers" :key="idx" class="option-row">
              <el-input v-model="form.fillAnswers[idx]" :placeholder="'答案 ' + (idx + 1)" class="option-input" />
              <el-button link type="danger" @click="removeFill(idx)">删除</el-button>
            </div>
            <el-button size="small" @click="addFill">+ 添加等价答案</el-button>
            <span class="option-tip">任一答案匹配即得分（忽略大小写与首尾空格）</span>
          </div>
        </el-form-item>

        <!-- 主观题（简答/论述）：参考答案 -->
        <el-form-item v-else-if="isSubjectiveForm" label="参考答案">
          <el-input
            v-model="form.referenceAnswer"
            type="textarea"
            :rows="form.type === 'ESSAY' ? 5 : 3"
            placeholder="阅卷参考要点（选填）；学生交卷后由教师人工评分"
          />
        </el-form-item>

        <el-form-item label="分值">
          <el-input-number v-model="form.score" :min="1" :max="100" :step="1" />
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio :value="1">易</el-radio>
            <el-radio :value="2">中</el-radio>
            <el-radio :value="3">难</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="答案解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="学生考后回顾可见（选填）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
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

.options-area {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-input {
  flex: 1;
}

.option-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}
</style>
