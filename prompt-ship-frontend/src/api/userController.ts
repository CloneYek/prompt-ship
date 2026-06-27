// @ts-ignore
/* eslint-disable */
import request from '@/request'

export async function userLogin(body: API.UserLoginRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/login', {
    method: 'POST',
    data: body,
    ...(options || {}),
  })
}

export async function userRegister(body: API.UserRegisterRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/user/register', {
    method: 'POST',
    data: body,
    ...(options || {}),
  })
}

export async function getLoginUser(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/get/login', {
    method: 'GET',
    ...(options || {}),
  })
}

export async function userLogout(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/logout', {
    method: 'POST',
    ...(options || {}),
  })
}

export async function listUsers(params: API.UserPageRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageLoginUserVO>('/user/page', {
    method: 'GET',
    params,
    ...(options || {}),
  })
}

export async function createUser(body: API.UserCreateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/user/create', {
    method: 'POST',
    data: body,
    ...(options || {}),
  })
}

export async function updateUser(body: API.UserUpdateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/update', {
    method: 'PUT',
    data: body,
    ...(options || {}),
  })
}

export async function updateMyUser(body: API.UserUpdateMyRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/update/my', {
    method: 'PUT',
    data: body,
    ...(options || {}),
  })
}

export async function removeUser(id: number, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>(`/user/remove/${id}`, {
    method: 'DELETE',
    ...(options || {}),
  })
}
