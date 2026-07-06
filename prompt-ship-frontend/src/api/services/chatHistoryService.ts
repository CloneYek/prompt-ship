import { listChatHistory } from '@/api/generated/chatHistoryController'
import type { AppId } from '@/api/services/appService'

export type ChatRole = 'user' | 'assistant'

export type ChatHistoryRecord = Omit<API.ChatHistoryVO, 'id' | 'appId' | 'userId' | 'role'> & {
  id?: string
  appId?: AppId
  userId?: AppId
  role?: ChatRole
}

export type ChatHistoryCursorPage = Omit<API.CursorPageChatHistoryVO, 'records'> & {
  records: ChatHistoryRecord[]
}

export type ChatHistoryPageParams = {
  appId: AppId
  cursor?: string
  pageSize?: number
}

const normalizeId = (id?: string | number) => (id == null ? undefined : String(id))
const normalizeRole = (role?: string): ChatRole | undefined =>
  role === 'user' || role === 'assistant' ? role : undefined

const normalizeRecord = (record: API.ChatHistoryVO): ChatHistoryRecord => ({
  ...record,
  id: normalizeId(record.id),
  appId: normalizeId(record.appId),
  userId: normalizeId(record.userId),
  role: normalizeRole(record.role),
})

export const listAppChatHistory = async ({
  appId,
  cursor,
  pageSize = 20,
}: ChatHistoryPageParams) => {
  const res = await listChatHistory({
    appId: appId as unknown as number,
    cursor,
    pageSize,
  })

  return {
    ...res,
    data: {
      ...res.data,
      data: {
        ...res.data.data,
        records: res.data.data?.records?.map(normalizeRecord) ?? [],
      } as ChatHistoryCursorPage,
    },
  }
}
