<script setup lang="ts">
/**
 * 教师端考试统计页（SPEC-results）
 * 成绩列表（可按班级筛选）+ 统计指标（参考人数/均分/最高最低/及格率/分数段分布）。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { teacherStatsApi, type ExamStats, type TeacherRecord } from '@/api/result'
import { classApi, type ClassInfo } from '@/api'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.examId)

const classes = ref<ClassInfo[]>([])
const records = ref<TeacherRecord[]>([])
const stats = ref<ExamStats | null>(null)
const loading = ref(false)
const classId = ref<number | undefined>(undefined)
const examTitle = ref('')

const maxBucketCount = computed(() =>
  Math.max(1, ...(stats.value?.buckets.map(b => b.count) ?? [1]))
)

async function fetchClasses() {
  classes.value = await classApi.getAll().then(res => res.data)
}

async function fetchAll() {
  loading.value = true
  try {
    const [r, s] = await Promise.all([
      teacherStatsApi.records(examId, classId.value),
      teacherStatsApi.stats(examId, classId.value)
    ])
    records.value = r
    stats.value = s
    examTitle.value = route.query.title as string || '考试统计'
  } catch {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchClasses()
  fetchAll()
})
</script>

<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <h2>{{ examTitle }}</h2>
        <div class="filters">
          <el-select v-model="classId" placeholder="全部班级" clearable style="width: 160px" @change="fetchAll">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id!" />
          </el-select>
          <el-button @click="router.push('/exams')">返回考试列表</el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计指标 -->
    <el-row :gutter="16" class="stat-row" v-if="stats">
      <el-col :span="4"><el-card shadow="never"><div class="stat"><span class="num">{{ stats.attended }}</span><span class="label">参考人数</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="never"><div class="stat"><span class="num">{{ stats.avgScore }}</span><span class="label">平均分</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="never"><div class="stat"><span class="num high">{{ stats.maxScore }}</span><span class="label">最高分</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="never"><div class="stat"><span class="num low">{{ stats.minScore }}</span><span class="label">最低分</span></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><div class="stat"><span class="num">{{ stats.passRate }}%</span><span class="label">及格率（≥60 分）</span></div></el-card></el-col>
    </el-row>

    <!-- 分数段分布 -->
    <el-card class="bucket-card" v-if="stats">
      <template #header><span>分数段分布</span></template>
      <div v-for="b in stats.buckets" :key="b.label" class="bucket">
        <span class="bucket-label">{{ b.label }}</span>
        <el-progress :percentage="Math.round((b.count / maxBucketCount) * 100)" :show-text="false"
          :stroke-width="14" class="bucket-bar" />
        <span class="bucket-count">{{ b.count }} 人</span>
      </div>
    </el-card>

    <!-- 成绩列表 -->
    <el-card v-loading="loading">
      <template #header><span>成绩列表</span></template>
      <el-table :data="records" stripe style="width: 100%">
        <el-table-column label="排名" type="index" width="70" />
        <el-table-column label="学号" prop="studentNumber" width="110" />
        <el-table-column label="姓名" prop="studentName" width="100" />
        <el-table-column label="班级" prop="className" width="120" />
        <el-table-column label="成绩" width="100">
          <template #default="{ row }">
            <span class="score" :class="row.totalScore >= 60 ? 'pass' : 'fail'">{{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交次数" prop="attemptCount" width="90" />
        <el-table-column label="违规次数" width="90">
          <template #default="{ row }">
            <span :class="{ 'violation': row.violationCount > 0 }">{{ row.violationCount }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无人参加考试" />
    </el-card>
  </div>
</template>

<style scoped>
.head-card {
  margin-bottom: 16px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.head h2 {
  margin: 0;
}

.filters {
  display: flex;
  gap: 10px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 0;
}

.stat .num {
  font-size: 28px;
  font-weight: 800;
  color: #00ffff;
}

.stat .num.high {
  color: #67c23a;
}

.stat .num.low {
  color: #f56c6c;
}

.stat .label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  margin-top: 4px;
}

.bucket-card {
  margin-bottom: 16px;
}

.bucket {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
}

.bucket-label {
  width: 80px;
  font-family: 'Consolas', monospace;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.bucket-bar {
  flex: 1;
}

.bucket-count {
  width: 50px;
  text-align: right;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.score.pass {
  color: #67c23a;
  font-weight: 700;
}

.score.fail {
  color: #f56c6c;
  font-weight: 700;
}

.violation {
  color: #e6a23c;
  font-weight: 700;
}
</style>
