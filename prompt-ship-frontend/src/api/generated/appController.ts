// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 DELETE /app/admin/delete/${param0} */
export async function deleteAppByAdmin(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteAppByAdminParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/app/admin/delete/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/admin/get/${param0} */
export async function getAppVoByIdByAdmin(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAppVOByIdByAdminParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseApp>(`/app/admin/get/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/admin/list */
export async function listAppVoByPageByAdmin(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listAppVOByPageByAdminParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppVO>('/app/admin/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 PUT /app/admin/update */
export async function updateAppByAdmin(
  body: API.AppUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppVO>('/app/admin/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/chat */
export async function chatToGenCode(body: API.AppCreateRequest, options?: { [key: string]: any }) {
  return request<API.SseEmitter>('/app/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/chat/continue */
export async function chatContinue(
  body: API.AppChatContinueRequest,
  options?: { [key: string]: any }
) {
  return request<API.SseEmitter>('/app/chat/continue', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/create */
export async function createApp(body: API.AppCreateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/app/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 DELETE /app/delete/${param0} */
export async function deleteApp(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteAppParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/app/delete/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/deploy */
export async function deployApp(body: API.AppDeployRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/app/deploy', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/get/vo/${param0} */
export async function getAppVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAppVOByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseAppVO>(`/app/get/vo/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/list/good */
export async function listGoodAppVoByPage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listGoodAppVOByPageParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppVO>('/app/list/good', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/list/my */
export async function listMyAppVoByPage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMyAppVOByPageParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppVO>('/app/list/my', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 PUT /app/update */
export async function updateApp(body: API.AppUpdateMyRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseAppVO>('/app/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
