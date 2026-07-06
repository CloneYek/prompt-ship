// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /chatHistory/list */
export async function listChatHistory(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listChatHistoryParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCursorPageChatHistoryVO>('/chatHistory/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
