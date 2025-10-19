<script setup lang="ts">
/**
 * 选课管理页面组件
 * 提供选课列表展示、选课、成绩更新和退课功能
 */
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElSelect, ElOption, ElInputNumber, ElMessage, ElMessageBox } from 'element-plus'
import { enrollmentApi, studentApi, courseApi, type Enrollment, type Student, type Course } from '@/api'

/**
 * 选课记录列表数据
 */
const enrollments = ref<Enrollment[]>([])

/**
 * 学生列表数据，用于下拉选择
 */
const students = ref<Student[]>([])

/**
 * 课程列表数据，用于下拉选择
 */
const courses = ref<Course[]>([])

/**
 * 对话框显示状态
 */
const dialogVisible = ref(false)

/**
 * 对话框标题
 */
const dialogTitle = ref('选课')

/**
 * 表单数据
 */
const form = ref({
  studentId: 0 as number,
  courseId: 0 as number
})

/**
 * 加载选课记录列表数据
 */
const loadEnrollments = async () => {
  try {
    const res = await enrollmentApi.getAll()
    enrollments.value = res.data
  } catch (error) {
    ElMessage.error('加载选课数据失败')
  }
}

/**
 * 加载学生列表数据
 */
const loadStudents = async () => {
  try {
    const res = await studentApi.getAll()
    students.value = res.data
  } catch (error) {
    ElMessage.error('加载学生数据失败')
  }
}

/**
 * 加载课程列表数据
 */
const loadCourses = async () => {
  try {
    const res = await courseApi.getAll()
    courses.value = res.data
  } catch (error) {
    ElMessage.error('加载课程数据失败')
  }
}

/**
 * 打开选课对话框
 */
const openAddDialog = () => {
  dialogTitle.value = '选课'
  form.value = { studentId: 0, courseId: 0 }
  dialogVisible.value = true
}

/**
 * 保存选课记录
 */
const saveEnrollment = async () => {
  try {
    if (form.value.studentId && form.value.courseId) {
      await enrollmentApi.create(form.value.studentId, form.value.courseId)
      ElMessage.success('选课成功')
      dialogVisible.value = false
      loadEnrollments()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

/**
 * 更新成绩
 * @param id 选课记录ID
 * @param grade 成绩
 */
const updateGrade = async (id: number, grade: number | undefined) => {
  try {
    if (grade !== undefined) {
      await enrollmentApi.updateGrade(id, grade)
      ElMessage.success('成绩更新成功')
      loadEnrollments()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

/**
 * 删除选课记录（退课）
 * @param studentId 学生ID
 * @param courseId 课程ID
 */
const deleteEnrollment = async (studentId: number, courseId: number) => {
  try {
    await ElMessageBox.confirm('确定退课吗？', '提示', { type: 'warning' })
    await enrollmentApi.delete(studentId, courseId)
    ElMessage.success('退课成功')
    loadEnrollments()
  } catch (error) {
    // 用户取消退课
  }
}

/**
 * 组件挂载时加载数据
 */
onMounted(() => {
  loadEnrollments()
  loadStudents()
  loadCourses()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>选课管理</h2>
      <el-button type="primary" @click="openAddDialog">选课</el-button>
    </div>
    
    <el-table :data="enrollments" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="学生">
        <template #default="{ row }">{{ row.student?.name }} ({{ row.student?.studentNumber }})</template>
      </el-table-column>
      <el-table-column label="班级">
        <template #default="{ row }">{{ row.student?.classInfo?.className }}</template>
      </el-table-column>
      <el-table-column label="课程">
        <template #default="{ row }">{{ row.course?.courseName }}</template>
      </el-table-column>
      <el-table-column label="学分">
        <template #default="{ row }">{{ row.course?.credits }}</template>
      </el-table-column>
      <el-table-column label="成绩">
        <template #default="{ row }">
          <el-input-number 
            :model-value="row.grade" 
            :min="0" 
            :max="100" 
            size="small"
            @change="(val) => updateGrade(row.id, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="deleteEnrollment(row.student?.id || 0, row.course?.id || 0)">退课</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="450px">
      <el-form :model="form">
        <el-form-item label="学生" required>
          <el-select v-model="form.studentId" placeholder="请选择学生">
            <el-option 
              v-for="s in students" 
              :key="s.id" 
              :label="s.name + ' (' + s.studentNumber + ')'" 
              :value="s.id || 0" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课程" required>
          <el-select v-model="form.courseId" placeholder="请选择课程">
            <el-option 
              v-for="c in courses" 
              :key="c.id" 
              :label="c.courseName + ' (' + c.credits + '学分)'" 
              :value="c.id || 0" 
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEnrollment">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
}
</style>