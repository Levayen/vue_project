/**
 * 题库接口（SPEC-question-bank）
 */
import request from './axios'

export type QuestionType = 'SINGLE' | 'MULTI' | 'JUDGE' | 'FILL' | 'SHORT_ANSWER' | 'ESSAY'

/** 主观题（简答/论述）：文本作答，教师人工阅卷 */
export const SUBJECTIVE_TYPES: QuestionType[] = ['SHORT_ANSWER', 'ESSAY']

export function isSubjective(type: string): type is QuestionType {
  return SUBJECTIVE_TYPES.includes(type as QuestionType)
}

export interface QuestionOption {
  key: string
  text: string
}

export interface Question {
  id?: number
  courseId: number
  courseName?: string
  type: QuestionType
  content: string
  options: QuestionOption[]
  answer: string
  score: number
  difficulty: number
  analysis?: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface QuestionQuery {
  courseId?: number
  type?: QuestionType | ''
  difficulty?: number | ''
  keyword?: string
  page?: number
  size?: number
}

/** Spring Page 序列化为 {content,totalElements,...} */
export const questionApi = {
  page: (params: QuestionQuery) =>
    request.get<PageResult<Question>>('/questions', { params }).then(res => res.data),
  get: (id: number) => request.get<Question>(`/questions/${id}`).then(res => res.data),
  create: (data: Omit<Question, 'id' | 'courseName'>) =>
    request.post<Question>('/questions', data).then(res => res.data),
  update: (id: number, data: Omit<Question, 'id' | 'courseName'>) =>
    request.put<Question>(`/questions/${id}`, data).then(res => res.data),
  remove: (id: number) => request.delete(`/questions/${id}`).then(res => res.data)
}
