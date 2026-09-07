/**
 * API 接口定义和类型声明
 * 包含学生、班级、课程、选课相关的接口类型和 API 方法
 */
import axios from './axios'

/**
 * 学生接口类型
 */
export interface Student {
  id?: number
  studentNumber: string
  name: string
  gender?: string
  birthDate?: string
  phone?: string
  email?: string
  classInfo?: { id: number; className?: string }
}

/**
 * 班级接口类型
 */
export interface ClassInfo {
  id?: number
  className: string
  grade?: string
  major?: string
}

/**
 * 课程接口类型
 */
export interface Course {
  id?: number
  courseName: string
  credits?: number
  description?: string
}

/**
 * 选课记录接口类型
 */
export interface Enrollment {
  id?: number
  student?: Student
  course?: Course
  grade?: number
}

/**
 * 学生管理 API
 * 提供学生相关的 CRUD 操作
 */
export const studentApi = {
  getAll: () => axios.get<Student[]>('/students'),
  getById: (id: number) => axios.get<Student>(`/students/${id}`),
  create: (data: Student) => axios.post<Student>('/students', data),
  update: (id: number, data: Student) => axios.put<Student>(`/students/${id}`, data),
  delete: (id: number) => axios.delete(`/students/${id}`),
  getByClass: (classId: number) => axios.get<Student[]>(`/students/class/${classId}`),
  searchByName: (name: string) => axios.get<Student[]>(`/students/search?name=${name}`)
}

/**
 * 班级管理 API
 * 提供班级相关的 CRUD 操作
 */
export const classApi = {
  getAll: () => axios.get<ClassInfo[]>('/classes'),
  getById: (id: number) => axios.get<ClassInfo>(`/classes/${id}`),
  create: (data: ClassInfo) => axios.post<ClassInfo>('/classes', data),
  update: (id: number, data: ClassInfo) => axios.put<ClassInfo>(`/classes/${id}`, data),
  delete: (id: number) => axios.delete(`/classes/${id}`)
}

/**
 * 课程管理 API
 * 提供课程相关的 CRUD 操作
 */
export const courseApi = {
  getAll: () => axios.get<Course[]>('/courses'),
  getById: (id: number) => axios.get<Course>(`/courses/${id}`),
  create: (data: Course) => axios.post<Course>('/courses', data),
  update: (id: number, data: Course) => axios.put<Course>(`/courses/${id}`, data),
  delete: (id: number) => axios.delete(`/courses/${id}`)
}

/**
 * 选课管理 API
 * 提供选课相关的操作，包括选课、退课、成绩管理
 */
export const enrollmentApi = {
  getAll: () => axios.get<Enrollment[]>('/enrollments'),
  getByStudent: (studentId: number) => axios.get<Enrollment[]>(`/enrollments/student/${studentId}`),
  getByCourse: (courseId: number) => axios.get<Enrollment[]>(`/enrollments/course/${courseId}`),
  create: (studentId: number, courseId: number) => axios.post<Enrollment>('/enrollments', { studentId, courseId }),
  updateGrade: (id: number, grade: number) => axios.put<Enrollment>(`/enrollments/${id}/grade`, { grade }),
  delete: (studentId: number, courseId: number) => axios.delete(`/enrollments?studentId=${studentId}&courseId=${courseId}`)
}