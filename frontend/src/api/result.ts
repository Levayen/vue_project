/**
 * 成绩与统计接口（SPEC-results）
 */
import request from './axios'
import type { QuestionOption } from './question'

export interface StudentResult {
  examId: number
  examTitle: string
  courseName: string
  /** pending=true 时为客观题临时分 */
  totalScore: number
  attemptCount: number
  submitTime: string | null
  /** pending=true 时无意义（成绩未定稿） */
  pass: boolean
  /** M7：成绩待定（主观题待教师阅卷） */
  pending?: boolean
}

export interface QuestionReview {
  type: string
  content: string
  options: QuestionOption[]
  /** 未答为 null；主观题为作答原文 */
  studentAnswer: string | null
  /** 主观题为空字符串，参考答案见 referenceAnswer */
  correctAnswer: string
  /** 0 错 / 1 对 / 3 待评阅（主观题未评） */
  correctFlag: number
  analysis: string | null
  score: number
  fullScore: number
  /** M7：主观题参考答案 */
  referenceAnswer?: string | null
  /** M7：教师阅卷评语 */
  teacherComment?: string | null
}

export interface ExamReview {
  examId: number
  title: string
  courseName: string
  score: number
  fullScore: number
  submitTime: string | null
  questions: QuestionReview[]
}

export interface WrongQuestion {
  examId: number
  examTitle: string
  courseName: string
  type: string
  content: string
  options: QuestionOption[]
  /** 主观题为作答原文 */
  studentAnswer: string | null
  /** 主观题为空，参考答案见 referenceAnswer */
  correctAnswer: string
  analysis: string | null
  score: number
  fullScore: number
  submitTime: string | null
  /** M7：主观题参考答案 */
  referenceAnswer?: string | null
}

export interface ScoreBucket {
  label: string
  count: number
}

export interface ExamStats {
  attended: number
  avgScore: number
  maxScore: number
  minScore: number
  passRate: number
  buckets: ScoreBucket[]
}

export interface TeacherRecord {
  studentId: number
  studentNumber: string
  studentName: string
  className: string
  totalScore: number
  attemptCount: number
  violationCount: number
  /** M7：成绩待定（待阅卷），totalScore 为临时分 */
  pending?: boolean
}

/** 学生端 */
export const studentResultApi = {
  myResults: () => request.get<StudentResult[]>('/student/results').then(res => res.data),
  review: (examId: number) =>
    request.get<ExamReview>(`/student/results/${examId}/review`).then(res => res.data),
  wrongBook: (courseId?: number) =>
    request.get<WrongQuestion[]>('/student/wrong-book', { params: courseId ? { courseId } : {} }).then(res => res.data)
}

/** 教师端 */
export const teacherStatsApi = {
  records: (examId: number, classId?: number) =>
    request.get<TeacherRecord[]>(`/teacher/exams/${examId}/records`, { params: classId ? { classId } : {} }).then(res => res.data),
  stats: (examId: number, classId?: number) =>
    request.get<ExamStats>(`/teacher/exams/${examId}/stats`, { params: classId ? { classId } : {} }).then(res => res.data)
}
