<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElDatePicker, ElMessage, ElMessageBox } from 'element-plus'
import { studentApi, classApi, type Student, type ClassInfo } from '@/api'

const students = ref<Student[]>([])
const classes = ref<ClassInfo[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加学生')
const searchKeyword = ref('')
const selectedClassId = ref<number>(0)
const form = ref<Student>({
  studentNumber: '', name: '', gender: '', birthDate: '', phone: '', email: '', classInfo: undefined
})

const rules = {
  studentNumber: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const filteredStudents = computed(() => {
  if (!searchKeyword.value) return students.value
  return students.value.filter(s => s.name.includes(searchKeyword.value) || s.studentNumber.includes(searchKeyword.value))
})

const loadStudents = async () => {
  try { const res = await studentApi.getAll(); students.value = res.data } catch (error) { ElMessage.error('加载学生数据失败') }
}
const loadClasses = async () => {
  try { const res = await classApi.getAll(); classes.value = res.data } catch (error) { ElMessage.error('加载班级数据失败') }
}

const openAddDialog = () => {
  dialogTitle.value = '添加学生'
  form.value = { studentNumber: '', name: '', gender: '', birthDate: '', phone: '', email: '', classInfo: undefined }
  selectedClassId.value = 0
  dialogVisible.value = true
}

const openEditDialog = (student: Student) => {
  dialogTitle.value = '编辑学生'
  form.value = { id: student.id, studentNumber: student.studentNumber, name: student.name, gender: student.gender || '', birthDate: student.birthDate || '', phone: student.phone || '', email: student.email || '', classInfo: student.classInfo ? { ...student.classInfo } : undefined }
  selectedClassId.value = student.classInfo?.id || 0
  dialogVisible.value = true
}

const saveStudent = async () => {
  try {
    const requestData = { ...form.value }
    if (selectedClassId.value && selectedClassId.value !== 0) { requestData.classInfo = { id: selectedClassId.value } } else { delete requestData.classInfo }
    if (form.value.id) { await studentApi.update(form.value.id, requestData); ElMessage.success('学生信息更新成功') }
    else { await studentApi.create(requestData); ElMessage.success('学生添加成功') }
    dialogVisible.value = false; loadStudents()
  } catch (error) { ElMessage.error('操作失败') }
}

const deleteStudent = async (id: number) => {
  try { await ElMessageBox.confirm('确定删除该学生吗？', '提示', { type: 'warning' }); await studentApi.delete(id); ElMessage.success('删除成功'); loadStudents() } catch (error) {}
}

onMounted(() => { loadStudents(); loadClasses() })
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2><span class="title-accent">◆</span> 学生管理</h2>
      <div class="header-actions">
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <el-input v-model="searchKeyword" placeholder="搜索姓名或学号" />
        </div>
        <el-button type="primary" @click="openAddDialog">+ 添加学生</el-button>
      </div>
    </div>
    
    <el-table :data="filteredStudents" class="tech-table">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="studentNumber" label="学号" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="gender" label="性别" />
      <el-table-column prop="birthDate" label="出生日期" />
      <el-table-column prop="phone" label="电话" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="classInfo.className" label="班级" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row as Student)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteStudent(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" append-to-body>
      <el-form :model="form" :rules="rules">
        <el-form-item label="学号" prop="studentNumber">
          <el-input v-model="form.studentNumber" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="请选择性别">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="form.birthDate" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="selectedClassId" placeholder="请选择班级">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id || 0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStudent">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  background: rgba(13, 17, 23, 0.8);
  border: 1px solid rgba(0, 255, 255, 0.1);
  border-radius: 12px;
  padding: 24px;
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.page-container::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.5), transparent);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-family: 'Orbitron', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #e0e6ed;
  letter-spacing: 2px;
}

.title-accent { color: #00ffff; font-size: 12px; margin-right: 4px; }

.header-actions { display: flex; gap: 12px; align-items: center; }

.search-box {
  display: flex;
  align-items: center;
  border: 1px solid rgba(0, 255, 255, 0.2);
  border-radius: 6px;
  padding: 0 12px;
  background: rgba(0, 255, 255, 0.03);
}

.search-icon { color: rgba(0, 255, 255, 0.5); margin-right: 8px; }
</style>
