<script setup lang="ts">
/**
 * 学生答题页（SPEC-exam-session）
 * 倒计时（服务端 deadline）、答案自动暂存、违规事件上报、按题型渲染、交卷判分。
 * 开卷考试显示答案与解析，闭卷考试答题时不显示。
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { studentExamApi, type StartExamResult, type SubmitResult } from '@/api/exam'
import type { QuestionType } from '@/api/question'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.examId)

const loading = ref(true)
const examData = ref<StartExamResult | null>(null)
const answers = reactive<Record<number, string>>({})
const remaining = ref(0)
const submitted = ref(false)
const submitResult = ref<SubmitResult | null>(null)
const saving = ref(false)

const deadlineTs = ref(0)
const serverOffset = ref(0) // 服务端时钟 - 本地时钟（毫秒）
let timer: number | null = null
let saveTimer: number | null = null

const TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', FILL: '填空题',
  SHORT_ANSWER: '简答题', ESSAY: '论述题'
}

/** 剩余秒数（基于服务端 deadline + 时钟差，不信任纯本地计时） */
function updateRemaining() {
  const now = Date.now() + serverOffset.value
  remaining.value = Math.max(0, Math.floor((deadlineTs.value - now) / 1000))
}

const timeDisplay = computed(() => {
  const s = remaining.value
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

const totalScore = computed(() => {
  const qs = examData.value?.questions
  if (!qs) return 0
  return Math.round(qs.reduce((s, q) => s + q.score, 0) * 10) / 10
})

const answeredCount = computed(() => {
  const qs = examData.value?.questions
  if (!qs) return 0
  return qs.filter(q => answers[q.paperQuestionId]?.trim()).length
})

const questions = computed(() => examData.value?.questions ?? [])

async function loadExam() {
  loading.value = true
  try {
    const data = await studentExamApi.start(examId)
    examData.value = data
    // 解析已有暂存答案
    if (data.existingAnswers) {
      try {
        const existing = JSON.parse(data.existingAnswers)
        Object.assign(answers, existing)
      } catch { /* ignore */ }
    }
    // 计算时钟差：服务端 deadline - remaining*1000 = 服务端当前时刻
    const serverNow = new Date(data.deadline).getTime() - data.remainingSeconds * 1000
    serverOffset.value = serverNow - Date.now()
    deadlineTs.value = new Date(data.deadline).getTime()
    updateRemaining()
    startTimer()
    attachViolationListeners()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '进入考试失败')
    router.replace('/student/exams')
  } finally {
    loading.value = false
  }
}

function startTimer() {
  timer = window.setInterval(() => {
    updateRemaining()
    if (remaining.value <= 0 && !submitted.value) {
      stopTimer()
      autoSubmit()
    }
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

/** 答案变更触发暂存（防抖 5 秒） */
function scheduleSave() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = window.setTimeout(saveAnswers, 5000)
}

async function saveAnswers() {
  if (submitted.value) return
  try {
    await studentExamApi.saveAnswers(examId, { ...answers })
  } catch (e: any) {
    if (e?.response?.data?.message?.includes('自动交卷')) {
      ElMessage.info('考试已超时，已自动交卷')
      await forceSubmitAfterTimeout()
    }
  }
}

async function forceSubmitAfterTimeout() {
  if (submitted.value) return
  try {
    const result = await studentExamApi.submit(examId)
    submitted.value = true
    submitResult.value = result
  } catch { /* ignore */ }
}

async function autoSubmit() {
  if (submitted.value) return
  try {
    await saveAnswers()
    const result = await studentExamApi.submit(examId)
    submitted.value = true
    submitResult.value = result
    ElMessage.success('时间到，已自动交卷')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '交卷失败')
  }
}

async function manualSubmit() {
  try {
    await ElMessageBox.confirm(`已答 ${answeredCount.value}/${examData.value?.questions.length ?? 0} 题，确定交卷吗？`, '交卷确认', {
      confirmButtonText: '交卷', cancelButtonText: '继续答题', type: 'warning'
    })
    saving.value = true
    if (saveTimer) clearTimeout(saveTimer)
    await saveAnswers()
    const result = await studentExamApi.submit(examId)
    submitted.value = true
    submitResult.value = result
  } catch (e: any) {
    if (e !== 'cancel' && e?.response) {
      ElMessage.error(e.response?.data?.message ?? '交卷失败')
    }
  } finally {
    saving.value = false
  }
}

// ---------- 违规监听 ----------

function attachViolationListeners() {
  document.addEventListener('visibilitychange', onVisibility)
  window.addEventListener('blur', onBlur)
  document.addEventListener('copy', onCopy)
  document.addEventListener('paste', onPaste)
  document.addEventListener('fullscreenchange', onFullscreen)
}

function detachViolationListeners() {
  document.removeEventListener('visibilitychange', onVisibility)
  window.removeEventListener('blur', onBlur)
  document.removeEventListener('copy', onCopy)
  document.removeEventListener('paste', onPaste)
  document.removeEventListener('fullscreenchange', onFullscreen)
}

function onVisibility() {
  if (document.hidden) reportViolation('VISIBILITY')
}
function onBlur() { reportViolation('BLUR') }
function onCopy() { reportViolation('COPY') }
function onPaste() { reportViolation('PASTE') }
function onFullscreen() {
  if (!document.fullscreenElement) reportViolation('FULLSCREEN')
}

function reportViolation(type: string) {
  if (submitted.value) return
  studentExamApi.reportViolation(examId, type).catch(() => { /* ignore */ })
}

// ---------- 答案处理 ----------

function setSingle(id: number, val: string) {
  answers[id] = val
  scheduleSave()
}

function setMulti(id: number, vals: string[]) {
  answers[id] = [...vals].sort().join('')
  scheduleSave()
}

function setJudge(id: number, val: string) {
  answers[id] = val
  scheduleSave()
}

function setFill(id: number, val: string) {
  answers[id] = val
  scheduleSave()
}

function getMulti(id: number): string[] {
  return (answers[id] ?? '').split('').filter(Boolean)
}

onMounted(loadExam)

onBeforeUnmount(() => {
  stopTimer()
  if (saveTimer) clearTimeout(saveTimer)
  detachViolationListeners()
  // 离开页面保存一次
  if (!submitted.value && examData.value) {
    saveAnswers()
  }
})
</script>

<template>
  <div v-if="loading" class="loading-wrap">
    <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    <p>正在进入考试...</p>
  </div>

  <div class="exam-room" v-else-if="examData">
    <!-- 交卷结果 -->
    <el-card v-if="submitted" class="result-card">
      <h2>交卷成功</h2>
      <div class="score-box">
        <span class="score-num">{{ submitResult?.score ?? 0 }}</span>
        <span class="score-total"> / {{ submitResult?.totalScore ?? totalScore }} 分</span>
      </div>
      <div class="result-actions">
        <el-button type="primary" @click="router.push(`/student/scores/${examId}/review`)">查看回顾</el-button>
        <el-button @click="router.push('/student/exams')">返回考试列表</el-button>
      </div>
    </el-card>

    <template v-else>
      <!-- 顶部：标题、倒计时、交卷 -->
      <header class="exam-header">
        <div class="exam-title">
          <el-tag :type="examData?.openBook ? 'warning' : 'success'" effect="dark">
            {{ examData?.openBook ? '开卷练习' : '闭卷考试' }}
          </el-tag>
          <span class="title-text">{{ examData?.title }}</span>
        </div>
        <div class="header-right">
          <span class="answered-info">已答 {{ answeredCount }} / {{ examData?.questions.length ?? 0 }}</span>
          <div class="countdown" :class="{ urgent: remaining < 60 }">
            <el-icon><Timer /></el-icon>
            <span>{{ timeDisplay }}</span>
          </div>
          <el-button type="primary" :loading="saving" @click="manualSubmit">交卷</el-button>
        </div>
      </header>

      <!-- 题目区 -->
      <main class="exam-body">
        <div v-for="(q, idx) in questions" :key="q.paperQuestionId" class="question-card">
          <div class="question-head">
            <span class="q-no">{{ idx + 1 }}</span>
            <el-tag size="small">{{ TYPE_LABELS[q.type as QuestionType] }}</el-tag>
            <span class="q-score">{{ q.score }} 分</span>
          </div>
          <div class="q-content">{{ q.content }}</div>

          <!-- 单选 -->
          <div v-if="q.type === 'SINGLE'" class="options">
            <el-radio-group v-model="answers[q.paperQuestionId]" @change="setSingle(q.paperQuestionId, answers[q.paperQuestionId])">
              <el-radio v-for="opt in q.options" :key="opt.key" :value="opt.key" class="opt-item">
                <strong>{{ opt.key }}.</strong> {{ opt.text }}
              </el-radio>
            </el-radio-group>
          </div>

          <!-- 多选 -->
          <div v-else-if="q.type === 'MULTI'" class="options">
            <el-checkbox-group :model-value="getMulti(q.paperQuestionId)"
              @change="(v: string[]) => setMulti(q.paperQuestionId, v)">
              <el-checkbox v-for="opt in q.options" :key="opt.key" :value="opt.key" class="opt-item">
                <strong>{{ opt.key }}.</strong> {{ opt.text }}
              </el-checkbox>
            </el-checkbox-group>
          </div>

          <!-- 判断 -->
          <div v-else-if="q.type === 'JUDGE'" class="options">
            <el-radio-group v-model="answers[q.paperQuestionId]" @change="setJudge(q.paperQuestionId, answers[q.paperQuestionId])">
              <el-radio value="T">正确</el-radio>
              <el-radio value="F">错误</el-radio>
            </el-radio-group>
          </div>

          <!-- 填空 -->
          <div v-else-if="q.type === 'FILL'" class="options">
            <el-input v-model="answers[q.paperQuestionId]" placeholder="输入答案" @input="setFill(q.paperQuestionId, ($event.target as HTMLInputElement).value)" />
          </div>

          <!-- 主观题（简答/论述）：文本作答 -->
          <div v-else-if="q.type === 'SHORT_ANSWER' || q.type === 'ESSAY'" class="options">
            <el-input
              v-model="answers[q.paperQuestionId]"
              type="textarea"
              :rows="q.type === 'ESSAY' ? 6 : 3"
              :placeholder="q.type === 'ESSAY' ? '请展开论述（交卷后由教师人工评分）' : '请简要作答（交卷后由教师人工评分）'"
              @input="setFill(q.paperQuestionId, answers[q.paperQuestionId])"
            />
          </div>

          <!-- 开卷：显示答案与解析（主观题为参考答案） -->
          <div v-if="examData?.openBook && q.answer" class="open-book-answer">
            <el-divider />
            <p><strong>参考答案：</strong>{{ q.answer }}</p>
            <p v-if="q.analysis"><strong>解析：</strong>{{ q.analysis }}</p>
          </div>
        </div>
      </main>
    </template>
  </div>
</template>

<style scoped>
.exam-room {
  min-height: 100vh;
  background: #0a0e17;
  color: #e6edf3;
}

.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  gap: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.exam-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: linear-gradient(180deg, #0d1117, #0a0e17);
  border-bottom: 1px solid rgba(0, 255, 255, 0.15);
  backdrop-filter: blur(10px);
}

.exam-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-text {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.answered-info {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'Consolas', monospace;
  font-size: 20px;
  font-weight: 700;
  color: #00ffff;
  padding: 6px 14px;
  border: 1px solid rgba(0, 255, 255, 0.3);
  border-radius: 6px;
  background: rgba(0, 255, 255, 0.05);
}

.countdown.urgent {
  color: #ff6b6b;
  border-color: rgba(255, 107, 107, 0.5);
  animation: blink 1s infinite;
}

@keyframes blink {
  50% { opacity: 0.5; }
}

.exam-body {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.question-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(0, 255, 255, 0.1);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}

.question-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.q-no {
  width: 26px;
  height: 26px;
  line-height: 26px;
  text-align: center;
  background: rgba(0, 255, 255, 0.12);
  color: #00ffff;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
}

.q-score {
  margin-left: auto;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.q-content {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 14px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.opt-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.open-book-answer {
  margin-top: 14px;
  padding-top: 10px;
  font-size: 13px;
  color: rgba(0, 255, 255, 0.8);
}

.result-card {
  max-width: 480px;
  margin: 60px auto;
  text-align: center;
}

.result-card h2 {
  margin-bottom: 20px;
}

.grading-alert {
  margin: 0 0 12px;
  text-align: left;
}

.score-box {
  margin: 24px 0;
}

.score-num {
  font-size: 56px;
  font-weight: 800;
  color: #00ffff;
}

.score-total {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.5);
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
