import {
  create,
  getLogin,
  login,
  logout,
  page,
  register,
  remove,
  update,
  updateMy,
} from '@/api/generated/userController'

export type UserPageParams = Omit<API.pageParams, 'pageNum'> & {
  pageNum?: number
  pageNumber?: number
}

export const userLogin = login
export const userRegister = register
export const getLoginUser = getLogin
export const userLogout = logout
export const createUser = create
export const updateUser = update
export const updateMyUser = updateMy

// 业务页面使用 pageNumber，更贴近表格分页；生成接口实际需要 pageNum。
export const listUsers = ({ pageNumber, ...params }: UserPageParams = {}) =>
  page({
    ...params,
    pageNum: pageNumber ?? params.pageNum,
  })

// 生成接口的删除参数是对象，这里包装成页面更好理解的 removeUser(id)。
export const removeUser = (id: number) => remove({ id })