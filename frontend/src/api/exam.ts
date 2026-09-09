/**
 * 在线考试接口（SPEC-exam-session）
 */
import request from './axios'
import type { QuestionOption, QuestionType } from './question'

export type ScoreRule = 'BEST' | 'LAST'

export interface Exam {
  id?: number
  paperId: number
  paperName?: string
  courseId: number
  courseName?: string
  title: string
  startTime: string
  endTime: string
  durationMinutes: number
  maxAttempts: number
  scoreRule: ScoreRule
  shuffle: boolean
  openBook: boolean
  totalScore?: number
}

export interface StudentExamItem {
  examId: number
  title: string
  courseName: string
  startTime: string
  endTime: string
  durationMinutes: number
  maxAttempts: number
  openBook: boolean
  totalScore: number
  /** NOT_STARTED / AVAILABLE / ENDED / ATTEMPTS_USED */
  status: string
  submittedAttempts: number
  score: number | null
}

export interface ExamQuestion {
  paperQuestionId: number
  type: QuestionType
  content: string
  options: QuestionOption[]
  score: number
  /** 闭卷考试为 null */
  answer: string | null
  /** 闭卷考试为 null */
  analysis: string | null
}

export interface StartExamResult {
  attemptId: number
  examId: number
  title: string
  openBook: boolean
  deadline: string
  remainingSeconds: number
  questions: ExamQuestion[]
  existingAnswers: string
}

export interface SubmitResult {
  attemptId: number
  score: number
  totalScore: number
  /** M7：true 表示含主观题待教师阅卷，score 为客观题临时分 */
  grading: boolean
}

export interface ViolationEvent {
  type: string
  time: string
}

export interface MonitoringStudent {
  studentId: number
  studentName: string
  studentNumber: string
  /** 未开始 / IN_PROGRESS / SUBMITTED */
  status: string
  attemptCount: number
  score: number | null
  violationCount: number
  startTime: string | null
  /** 剩余秒数（进行中有值） */
  remainingSeconds: number | null
  violations: ViolationEvent[]
}

export interface MonitoringResult {
  examId: number
  title: string
  enrolledCount: number
  inProgressCount: number
  submittedCount: number
  notStartedCount: number
  /** M7：待教师阅卷的答卷数 */
  pendingReviewCount?: number
  totalViolations: number
  students: MonitoringStudent[]
}

/** 教师端 */
export const examApi = {
  publish: (data: Omit<Exam, 'id' | 'paperName' | 'courseName' | 'totalScore'>) =>
    request.post<Exam>('/exams', data).then(res => res.data),
  list: (courseId?: number) =>
    request.get<Exam[]>('/exams', { params: courseId ? { courseId } : {} }).then(res => res.data),
  monitoring: (id: number) =>
    request.get<MonitoringResult>(`/exams/${id}/monitoring`).then(res => res.data),
  forceSubmit: (id: number, studentId?: number) =>
    request.post<{ forcedCount: number }>(`/exams/${id}/force-submit`, studentId ? { studentId } : {})
      .then(res => res.data)
}

/** 学生端 */
export const studentExamApi = {
  myExams: () => request.get<StudentExamItem[]>('/student/exams').then(res => res.data),
  start: (examId: number) =>
    request.post<StartExamResult>(`/student/exams/${examId}/start`).then(res => res.data),
  saveAnswers: (examId: number, answers: Record<number, string>) =>
    request.put(`/student/exams/${examId}/answers`, { answers }).then(res => res.data),
  reportViolation: (examId: number, type: string) =>
    request.post(`/student/exams/${examId}/violations`, { type }).then(res => res.data),
  submit: (examId: number) =>
    request.post<SubmitResult>(`/student/exams/${examId}/submit`).then(res => res.data)
}
