export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userInfo: UserVO
}

export interface UserVO {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  avatar: string
  status: number
  roles: string[]
  permissions: string[]
  menus: MenuVO[]
}

export interface MenuVO {
  id: number
  parentId: number
  name: string
  path: string
  component: string
  icon: string
  sortOrder: number
  menuType: number
  permission: string
  visible: number
  status: number
  children: MenuVO[]
}
