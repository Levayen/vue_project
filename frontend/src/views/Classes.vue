<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElMessage, ElMessageBox } from 'element-plus'
import { classApi, type ClassInfo } from '@/api'

const classes = ref<ClassInfo[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加班级')
const form = ref<ClassInfo>({ className: '', grade: '', major: '' })
const rules = { className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }] }

const loadClasses = async () => { try { const res = await classApi.getAll(); classes.value = res.data } catch (error) { ElMessage.error('加载班级数据失败') } }
const openAddDialog = () => { dialogTitle.value = '添加班级'; form.value = { className: '', grade: '', major: '' }; dialogVisible.value = true }
const openEditDialog = (cls: ClassInfo) => { dialogTitle.value = '编辑班级'; form.value = { ...cls }; dialogVisible.value = true }

const saveClass = async () => {
  try {
    if (form.value.id) { await classApi.update(form.value.id, form.value); ElMessage.success('班级信息更新成功') }
    else { await classApi.create(form.value); ElMessage.success('班级添加成功') }
    dialogVisible.value = false; loadClasses()
  } catch (error) { ElMessage.error('操作失败') }
}

const deleteClass = async (id: number) => { try { await ElMessageBox.confirm('确定删除该班级吗？', '提示', { type: 'warning' }); await classApi.delete(id); ElMessage.success('删除成功'); loadClasses() } catch (error) {} }

onMounted(() => { loadClasses() })
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2><span class="title-accent">◆</span> 班级管理</h2>
      <el-button type="primary" @click="openAddDialog">+ 添加班级</el-button>
    </div>
    <el-table :data="classes" class="tech-table">
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
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px" append-to-body>
      <el-form :model="form" :rules="rules">
        <el-form-item label="班级名称" prop="className"><el-input v-model="form.className" placeholder="请输入班级名称" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="form.grade" placeholder="请输入年级" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="form.major" placeholder="请输入专业" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveClass">确定</el-button></template>
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
