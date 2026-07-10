declare namespace API {
  type App = {
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: number
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type AppChatContinueRequest = {
    appId: number
    message: string
  }

  type AppCreateRequest = {
    appName?: string
    initPrompt: string
  }

  type AppDeployRequest = {
    appId?: number
  }

  type AppUpdateMyRequest = {
    id: number
    appName?: string
  }

  type AppUpdateRequest = {
    id: number
    appName?: string
    cover?: string
    priority?: number
  }

  type AppVO = {
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: number
    user?: LoginUserVO
    createTime?: string
    updateTime?: string
  }

  type BaseResponseApp = {
    code?: number
    data?: App
    message?: string
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseCursorPageChatHistoryVO = {
    code?: number
    data?: CursorPageChatHistoryVO
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageLoginUserVO = {
    code?: number
    data?: PageLoginUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type ChatHistoryVO = {
    id?: number
    role?: string
    content?: string
    appId?: number
    userId?: number
    createTime?: string
  }

  type CursorPageChatHistoryVO = {
    records?: ChatHistoryVO[]
    nextCursor?: string
    hasMore?: boolean
    pageSize?: number
  }

  type deleteAppByAdminParams = {
    id: number
  }

  type deleteAppParams = {
    id: number
  }

  type downloadAppProjectParams = {
    appId: number
  }

  type getAppVOByIdByAdminParams = {
    id: number
  }

  type getAppVOByIdParams = {
    id: number
  }

  type getByIdParams = {
    id: number
  }

  type getVOByIdParams = {
    id: number
  }

  type listAppVOByPageByAdminParams = {
    appName?: string
    userId?: number
    codeGenType?: string
    priority?: number
    deployKey?: string
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type listChatHistoryParams = {
    appId: number
    cursor?: string
    pageSize?: number
  }

  type listGoodAppVOByPageParams = {
    appName?: string
    userId?: number
    codeGenType?: string
    priority?: number
    deployKey?: string
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type listMyAppVOByPageParams = {
    appName?: string
    userId?: number
    codeGenType?: string
    priority?: number
    deployKey?: string
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    updateTime?: string
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageLoginUserVO = {
    records?: LoginUserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type pageParams = {
    userName?: string
    userRole?: string
    userAccount?: string
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type removeParams = {
    id: number
  }

  type serveStaticResourceParams = {
    deployKey: string
  }

  type SseEmitter = {
    timeout?: number
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserCreateRequest = {
    userAccount: string
    userPassword: string
    userName: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount: string
    userPassword: string
  }

  type UserRegisterRequest = {
    userAccount: string
    userPassword: string
    checkPassword: string
  }

  type UserUpdateMyRequest = {
    userName?: string
    userAvatar?: string
    userProfile?: string
  }

  type UserUpdateRequest = {
    id: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }
}
