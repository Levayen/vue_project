<script setup lang="ts">
/**
 * 教师人工阅卷工作台（M7：主观题简答/论述）
 * 三栏布局：左=待阅考试，中=待评学生答卷，右=逐题评分（得分+评语）；
 * 一份答卷全部评完后后端自动定稿并重算成绩。
 */
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  teacherReviewApi,
  type PendingExam,
  type PendingAttempt,
  type ReviewAttempt,
  type SubjectiveAnswer
} from '@/api/review'

const route = useRoute()
const router = useRouter()

const exams = ref<PendingExam[]>([])
const attempts = ref<PendingAttempt[]>([])
const detail = ref<ReviewAttempt | null>(null)
const examsLoading = ref(false)
const attemptsLoading = ref(false)
const detailLoading = ref(false)
const selectedExamId = ref<number | null>(null)
const selectedAttemptId = ref<number | null>(null)

/** 每题评分表单：answerId -> { score, comment, saving } */
const forms = reactive<Record<number, { score: number; comment: string; saving: boolean }>>({})

const TYPE_LABELS: Record<string, string> = {
  SHORT_ANSWER: '简答题',
  ESSAY: '论述题'
}

async function fetchExams(keepSelection = false) {
  examsLoading.value = true
  try {
    exams.value = await teacherReviewApi.pendingExams()
    if (!keepSelection) {
      const queryExam = route.query.examId ? Number(route.query.examId) : null
      const target = queryExam && exams.value.some(e => e.examId === queryExam)
        ? queryExam
        : exams.value[0]?.examId ?? null
      if (target != null) {
        selectExam(target)
      } else {
        selectedExamId.value = null
        attempts.value = []
        detail.value = null
      }
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '加载待阅考试失败')
  } finally {
    examsLoading.value = false
  }
}

async function selectExam(examId: number) {
  selectedExamId.value = examId
  selectedAttemptId.value = null
  detail.value = null
  attemptsLoading.value = true
  try {
    attempts.value = await teacherReviewApi.pendingAttempts(examId)
    if (attempts.value.length) {
      selectAttempt(attempts.value[0].attemptId)
    } else {
      attempts.value = []
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '加载待评学生失败')
  } finally {
    attemptsLoading.value = false
  }
}

async function selectAttempt(attemptId: number) {
  selectedAttemptId.value = attemptId
  detailLoading.value = true
  try {
    detail.value = await teacherReviewApi.reviewAttempt(attemptId)
    // 初始化评分表单（已评的带出原分/评语，便于教师改分）
    detail.value.answers.forEach(a => {
      forms[a.answerId] = {
        score: a.score ?? 0,
        comment: a.teacherComment ?? '',
        saving: false
      }
    })
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '加载答卷详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function submitScore(a: SubjectiveAnswer) {
  const form = forms[a.answerId]
  if (!form) return
  if (form.score == null || Number.isNaN(form.score)) {
    ElMessage.warning('请填写得分')
    return
  }
  if (form.score < 0 || form.score > a.fullScore) {
    ElMessage.warning(`得分必须在 0 ~ ${a.fullScore} 之间`)
    return
  }
  form.saving = true
  try {
    const res = await teacherReviewApi.scoreAnswer(a.answerId, {
      score: form.score,
      comment: form.comment?.trim() || null
    })
    if (res.finalized) {
      ElMessage.success('本题已评分，该答卷全部评完，成绩已定稿')
    } else {
      ElMessage.success(`本题已评分，剩余 ${res.remaining} 题待评`)
    }
    // 刷新详情与各列表（定稿后答卷会离开待评列表）
    await Promise.all([
      selectAttempt(selectedAttemptId.value!),
      refreshLists()
    ])
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '评分失败')
  } finally {
    form.saving = false
  }
}

/** 评分后刷新待评列表（保留当前考试选择） */
async function refreshLists() {
  await fetchExams(true)
  if (selectedExamId.value != null) {
    try {
      const list = await teacherReviewApi.pendingAttempts(selectedExamId.value)
      attempts.value = list
      // 当前答卷已定稿离开列表时，自动打开下一份
      if (selectedAttemptId.value != null && !list.some(a => a.attemptId === selectedAttemptId.value)) {
        if (list.length) {
          selectAttempt(list[0].attemptId)
        } else {
          detail.value = null
          selectedAttemptId.value = null
        }
      }
    } catch { /* ignore */ }
  }
}

function fmtTime(t: string | null): string {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(() => fetchExams())
</script>

<template>
  <div class="page-container review-center">
    <!-- 左栏：待阅考试 -->
    <el-card class="pane pane-exams" v-loading="examsLoading">
      <template #header>
        <div class="pane-header">
          <span>待阅考试</span>
          <el-button link type="primary" :icon="'Refresh'" @click="fetchExams(true)">刷新</el-button>
        </div>
      </template>
      <div
        v-for="e in exams"
        :key="e.examId"
        class="exam-item"
        :class="{ active: e.examId === selectedExamId }"
        @click="selectExam(e.examId)"
      >
        <div class="item-title">{{ e.examTitle }}</div>
        <div class="item-sub">{{ e.courseName }}</div>
        <div class="item-meta">
          <el-tag size="small" type="warning">{{ e.pendingAttempts }} 份待评</el-tag>
          <el-tag size="small" type="info">{{ e.pendingQuestions }} 道题</el-tag>
        </div>
      </div>
      <el-empty v-if="!examsLoading && !exams.length" description="暂无待阅考试" :image-size="70" />
    </el-card>

    <!-- 中栏：待评学生 -->
    <el-card class="pane pane-attempts" v-loading="attemptsLoading">
      <template #header><span>待评学生</span></template>
      <div
        v-for="a in attempts"
        :key="a.attemptId"
        class="attempt-item"
        :class="{ active: a.attemptId === selectedAttemptId }"
        @click="selectAttempt(a.attemptId)"
      >
        <div class="item-title">{{ a.studentName }} <span class="stu-no">({{ a.studentNumber }})</span></div>
        <div class="item-sub">{{ a.className }}</div>
        <div class="item-meta">
          <el-tag size="small" type="warning">{{ a.pendingQuestions }} 题待评</el-tag>
          <span class="temp-score">临时 {{ a.temporaryScore }} 分</span>
        </div>
        <div class="item-time">交卷：{{ fmtTime(a.submitTime) }}</div>
      </div>
      <el-empty v-if="!attemptsLoading && !attempts.length" description="该考试暂无待评答卷" :image-size="70" />
    </el-card>

    <!-- 右栏：答卷评分 -->
    <el-card class="pane pane-detail" v-loading="detailLoading">
      <template #header>
        <div class="pane-header">
          <span v-if="detail">
            {{ detail.studentName }}（{{ detail.studentNumber }}）· {{ detail.className }}
          </span>
          <span v-else>答卷评分</span>
          <el-button link type="primary" @click="router.push('/exams')">返回考试管理</el-button>
        </div>
      </template>

      <template v-if="detail">
        <el-alert
          :type="detail.finalized ? 'success' : 'warning'"
          :closable="false"
          show-icon
          class="detail-alert"
          :title="detail.finalized
            ? `该答卷已全部评完并定稿，最终成绩 ${detail.currentScore} / ${detail.paperTotalScore} 分。`
            : `阅卷中：当前 ${detail.currentScore} / ${detail.paperTotalScore} 分（含客观题与已评主观题），评完最后一题自动定稿。`"
        />

        <div v-for="(a, idx) in detail.answers" :key="a.answerId" class="answer-card"
          :class="{ scored: a.correctFlag !== 3 }">
          <div class="answer-head">
            <span class="q-no">{{ idx + 1 }}</span>
            <el-tag size="small">{{ TYPE_LABELS[a.type] ?? a.type }}</el-tag>
            <el-tag v-if="a.correctFlag === 3" size="small" type="warning" effect="dark">待评阅</el-tag>
            <el-tag v-else size="small" :type="a.correctFlag === 1 ? 'success' : 'danger'" effect="dark">
              {{ a.correctFlag === 1 ? '满分' : '未满分' }}
            </el-tag>
            <span class="q-full">满分 {{ a.fullScore }} 分</span>
          </div>

          <div class="q-content">{{ a.content }}</div>

          <div v-if="a.referenceAnswer" class="ref-answer">
            <strong>参考答案：</strong>{{ a.referenceAnswer }}
          </div>

          <div class="stu-answer">
            <strong>学生作答：</strong>
            <span :class="{ empty: !a.answerText?.trim() }">{{ a.answerText?.trim() || '（未作答）' }}</span>
          </div>

          <div class="score-form">
            <div class="score-row">
              <span class="form-label">得分：</span>
              <el-input-number
                v-model="forms[a.answerId].score"
                :min="0"
                :max="a.fullScore"
                :precision="1"
                :step="1"
                size="small"
              />
              <span class="form-hint">/ {{ a.fullScore }} 分（满分记正确，否则入错题本）</span>
            </div>
            <div class="comment-row">
              <span class="form-label">评语：</span>
              <el-input
                v-model="forms[a.answerId].comment"
                type="textarea"
                :rows="2"
                size="small"
                placeholder="可选：填写阅卷评语"
              />
            </div>
            <div class="submit-row">
              <el-button
                type="primary"
                size="small"
                :loading="forms[a.answerId]?.saving"
                @click="submitScore(a)"
              >
                {{ a.correctFlag === 3 ? '提交评分' : '修改评分' }}
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <el-empty v-else description="请选择左侧待评答卷" />
    </el-card>
  </div>
</template>

<style scoped>
.review-center {
  display: flex;
  gap: 12px;
  align-items: stretch;
  padding: 16px;
  height: calc(100vh - 40px);
  box-sizing: border-box;
}

.pane {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}

.pane-exams {
  flex: 0 0 260px;
}

.pane-attempts {
  flex: 0 0 280px;
}

.pane-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.exam-item,
.attempt-item {
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.exam-item:hover,
.attempt-item:hover {
  border-color: rgba(0, 255, 255, 0.4);
  background: rgba(0, 255, 255, 0.04);
}

.exam-item.active,
.attempt-item.active {
  border-color: #00ffff;
  background: rgba(0, 255, 255, 0.08);
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.stu-no {
  font-weight: 400;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.item-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 6px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.temp-score {
  font-size: 12px;
  color: #e6a23c;
}

.item-time {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.detail-alert {
  margin-bottom: 14px;
}

.answer-card {
  border: 1px solid rgba(230, 162, 60, 0.35);
  border-left: 4px solid #e6a23c;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 14px;
  background: rgba(255, 255, 255, 0.02);
}

.answer-card.scored {
  border-color: rgba(103, 194, 58, 0.35);
  border-left-color: #67c23a;
}

.answer-head {
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

.q-full {
  margin-left: auto;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
}

.q-content {
  line-height: 1.6;
  margin-bottom: 10px;
}

.ref-answer {
  padding: 8px 12px;
  background: rgba(103, 194, 58, 0.06);
  border-radius: 6px;
  font-size: 13px;
  color: rgba(103, 194, 58, 0.9);
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.stu-answer {
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 12px;
}

.stu-answer .empty {
  color: rgba(255, 255, 255, 0.35);
}

.score-form {
  border-top: 1px dashed rgba(255, 255, 255, 0.1);
  padding-top: 12px;
}

.score-row,
.comment-row,
.submit-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.form-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 28px;
  flex-shrink: 0;
}

.comment-row .form-label {
  line-height: 1.5;
  padding-top: 4px;
}

.form-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  line-height: 28px;
}

.comment-row .el-input {
  flex: 1;
}
</style>
