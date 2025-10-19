<script setup lang="ts">
/**
 * 课程管理页面组件
 * 提供课程列表展示、添加、编辑和删除功能
 */
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox } from 'element-plus'
import { courseApi, type Course } from '@/api'

/**
 * 课程列表数据
 */
const courses = ref<Course[]>([])

/**
 * 对话框显示状态
 */
const dialogVisible = ref(false)

/**
 * 对话框标题
 */
const dialogTitle = ref('添加课程')

/**
 * 表单数据
 */
const form = ref<Course>({
  courseName: '',
  credits: 0,
  description: ''
})

/**
 * 表单校验规则
 */
const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
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
 * 打开添加课程对话框
 */
const openAddDialog = () => {
  dialogTitle.value = '添加课程'
  form.value = { courseName: '', credits: 0, description: '' }
  dialogVisible.value = true
}

/**
 * 打开编辑课程对话框
 * @param course 要编辑的课程对象
 */
const openEditDialog = (course: Course) => {
  dialogTitle.value = '编辑课程'
  form.value = { ...course }
  dialogVisible.value = true
}

/**
 * 保存课程信息（添加或更新）
 */
const saveCourse = async () => {
  try {
    if (form.value.id) {
      await courseApi.update(form.value.id, form.value)
      ElMessage.success('课程信息更新成功')
    } else {
      await courseApi.create(form.value)
      ElMessage.success('课程添加成功')
    }
    dialogVisible.value = false
    loadCourses()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

/**
 * 删除课程
 * @param id 课程ID
 */
const deleteCourse = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该课程吗？', '提示', { type: 'warning' })
    await courseApi.delete(id)
    ElMessage.success('删除成功')
    loadCourses()
  } catch (error) {
    // 用户取消删除
  }
}

/**
 * 组件挂载时加载数据
 */
onMounted(() => {
  loadCourses()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>课程管理</h2>
      <el-button type="primary" @click="openAddDialog">添加课程</el-button>
    </div>
    
    <el-table :data="courses" border>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="450px">
      <el-form :model="form" :rules="rules">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="学分">
          <el-input-number v-model="form.credits" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="form.description" type="textarea" placeholder="请输入课程描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">确定</el-button>
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