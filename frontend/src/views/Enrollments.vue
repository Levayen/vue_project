<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElSelect, ElOption, ElInputNumber, ElMessage, ElMessageBox } from 'element-plus'
import { enrollmentApi, studentApi, courseApi, type Enrollment, type Student, type Course } from '@/api'

const enrollments = ref<Enrollment[]>([])
const students = ref<Student[]>([])
const courses = ref<Course[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('选课')
const form = ref({ studentId: 0 as number, courseId: 0 as number })

const loadEnrollments = async () => { try { const res = await enrollmentApi.getAll(); enrollments.value = res.data } catch (error) { ElMessage.error('加载选课数据失败') } }
const loadStudents = async () => { try { const res = await studentApi.getAll(); students.value = res.data } catch (error) { ElMessage.error('加载学生数据失败') } }
const loadCourses = async () => { try { const res = await courseApi.getAll(); courses.value = res.data } catch (error) { ElMessage.error('加载课程数据失败') } }

const openAddDialog = () => { dialogTitle.value = '选课'; form.value = { studentId: 0, courseId: 0 }; dialogVisible.value = true }

const saveEnrollment = async () => {
  try {
    if (form.value.studentId && form.value.courseId) { await enrollmentApi.create(form.value.studentId, form.value.courseId); ElMessage.success('选课成功'); dialogVisible.value = false; loadEnrollments() }
  } catch (error) { ElMessage.error('操作失败') }
}

const updateGrade = async (id: number, grade: number | undefined) => {
  try { if (grade !== undefined) { await enrollmentApi.updateGrade(id, grade); ElMessage.success('成绩更新成功'); loadEnrollments() } } catch (error) { ElMessage.error('操作失败') }
}

const deleteEnrollment = async (studentId: number, courseId: number) => { try { await ElMessageBox.confirm('确定退课吗？', '提示', { type: 'warning' }); await enrollmentApi.delete(studentId, courseId); ElMessage.success('退课成功'); loadEnrollments() } catch (error) {} }

onMounted(() => { loadEnrollments(); loadStudents(); loadCourses() })
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2><span class="title-accent">◆</span> 选课管理</h2>
      <el-button type="primary" @click="openAddDialog">+ 选课</el-button>
    </div>
    <el-table :data="enrollments" class="tech-table">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="学生"><template #default="{ row }">{{ row.student?.name }} ({{ row.student?.studentNumber }})</template></el-table-column>
      <el-table-column label="班级"><template #default="{ row }">{{ row.student?.classInfo?.className }}</template></el-table-column>
      <el-table-column label="课程"><template #default="{ row }">{{ row.course?.courseName }}</template></el-table-column>
      <el-table-column label="学分"><template #default="{ row }">{{ row.course?.credits }}</template></el-table-column>
      <el-table-column label="成绩">
        <template #default="{ row }">
          <el-input-number :model-value="row.grade" :min="0" :max="100" size="small" @change="(val: number) => updateGrade(row.id, val)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="deleteEnrollment(row.student?.id || 0, row.course?.id || 0)">退课</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="450px" append-to-body>
      <el-form :model="form">
        <el-form-item label="学生" required>
          <el-select v-model="form.studentId" placeholder="请选择学生">
            <el-option v-for="s in students" :key="s.id" :label="s.name + ' (' + s.studentNumber + ')'" :value="s.id || 0" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程" required>
          <el-select v-model="form.courseId" placeholder="请选择课程">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName + ' (' + c.credits + '学分)'" :value="c.id || 0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveEnrollment">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { background: rgba(13, 17, 23, 0.8); border: 1px solid rgba(0, 255, 255, 0.1); border-radius: 12px; padding: 24px; backdrop-filter: blur(10px); position: relative; overflow: hidden; }
.page-container::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 1px; background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.5), transparent); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-family: 'Orbitron', sans-serif; font-size: 18px; font-weight: 700; color: #e0e6ed; letter-spacing: 2px; }
.title-accent { color: #00ffff; font-size: 12px; margin-right: 4px; }
</style>
