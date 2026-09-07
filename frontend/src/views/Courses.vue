<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox } from 'element-plus'
import { courseApi, type Course } from '@/api'

const courses = ref<Course[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加课程')
const form = ref<Course>({ courseName: '', credits: 0, description: '' })
const rules = { courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }] }

const loadCourses = async () => { try { const res = await courseApi.getAll(); courses.value = res.data } catch (error) { ElMessage.error('加载课程数据失败') } }
const openAddDialog = () => { dialogTitle.value = '添加课程'; form.value = { courseName: '', credits: 0, description: '' }; dialogVisible.value = true }
const openEditDialog = (course: Course) => { dialogTitle.value = '编辑课程'; form.value = { ...course }; dialogVisible.value = true }

const saveCourse = async () => {
  try {
    if (form.value.id) { await courseApi.update(form.value.id, form.value); ElMessage.success('课程信息更新成功') }
    else { await courseApi.create(form.value); ElMessage.success('课程添加成功') }
    dialogVisible.value = false; loadCourses()
  } catch (error) { ElMessage.error('操作失败') }
}

const deleteCourse = async (id: number) => { try { await ElMessageBox.confirm('确定删除该课程吗？', '提示', { type: 'warning' }); await courseApi.delete(id); ElMessage.success('删除成功'); loadCourses() } catch (error) {} }

onMounted(() => { loadCourses() })
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2><span class="title-accent">◆</span> 课程管理</h2>
      <el-button type="primary" @click="openAddDialog">+ 添加课程</el-button>
    </div>
    <el-table :data="courses" class="tech-table">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="courseName" label="课程名称" />
      <el-table-column prop="credits" label="学分" />
      <el-table-column prop="description" label="课程描述" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row as Course)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteCourse(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="450px" append-to-body>
      <el-form :model="form" :rules="rules">
        <el-form-item label="课程名称" prop="courseName"><el-input v-model="form.courseName" placeholder="请输入课程名称" /></el-form-item>
        <el-form-item label="学分"><el-input-number v-model="form.credits" :min="1" :max="10" /></el-form-item>
        <el-form-item label="课程描述"><el-input v-model="form.description" type="textarea" placeholder="请输入课程描述" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveCourse">确定</el-button></template>
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
