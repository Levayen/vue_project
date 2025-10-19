<script setup lang="ts">
/**
 * 班级管理页面组件
 * 提供班级列表展示、添加、编辑和删除功能
 */
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox } from 'element-plus'
import { classApi, type ClassInfo } from '@/api'

/**
 * 班级列表数据
 */
const classes = ref<ClassInfo[]>([])

/**
 * 对话框显示状态
 */
const dialogVisible = ref(false)

/**
 * 对话框标题
 */
const dialogTitle = ref('添加班级')

/**
 * 表单数据
 */
const form = ref<ClassInfo>({
  className: '',
  grade: '',
  major: ''
})

/**
 * 表单校验规则
 */
const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }]
}

/**
 * 加载班级列表数据
 */
const loadClasses = async () => {
  try {
    const res = await classApi.getAll()
    classes.value = res.data
  } catch (error) {
    ElMessage.error('加载班级数据失败')
  }
}

/**
 * 打开添加班级对话框
 */
const openAddDialog = () => {
  dialogTitle.value = '添加班级'
  form.value = { className: '', grade: '', major: '' }
  dialogVisible.value = true
}

/**
 * 打开编辑班级对话框
 * @param cls 要编辑的班级对象
 */
const openEditDialog = (cls: ClassInfo) => {
  dialogTitle.value = '编辑班级'
  form.value = { ...cls }
  dialogVisible.value = true
}

/**
 * 保存班级信息（添加或更新）
 */
const saveClass = async () => {
  try {
    if (form.value.id) {
      await classApi.update(form.value.id, form.value)
      ElMessage.success('班级信息更新成功')
    } else {
      await classApi.create(form.value)
      ElMessage.success('班级添加成功')
    }
    dialogVisible.value = false
    loadClasses()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

/**
 * 删除班级
 * @param id 班级ID
 */
const deleteClass = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该班级吗？', '提示', { type: 'warning' })
    await classApi.delete(id)
    ElMessage.success('删除成功')
    loadClasses()
  } catch (error) {
    // 用户取消删除
  }
}

/**
 * 组件挂载时加载数据
 */
onMounted(() => {
  loadClasses()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>班级管理</h2>
      <el-button type="primary" @click="openAddDialog">添加班级</el-button>
    </div>
    
    <el-table :data="classes" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="className" label="班级名称" />
      <el-table-column prop="grade" label="年级" />
      <el-table-column prop="major" label="专业" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row as ClassInfo)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteClass(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px">
      <el-form :model="form" :rules="rules">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="form.grade" placeholder="请输入年级" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" placeholder="请输入专业" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClass">确定</el-button>
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