<script setup lang="ts">
/**
 * 考后回顾页（SPEC-results）
 * 已交卷考试逐题回顾：题干/选项/我的答案/正确答案/对错/解析。
 * 闭卷考试的答案在交卷后于此页开放（答题阶段由 exam-session 脱敏）。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentResultApi, type ExamReview, type QuestionReview } from '@/api/result'
import type { QuestionType } from '@/api/question'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.examId)

const loading = ref(true)
const review = ref<ExamReview | null>(null)

const TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', FILL: '填空题',
  SHORT_ANSWER: '简答题', ESSAY: '论述题'
}

const SUBJECTIVE = new Set(['SHORT_ANSWER', 'ESSAY'])

const correctCount = computed(() =>
  review.value?.questions.filter(q => q.correctFlag === 1).length ?? 0
)

/** M7：存在待评阅主观题时，成绩为临时分 */
const pending = computed(() =>
  review.value?.questions.some(q => q.correctFlag === 3) ?? false
)

function isSubjective(type: string): boolean {
  return SUBJECTIVE.has(type)
}

/** 答案编码转展示文本：T/F → 正确/错误，选项 key → key. text */
function displayAnswer(code: string | null, q: QuestionReview): string {
  if (!code) return '未作答'
  if (q.type === 'JUDGE') {
    return code === 'T' ? '正确' : '错误'
  }
  if (q.type === 'SINGLE' || q.type === 'MULTI') {
    return (code.match(/[A-Za-z]/g) ?? []).map(k => {
      const opt = q.options.find(o => o.key === k)
      return opt ? `${k}. ${opt.text}` : k
    }).join('；')
  }
  return code
}

async function fetchReview() {
  loading.value = true
  try {
    review.value = await studentResultApi.review(examId)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '加载回顾失败')
    router.replace('/student/scores')
  } finally {
    loading.value = false
  }
}

onMounted(fetchReview)
</script>

<template>
  <div class="page-container" v-if="review">
    <el-card class="summary-card">
      <el-alert
        v-if="pending"
        type="warning"
        :closable="false"
        show-icon
        class="pending-alert"
        title="本场考试含待教师评阅的主观题，当前分数为客观题临时分，定稿前不判定及格。"
      />
      <div class="summary">
        <div class="score-box">
          <span class="score" :class="pending ? '' : (review.score >= 60 ? 'pass' : 'fail')">{{ review.score }}</span>
          <span class="full">/ {{ review.fullScore }} 分{{ pending ? '（临时分）' : '' }}</span>
        </div>
        <div class="meta">
          <h2>{{ review.title }}</h2>
          <p>{{ review.courseName }} · 提交于 {{ (review.submitTime ?? '').replace('T', ' ').slice(0, 16) }}</p>
          <p>答对 {{ correctCount }} / {{ review.questions.length }} 题</p>
        </div>
        <el-button @click="router.push('/student/scores')">返回成绩列表</el-button>
      </div>
    </el-card>

    <div v-for="(q, idx) in review.questions" :key="idx" class="question-card"
      :class="q.correctFlag === 3 ? 'pending' : (q.correctFlag === 1 ? 'right' : 'wrong')">
      <div class="question-head">
        <span class="q-no">{{ idx + 1 }}</span>
        <el-tag size="small">{{ TYPE_LABELS[q.type as QuestionType] ?? q.type }}</el-tag>
        <el-tag v-if="q.correctFlag === 3" size="small" type="warning" effect="dark">待评阅</el-tag>
        <el-tag v-else size="small" :type="q.correctFlag === 1 ? 'success' : 'danger'" effect="dark">
          {{ q.correctFlag === 1 ? '正确' : '错误' }}
        </el-tag>
        <span class="q-score">{{ q.correctFlag === 3 ? '待评' : q.score }} / {{ q.fullScore }} 分</span>
      </div>
      <div class="q-content">{{ q.content }}</div>

      <div v-if="q.options.length" class="options">
        <div v-for="opt in q.options" :key="opt.key" class="opt-item"
          :class="{
            chosen: q.studentAnswer?.includes(opt.key),
            'opt-correct': q.correctAnswer.includes(opt.key),
            'opt-wrong': q.studentAnswer?.includes(opt.key) && !q.correctAnswer.includes(opt.key)
          }">
          <strong>{{ opt.key }}.</strong> {{ opt.text }}
        </div>
      </div>

      <!-- 主观题：作答原文 + 参考答案 + 教师评语 -->
      <div v-if="isSubjective(q.type)" class="answers">
        <p class="essay-block">我的作答：<span :class="q.correctFlag === 3 ? '' : (q.correctFlag === 1 ? 'pass' : 'fail')">{{ q.studentAnswer?.trim() || '未作答' }}</span></p>
        <p v-if="q.referenceAnswer" class="essay-block">参考答案：<span class="pass">{{ q.referenceAnswer }}</span></p>
        <p v-if="q.teacherComment" class="analysis">教师评语：{{ q.teacherComment }}</p>
        <p v-else-if="q.correctFlag === 3" class="pending-tip">本题待教师评阅，得分将在阅卷完成后公布。</p>
      </div>

      <!-- 客观题 -->
      <div v-else class="answers">
        <p>我的答案：<span :class="q.correctFlag === 1 ? 'pass' : 'fail'">{{ displayAnswer(q.studentAnswer, q) }}</span></p>
        <p>正确答案：<span class="pass">{{ displayAnswer(q.correctAnswer, q) }}</span></p>
        <p v-if="q.analysis" class="analysis">解析：{{ q.analysis }}</p>
      </div>
    </div>
  </div>

  <div v-else class="loading-wrap">
    <p>加载中...</p>
  </div>
</template>

<style scoped>
.summary-card {
  margin-bottom: 16px;
}

.summary {
  display: flex;
  align-items: center;
  gap: 24px;
}

.score-box .score {
  font-size: 44px;
  font-weight: 800;
}

.score-box .full {
  color: rgba(255, 255, 255, 0.5);
  margin-left: 6px;
}

.meta {
  flex: 1;
}

.meta h2 {
  margin: 0 0 6px;
}

.meta p {
  margin: 2px 0;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.question-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-left: 4px solid #909399;
  border-radius: 8px;
  padding: 18px;
  margin-bottom: 14px;
  background: rgba(255, 255, 255, 0.02);
}

.question-card.right {
  border-left-color: #67c23a;
}

.question-card.wrong {
  border-left-color: #f56c6c;
}

.question-card.pending {
  border-left-color: #e6a23c;
}

.pending-alert {
  margin-bottom: 16px;
}

.essay-block span {
  display: inline-block;
  white-space: pre-wrap;
  word-break: break-word;
}

.pending-tip {
  color: #e6a23c !important;
  font-size: 12px;
}

.question-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.q-no {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: rgba(0, 255, 255, 0.12);
  color: #00ffff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
}

.q-score {
  margin-left: auto;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
}

.q-content {
  margin-bottom: 12px;
  line-height: 1.6;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.opt-item {
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid transparent;
}

.opt-item.chosen {
  background: rgba(255, 255, 255, 0.04);
}

.opt-item.opt-correct {
  border-color: rgba(103, 194, 58, 0.6);
  color: #67c23a;
}

.opt-item.opt-wrong {
  border-color: rgba(245, 108, 108, 0.6);
  color: #f56c6c;
}

.answers {
  font-size: 13px;
}

.answers p {
  margin: 4px 0;
  color: rgba(255, 255, 255, 0.7);
}

.answers .pass {
  color: #67c23a;
}

.answers .fail {
  color: #f56c6c;
}

.analysis {
  padding: 8px 12px;
  background: rgba(0, 255, 255, 0.05);
  border-radius: 6px;
  color: rgba(0, 255, 255, 0.8) !important;
}

.loading-wrap {
  text-align: center;
  padding: 60px;
  color: rgba(255, 255, 255, 0.6);
}
</style>
