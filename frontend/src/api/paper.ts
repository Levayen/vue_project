/**
 * 试卷接口（SPEC-exam-paper）
 */
import request from './axios'
import type { PageResult, QuestionOption, QuestionType } from './question'

export type PaperGenerateType = 'MANUAL' | 'RANDOM'
export type PaperStatus = 'DRAFT' | 'PUBLISHED'

export interface PaperItem {
  questionId?: number
  seq?: number
  score?: number
  /** 快照字段（详情返回） */
  type?: QuestionType
  content?: string
  options?: QuestionOption[]
  answer?: string
  analysis?: string
  /** M7：主观题参考答案 */
  referenceAnswer?: string | null
}

export interface Paper {
  id?: number
  name: string
  courseId: number
  courseName?: string
  totalScore?: number
  generateType?: PaperGenerateType
  status?: PaperStatus
  createTime?: string
  items?: PaperItem[]
}

export interface DrawRule {
  type: QuestionType
  /** null 表示不限难度 */
  difficulty: number | null
  count: number
  scorePerQuestion: number
}

export interface PaperQuery {
  courseId?: number
  page?: number
  size?: number
}

export const paperApi = {
  page: (params: PaperQuery) =>
    request.get<PageResult<Paper>>('/papers', { params }).then(res => res.data),
  get: (id: number) => request.get<Paper>(`/papers/${id}`).then(res => res.data),
  createManual: (data: { name: string; courseId: number; items: PaperItem[] }) =>
    request.post<Paper>('/papers/manual', data).then(res => res.data),
  createRandom: (data: { name: string; courseId: number; rules: DrawRule[] }) =>
    request.post<Paper>('/papers/random', data).then(res => res.data),
  update: (id: number, data: { name: string; courseId: number; items: PaperItem[] }) =>
    request.put<Paper>(`/papers/${id}`, data).then(res => res.data),
  remove: (id: number) => request.delete(`/papers/${id}`).then(res => res.data),
  publish: (id: number) => request.post<Paper>(`/papers/${id}/publish`).then(res => res.data)
}
