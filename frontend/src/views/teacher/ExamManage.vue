<script setup lang="ts">
/**
 * 考试管理页（SPEC-exam-session，教师端）
 * 发布考试（选择已发布试卷 + 时间窗口/时长/次数/开卷闭卷）、考试列表、监考视图。
 */
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examApi, type Exam, type MonitoringResult, type ScoreRule } from '@/api/exam'
import { courseApi, type Course } from '@/api'
import { paperApi, type Paper } from '@/api/paper'

const courses = ref<Course[]>([])
const papers = ref<Paper[]>([])
const list = ref<Exam[]>([])
const loading = ref(false)
const router = useRouter()

const query = reactive({ courseId: undefined as number | undefined })

const publishVisible = ref(false)
const saving = ref(false)
const form = reactive({
  title: '',
  paperId: undefined as number | undefined,
  startTime: '',
  endTime: '',
  durationMinutes: 120,
  maxAttempts: 1,
  scoreRule: 'LAST' as ScoreRule,
  shuffle: true,
  openBook: false
})

const monitoringVisible = ref(false)
const monitoringLoading = ref(false)
const monitoring = ref<MonitoringResult | null>(null)
const monitoringExamId = ref<number | null>(null)
const forceSubmitting = ref(false)
let monitoringTimer: number | null = null

async function fetchCourses() {
  courses.value = await courseApi.getAll().then(res => res.data)
}

async function fetchPapers() {
  const res = await paperApi.page({ page: 0, size: 100 })
  // 仅已发布试卷可选
  papers.value = res.content.filter(p => p.status === 'PUBLISHED')
}

async function fetchList() {
  loading.value = true
  try {
    list.value = await examApi.list(query.courseId || undefined)
  } catch {
    ElMessage.error('加载考试列表失败')
  } finally {
    loading.value = false
  }
}

function openPublish() {
  Object.assign(form, {
    title: '', paperId: undefined, startTime: '', endTime: '',
    durationMinutes: 120, maxAttempts: 1, scoreRule: 'LAST' as ScoreRule,
    shuffle: true, openBook: false
  })
  publishVisible.value = true
}

function validate(): string | null {
  if (!form.title.trim()) return '请输入考试名称'
  if (!form.paperId) return '请选择试卷'
  if (!form.startTime || !form.endTime) return '请设置考试起止时间'
  if (new Date(form.endTime) <= new Date(form.startTime)) return '结束时间必须晚于开始时间'
  if (form.durationMinutes <= 0) return '考试时长必须大于 0'
  if (form.maxAttempts <= 0) return '考试次数必须大于 0'
  return null
}

async function handlePublish() {
  const error = validate()
  if (error) {
    ElMessage.warning(error)
    return
  }
  saving.value = true
  try {
    const selectedPaper = papers.value.find(p => p.id === form.paperId)
    await examApi.publish({
      title: form.title.trim(),
      paperId: form.paperId!,
      courseId: selectedPaper?.courseId as number,
      startTime: form.startTime,
      endTime: form.endTime,
      durationMinutes: form.durationMinutes,
      maxAttempts: form.maxAttempts,
      scoreRule: form.scoreRule,
      shuffle: form.shuffle,
      openBook: form.openBook
    })
    ElMessage.success('考试发布成功')
    publishVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '发布失败')
  } finally {
    saving.value = false
  }
}

async function openMonitoring(exam: Exam) {
  monitoringExamId.value = exam.id!
  monitoringVisible.value = true
  await loadMonitoring()
  startMonitoringTimer()
}

async function loadMonitoring() {
  if (monitoringExamId.value == null) return
  monitoringLoading.value = true
  try {
    monitoring.value = await examApi.monitoring(monitoringExamId.value)
  } catch {
    ElMessage.error('加载监考信息失败')
  } finally {
    monitoringLoading.value = false
  }
}

function startMonitoringTimer() {
  stopMonitoringTimer()
  monitoringTimer = window.setInterval(loadMonitoring, 10000)
}

function stopMonitoringTimer() {
  if (monitoringTimer) {
    clearInterval(monitoringTimer)
    monitoringTimer = null
  }
}

function formatRemaining(sec: number | null): string {
  if (sec == null) return '-'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const VIOLATION_LABELS: Record<string, string> = {
  VISIBILITY: '切屏/失焦',
  COPY: '复制',
  PASTE: '粘贴',
  FULLSCREEN: '全屏'
}

async function handleForceSubmit(studentId?: number, studentName?: string) {
  const confirmMsg = studentId
    ? `确定强制收卷「${studentName}」的考试吗？`
    : '确定强制收卷全体进行中考生吗？'
  try {
    await ElMessageBox.confirm(confirmMsg, '强制收卷', { type: 'warning' })
  } catch {
    return
  }
  forceSubmitting.value = true
  try {
    const res = await examApi.forceSubmit(monitoringExamId.value!, studentId)
    ElMessage.success(`已强制收卷 ${res.forcedCount} 人`)
    await loadMonitoring()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '强制收卷失败')
  } finally {
    forceSubmitting.value = false
  }
}

onMounted(() => {
  fetchCourses()
  fetchPapers()
  fetchList()
})

onBeforeUnmount(() => {
  stopMonitoringTimer()
})
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="课程">
          <el-select v-model="query.courseId" placeholder="全部课程" clearable style="width: 180px" @change="fetchList">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id!" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">刷新</el-button>
          <el-button type="success" @click="openPublish">发布考试</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="考试名称" prop="title" min-width="160" show-overflow-tooltip />
        <el-table-column label="课程" prop="courseName" width="120" />
        <el-table-column label="试卷" prop="paperName" width="120" show-overflow-tooltip />
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
        <el-table-column label="次数" prop="maxAttempts" width="60" />
        <el-table-column label="总分" prop="totalScore" width="70" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMonitoring(row)">监考</el-button>
            <el-button link type="primary" @click="router.push(`/exams/${row.id}/stats?title=${encodeURIComponent(row.title)}`)">统计</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发布考试弹窗 -->
    <el-dialog v-model="publishVisible" title="发布考试" width="640px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="考试名称" required>
          <el-input v-model="form.title" placeholder="如：数据结构期中考试" maxlength="128" />
        </el-form-item>
        <el-form-item label="试卷" required>
          <el-select v-model="form.paperId" placeholder="选择已发布试卷" style="width: 100%">
            <el-option v-for="p in papers" :key="p.id" :label="p.name + '（' + (p.totalScore ?? 0) + '分）'" :value="p.id!" />
          </el-select>
          <div v-if="!papers.length" class="form-tip">暂无已发布试卷，请先到试卷管理发布试卷</div>
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm"
            placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm"
            placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="考试时长">
          <el-input-number v-model="form.durationMinutes" :min="1" :max="600" :step="5" />
          <span class="form-tip">分钟；实际截止为开考时刻+时长与考试结束时间的较早者</span>
        </el-form-item>
        <el-form-item label="最大次数">
          <el-input-number v-model="form.maxAttempts" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="成绩规则">
          <el-radio-group v-model="form.scoreRule">
            <el-radio value="LAST">末次成绩</el-radio>
            <el-radio value="BEST">最好成绩</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="乱序">
          <el-switch v-model="form.shuffle" />
          <span class="form-tip">打乱题目顺序</span>
        </el-form-item>
        <el-form-item label="考试模式">
          <el-radio-group v-model="form.openBook">
            <el-radio :value="false">闭卷（正式，发卷脱敏）</el-radio>
            <el-radio :value="true">开卷（练习，下发答案）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>

    <!-- 监考弹窗 -->
    <el-dialog v-model="monitoringVisible" :title="`监考 - ${monitoring?.title ?? ''}`" width="900px" append-to-body
      @closed="stopMonitoringTimer">
      <!-- 汇总统计 -->
      <el-row :gutter="12" class="stat-row" v-if="monitoring">
        <el-col :span="4"><el-card shadow="never"><div class="stat"><span class="num">{{ monitoring.enrolledCount }}</span><span class="label">参考人数</span></div></el-card></el-col>
        <el-col :span="5"><el-card shadow="never"><div class="stat"><span class="num warning">{{ monitoring.inProgressCount }}</span><span class="label">考试中</span></div></el-card></el-col>
        <el-col :span="5"><el-card shadow="never"><div class="stat"><span class="num success">{{ monitoring.submittedCount }}</span><span class="label">已交卷</span></div></el-card></el-col>
        <el-col :span="5"><el-card shadow="never"><div class="stat"><span class="num">{{ monitoring.notStartedCount }}</span><span class="label">未开始</span></div></el-card></el-col>
        <el-col :span="5"><el-card shadow="never"><div class="stat"><span class="num danger">{{ monitoring.totalViolations }}</span><span class="label">违规人次</span></div></el-card></el-col>
      </el-row>

      <div class="monitor-toolbar">
        <el-button type="danger" size="small" :loading="forceSubmitting" @click="handleForceSubmit()">
          强制收卷全体进行中
        </el-button>
        <span class="refresh-tip">每 10 秒自动刷新</span>
      </div>

      <el-table v-loading="monitoringLoading" :data="monitoring?.students ?? []" stripe size="small"
        row-key="studentId">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="violation-detail">
              <div v-if="row.violations?.length" class="violation-list">
                <el-tag v-for="(v, i) in row.violations" :key="i" size="small" type="danger" class="v-tag">
                  {{ VIOLATION_LABELS[v.type] ?? v.type }}
                  <span class="v-time">{{ v.time }}</span>
                </el-tag>
              </div>
              <span v-else class="no-violation">无违规记录</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学号" prop="studentNumber" width="110" />
        <el-table-column label="姓名" prop="studentName" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'SUBMITTED' ? 'success' : row.status === 'IN_PROGRESS' ? 'warning' : 'info'">
              {{ { SUBMITTED: '已交卷', IN_PROGRESS: '考试中' }[row.status as 'SUBMITTED' | 'IN_PROGRESS'] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="150">
          <template #default="{ row }">{{ row.startTime ? row.startTime.replace('T', ' ').slice(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column label="剩余时间" width="100">
          <template #default="{ row }">
            <span :class="row.status === 'IN_PROGRESS' && row.remainingSeconds != null && row.remainingSeconds < 300 ? 'time-urgent' : ''">
              {{ formatRemaining(row.remainingSeconds) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING_REVIEW'" size="small" type="warning" effect="plain">
              {{ row.score ?? 0 }}（临时）
            </el-tag>
            <template v-else>{{ row.score ?? '-' }}</template>
          </template>
        </el-table-column>
        <el-table-column label="违规" width="70" prop="violationCount" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'IN_PROGRESS'" link type="danger" size="small"
              :loading="forceSubmitting" @click="handleForceSubmit(row.studentId, row.studentName)">
              强制收卷
            </el-button>
            <span v-else class="op-dash">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.form-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-left: 8px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

.stat .num {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
}

.stat .num.warning { color: #e6a23c; }
.stat .num.success { color: #67c23a; }
.stat .num.danger { color: #f56c6c; }
.stat .num.pending { color: #e6a23c; }

.stat .label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.monitor-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.refresh-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.time-urgent {
  color: #f56c6c;
  font-weight: 600;
}

.violation-detail {
  padding: 8px 16px;
}

.violation-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.v-tag .v-time {
  margin-left: 6px;
  opacity: 0.7;
  font-size: 11px;
}

.no-violation {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

.op-dash {
  color: rgba(255, 255, 255, 0.2);
}
</style>
