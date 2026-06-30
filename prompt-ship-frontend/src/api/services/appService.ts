import {
  deployApp,
  getAppVoById,
  listGoodAppVoByPage,
  listMyAppVoByPage,
  updateApp,
} from '@/api/generated/appController'
import { API_BASE_URL } from '@/request'

export const INITIAL_PROMPT_STORAGE_KEY = 'prompt_ship_initial_prompt'
export const DEFAULT_CODE_GEN_TYPE = 'multi_file'

export type AppId = string

export type AppVO = Omit<API.AppVO, 'id' | 'userId'> & {
  id?: AppId
  userId?: AppId
}

export type AppPage = Omit<API.PageAppVO, 'records'> & {
  records?: AppVO[]
}

export type AppPageParams = Omit<API.listMyAppVOByPageParams, 'pageNum'> & {
  pageNum?: number
  pageNumber?: number
}

export type ChatStreamHandlers = {
  onAppId?: (appId: AppId) => void
  onChunk?: (chunk: string) => void
  onDone?: () => void
}

const normalizeAppId = (id?: string | number) => (id == null ? undefined : String(id))

const normalizeApp = (app?: API.AppVO): AppVO | undefined => {
  if (!app) {
    return undefined
  }
  return {
    ...app,
    id: normalizeAppId(app.id),
    userId: normalizeAppId(app.userId),
  }
}

const normalizePage = (page?: API.PageAppVO): AppPage | undefined => {
  if (!page) {
    return undefined
  }
  return {
    ...page,
    records: page.records?.map(normalizeApp).filter(Boolean) as AppVO[] | undefined,
  }
}

export const listMyApps = async (params: AppPageParams = {}) => {
  const { pageNumber, ...restParams } = params
  const res = await listMyAppVoByPage({
    ...restParams,
    pageNum: pageNumber ?? restParams.pageNum,
  })
  return {
    ...res,
    data: {
      ...res.data,
      data: normalizePage(res.data.data),
    },
  }
}

export const listGoodApps = async (params: AppPageParams = {}) => {
  const { pageNumber, ...restParams } = params
  const res = await listGoodAppVoByPage({
    ...restParams,
    pageNum: pageNumber ?? restParams.pageNum,
  })
  return {
    ...res,
    data: {
      ...res.data,
      data: normalizePage(res.data.data),
    },
  }
}

export const getAppDetail = async (id: AppId) => {
  const res = await getAppVoById({ id: id as unknown as number })
  return {
    ...res,
    data: {
      ...res.data,
      data: normalizeApp(res.data.data),
    },
  }
}

export const deployMyApp = (appId: AppId) => deployApp({ appId: appId as unknown as number })

export const updateMyApp = updateApp

export const getGeneratedPreviewUrl = (appId?: AppId, codeGenType = DEFAULT_CODE_GEN_TYPE) => {
  if (!appId) {
    return ''
  }
  return `${API_BASE_URL}/static/${codeGenType}_${appId}/`
}

// SSE 需要逐段读取响应流；axios 更适合普通 JSON 请求，这里使用浏览器原生 fetch。
export const chatToGenerateApp = async (
  body: API.AppCreateRequest,
  handlers: ChatStreamHandlers,
) => {
  const response = await fetch(`${API_BASE_URL}/app/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(body),
  })

  if (!response.ok || !response.body) {
    throw new Error(`生成请求失败：${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const handleEvent = (rawEvent: string) => {
    const lines = rawEvent.split(/\r?\n/)
    const eventName = lines
      .find((line) => line.startsWith('event:'))
      ?.replace('event:', '')
      .trim()
    const data = lines
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.replace(/^data:\s?/, ''))
      .join('\n')

    if (eventName === 'done') {
      handlers.onDone?.()
      return
    }

    if (!data) {
      return
    }

    try {
      const parsed = JSON.parse(data) as { i?: string | number; d?: string }
      if (parsed.i != null) {
        handlers.onAppId?.(String(parsed.i))
      }
      if (typeof parsed.d === 'string') {
        handlers.onChunk?.(parsed.d)
      }
    } catch {
      handlers.onChunk?.(data)
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() ?? ''
    events.filter(Boolean).forEach(handleEvent)
  }

  if (buffer.trim()) {
    handleEvent(buffer)
  }
}