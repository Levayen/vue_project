<script setup lang="ts">
/**
 * 学生成绩列表页（SPEC-results）
 * 展示所有考试的有效成绩（按成绩规则聚合）、次数、及格状态；可跳转考后回顾。
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentResultApi, type StudentResult } from '@/api/result'

const router = useRouter()
const list = ref<StudentResult[]>([])
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    list.value = await studentResultApi.myResults()
  } catch {
    ElMessage.error('加载成绩失败')
  } finally {
    loading.value = false
  }
}

function fmtTime(t: string | null) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的成绩</span>
          <el-button link type="primary" @click="router.push('/student/wrong-book')">错题本</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" stripe style="width: 100%">
        <el-table-column label="考试名称" prop="examTitle" min-width="180" show-overflow-tooltip />
        <el-table-column label="课程" prop="courseName" width="130" />
        <el-table-column label="成绩" width="110">
          <template #default="{ row }">
            <template v-if="row.pending">
              <span class="score pending-score">{{ row.totalScore }}</span>
              <el-tag size="small" type="warning" effect="plain">临时分</el-tag>
            </template>
            <span v-else class="score" :class="row.pass ? 'pass' : 'fail'">{{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.pending" size="small" type="warning">阅卷中</el-tag>
            <el-tag v-else size="small" :type="row.pass ? 'success' : 'danger'">{{ row.pass ? '及格' : '不及格' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交次数" prop="attemptCount" width="90" />
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/student/scores/${row.examId}/review`)">查看回顾</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无考试成绩" />
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.score.pass {
  color: #67c23a;
  font-weight: 700;
}

.score.fail {
  color: #f56c6c;
  font-weight: 700;
}
</style>
