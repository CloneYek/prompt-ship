import {
  deleteApp,
  deleteAppByAdmin,
  deployApp,
  getAppVoById,
  getAppVoByIdByAdmin,
  listAppVoByPageByAdmin,
  listGoodAppVoByPage,
  listMyAppVoByPage,
  updateApp,
  updateAppByAdmin,
} from '@/api/generated/appController'
import { API_BASE_URL } from '@/request'

export const INITIAL_PROMPT_STORAGE_KEY = 'prompt_ship_initial_prompt'
export const DEFAULT_CODE_GEN_TYPE = 'multi_file'
export const DEFAULT_APP_COVER =
  'https://cdn.phototourl.com/free/2026-07-02-40ee78e7-12d4-47f5-9554-4fa777136113.png'

export type AppId = string

export type AppCreator = Omit<API.LoginUserVO, 'id'> & {
  id?: AppId
}

export type AppVO = Omit<API.AppVO, 'id' | 'userId' | 'user'> & {
  id?: AppId
  userId?: AppId
  user?: AppCreator
}

export type AppPage = Omit<API.PageAppVO, 'records'> & {
  records?: AppVO[]
}

export type AppPageParams = Omit<API.listMyAppVOByPageParams, 'pageNum'> & {
  pageNum?: number
  pageNumber?: number
}

export type AdminAppPageParams = Omit<API.listAppVOByPageByAdminParams, 'pageNum' | 'userId'> & {
  pageNum?: number
  pageNumber?: number
  userId?: AppId
}

export type ChatStreamHandlers = {
  onAppId?: (appId: AppId) => void
  onChunk?: (chunk: string) => void
  onDone?: () => void
}

const normalizeAppId = (id?: string | number) => (id == null ? undefined : String(id))

const normalizeCreator = (user?: API.LoginUserVO): AppCreator | undefined => {
  if (!user) {
    return undefined
  }
  return {
    ...user,
    id: normalizeAppId(user.id),
  }
}

const normalizeApp = (app?: API.AppVO): AppVO | undefined => {
  if (!app) {
    return undefined
  }
  return {
    ...app,
    cover: app.cover || DEFAULT_APP_COVER,
    id: normalizeAppId(app.id),
    userId: normalizeAppId(app.userId),
    user: normalizeCreator(app.user),
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

const withPageNum = <T extends { pageNum?: number; pageNumber?: number }>(params: T) => {
  const { pageNumber, ...restParams } = params
  return {
    ...restParams,
    pageNum: pageNumber ?? restParams.pageNum,
  }
}

export const listMyApps = async (params: AppPageParams = {}) => {
  const res = await listMyAppVoByPage(withPageNum(params))
  return {
    ...res,
    data: {
      ...res.data,
      data: normalizePage(res.data.data),
    },
  }
}

export const listGoodApps = async (params: AppPageParams = {}) => {
  const res = await listGoodAppVoByPage(withPageNum(params))
  return {
    ...res,
    data: {
      ...res.data,
      data: normalizePage(res.data.data),
    },
  }
}

export const listAdminApps = async (params: AdminAppPageParams = {}) => {
  const { userId, ...restParams } = withPageNum(params)
  const res = await listAppVoByPageByAdmin({
    ...restParams,
    userId: userId as unknown as number | undefined,
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

export const getAdminAppDetail = async (id: AppId) =>
  getAppVoByIdByAdmin({ id: id as unknown as number })

export const deployMyApp = (appId: AppId) => deployApp({ appId: appId as unknown as number })

export const updateMyApp = (body: { id: AppId; appName?: string }) =>
  updateApp({
    id: body.id as unknown as number,
    appName: body.appName,
  })

export const deleteMyApp = (id: AppId) => deleteApp({ id: id as unknown as number })

export const updateAdminApp = (body: {
  id: AppId
  appName?: string
  cover?: string
  priority?: number
}) =>
  updateAppByAdmin({
    id: body.id as unknown as number,
    appName: body.appName,
    cover: body.cover,
    priority: body.priority,
  })

export const deleteAdminApp = (id: AppId) => deleteAppByAdmin({ id: id as unknown as number })

export const getAppCover = (app?: Pick<AppVO, 'cover'>) => app?.cover || DEFAULT_APP_COVER

export const getAppCreatorName = (app?: Pick<AppVO, 'user' | 'userId'>) =>
  app?.user?.userName || app?.user?.userAccount || (app?.userId ? '用户 ' + app.userId : '未知用户')

export const getAppCreatorInitial = (app?: Pick<AppVO, 'user' | 'userId'>) =>
  getAppCreatorName(app).slice(0, 1).toUpperCase()

export const getGeneratedPreviewUrl = (appId?: AppId, codeGenType = DEFAULT_CODE_GEN_TYPE) => {
  if (!appId) {
    return ''
  }
  return API_BASE_URL + '/static/' + codeGenType + '_' + appId + '/'
}

export const chatToGenerateApp = async (
  body: API.AppCreateRequest,
  handlers: ChatStreamHandlers,
) => {
  const response = await fetch(API_BASE_URL + '/app/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(body),
  })

  if (!response.ok || !response.body) {
    throw new Error('生成请求失败：' + response.status)
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
