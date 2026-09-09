<script setup lang="ts">
/**
 * 学生考试列表页（SPEC-exam-session）
 * 显示已选课课程下的考试，含状态（未开始/可考/已截止/次数用尽）与已考成绩。
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentExamApi, type StudentExamItem } from '@/api/exam'

const router = useRouter()

const list = ref<StudentExamItem[]>([])
const loading = ref(false)

const STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: '未开始',
  AVAILABLE: '可参加',
  ENDED: '已截止',
  ATTEMPTS_USED: '次数用尽'
}
const STATUS_TYPES: Record<string, string> = {
  NOT_STARTED: 'info',
  AVAILABLE: 'success',
  ENDED: 'danger',
  ATTEMPTS_USED: 'warning'
}

async function fetchList() {
  loading.value = true
  try {
    list.value = await studentExamApi.myExams()
  } catch {
    ElMessage.error('加载考试列表失败')
  } finally {
    loading.value = false
  }
}

function enterExam(item: StudentExamItem) {
  if (item.status !== 'AVAILABLE') {
    ElMessage.warning(STATUS_LABELS[item.status] + '，无法进入')
    return
  }
  router.push(`/student/exam/${item.examId}`)
}

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <el-card>
      <template #header><span>我的考试</span></template>
      <el-table v-loading="loading" :data="list" stripe style="width: 100%">
        <el-table-column label="考试名称" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="课程" prop="courseName" width="120" />
        <el-table-column label="模式" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.openBook ? 'warning' : 'success'">
              {{ row.openBook ? '开卷' : '闭卷' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始" width="160">
          <template #default="{ row }">{{ row.startTime.replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="结束" width="160">
          <template #default="{ row }">{{ row.endTime.replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="时长(分)" prop="durationMinutes" width="80" />
        <el-table-column label="进度" width="120">
          <template #default="{ row }">{{ row.submittedAttempts }} / {{ row.maxAttempts }}</template>
        </el-table-column>
        <el-table-column label="成绩" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.grading" size="small" type="warning">待阅卷</el-tag>
            <template v-else>{{ row.score ?? '-' }}</template>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="STATUS_TYPES[row.status]">{{ STATUS_LABELS[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status !== 'AVAILABLE'" @click="enterExam(row)">
              {{ row.status === 'AVAILABLE' ? '进入考试' : STATUS_LABELS[row.status] }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无可用考试" />
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
