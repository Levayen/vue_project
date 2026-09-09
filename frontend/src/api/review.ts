/**
 * 教师人工阅卷接口（M7：主观题简答/论述）
 */
import request from './axios'

/** 待阅考试汇总 */
export interface PendingExam {
  examId: number
  examTitle: string
  courseName: string
  pendingAttempts: number
  pendingQuestions: number
}

/** 待评学生答卷 */
export interface PendingAttempt {
  attemptId: number
  studentId: number
  studentNumber: string
  studentName: string
  className: string
  pendingQuestions: number
  /** 客观题临时分 */
  temporaryScore: number
  submitTime: string
}

/** 单道主观题评分信息 */
export interface SubjectiveAnswer {
  answerId: number
  type: string
  content: string
  referenceAnswer: string | null
  answerText: string | null
  fullScore: number
  /** 未评为 null */
  score: number | null
  /** 3 待评阅 / 1 满分 / 0 未满分 */
  correctFlag: number
  teacherComment: string | null
}

/** 答卷阅卷详情 */
export interface ReviewAttempt {
  attemptId: number
  examId: number
  examTitle: string
  studentNumber: string
  studentName: string
  className: string
  paperTotalScore: number
  currentScore: number
  finalized: boolean
  answers: SubjectiveAnswer[]
}

export interface ScoreRequest {
  score: number
  comment?: string | null
}

export interface ScoreResponse {
  /** 剩余待评题数 */
  remaining: number
  /** 是否触发定稿（全部评完） */
  finalized: boolean
}

export const teacherReviewApi = {
  /** 待阅考试列表 */
  pendingExams: () =>
    request.get<PendingExam[]>('/teacher/reviews/pending').then(res => res.data),
  /** 某场考试的待评学生列表 */
  pendingAttempts: (examId: number) =>
    request.get<PendingAttempt[]>(`/teacher/reviews/exams/${examId}/pending`).then(res => res.data),
  /** 答卷主观题详情 */
  reviewAttempt: (attemptId: number) =>
    request.get<ReviewAttempt>(`/teacher/reviews/attempts/${attemptId}`).then(res => res.data),
  /** 提交单题评分 */
  scoreAnswer: (answerId: number, data: ScoreRequest) =>
    request.post<ScoreResponse>(`/teacher/reviews/answers/${answerId}/score`, data).then(res => res.data)
}
