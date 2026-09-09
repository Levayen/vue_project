<script setup lang="ts">
/**
 * 学生错题本（SPEC-results）
 * 跨考试聚合答错/漏选/错选题目（is_correct=0），含我的答案/正确答案/解析，可按课程筛选。
 */
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { studentResultApi, type WrongQuestion } from '@/api/result'
import { courseApi, type Course } from '@/api'
import type { QuestionType } from '@/api/question'

const courses = ref<Course[]>([])
const list = ref<WrongQuestion[]>([])
const loading = ref(false)
const courseId = ref<number | undefined>(undefined)

const TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', FILL: '填空题',
  SHORT_ANSWER: '简答题', ESSAY: '论述题'
}

const SUBJECTIVE = new Set(['SHORT_ANSWER', 'ESSAY'])

function isSubjective(type: string): boolean {
  return SUBJECTIVE.has(type)
}

async function fetchCourses() {
  courses.value = await courseApi.getAll().then(res => res.data)
}

async function fetchList() {
  loading.value = true
  try {
    list.value = await studentResultApi.wrongBook(courseId.value)
  } catch {
    ElMessage.error('加载错题本失败')
  } finally {
    loading.value = false
  }
}

function displayAnswer(q: WrongQuestion, code: string | null): string {
  if (!code) return '未作答'
  if (q.options.length > 0) {
    return (code.match(/[A-Za-z]/g) ?? []).join('、')
  }
  return code
}

onMounted(() => {
  fetchCourses()
  fetchList()
})
</script>

<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>错题本（{{ list.length }} 题）</span>
          <el-select v-model="courseId" placeholder="全部课程" clearable style="width: 180px" @change="fetchList">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id!" />
          </el-select>
        </div>
      </template>

      <div v-for="(q, idx) in list" :key="idx" class="wrong-card">
        <div class="question-head">
          <el-tag size="small" type="danger">错题</el-tag>
          <el-tag size="small">{{ TYPE_LABELS[q.type as QuestionType] ?? q.type }}</el-tag>
          <span class="exam-ref">{{ q.courseName }} · {{ q.examTitle }}</span>
          <span class="q-score">{{ q.score }} / {{ q.fullScore }} 分</span>
        </div>
        <div class="q-content">{{ q.content }}</div>

        <div v-if="q.options.length" class="options">
          <div v-for="opt in q.options" :key="opt.key" class="opt-item"
            :class="{
              'opt-correct': q.correctAnswer.includes(opt.key),
              'opt-wrong': q.studentAnswer?.includes(opt.key) && !q.correctAnswer.includes(opt.key)
            }">
            <strong>{{ opt.key }}.</strong> {{ opt.text }}
          </div>
        </div>

        <!-- 主观题：作答原文 + 参考答案 -->
        <div v-if="isSubjective(q.type)" class="answers">
          <p class="essay-block">我的作答：<span class="fail">{{ q.studentAnswer?.trim() || '未作答' }}</span></p>
          <p v-if="q.referenceAnswer" class="essay-block">参考答案：<span class="pass">{{ q.referenceAnswer }}</span></p>
          <p v-if="q.analysis" class="analysis">解析：{{ q.analysis }}</p>
        </div>

        <!-- 客观题 -->
        <div v-else class="answers">
          <p>我的答案：<span class="fail">{{ displayAnswer(q, q.studentAnswer) }}</span></p>
          <p>正确答案：<span class="pass">{{ displayAnswer(q, q.correctAnswer) }}</span></p>
          <p v-if="q.analysis" class="analysis">解析：{{ q.analysis }}</p>
        </div>
      </div>

      <el-empty v-if="!loading && !list.length" description="太棒了，暂无错题！" />
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wrong-card {
  border: 1px solid rgba(245, 108, 108, 0.3);
  border-left: 4px solid #f56c6c;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 14px;
  background: rgba(255, 255, 255, 0.02);
}

.question-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.exam-ref {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.q-score {
  margin-left: auto;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
}

.q-content {
  margin-bottom: 10px;
  line-height: 1.6;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.opt-item {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid transparent;
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

.essay-block span {
  display: inline-block;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
