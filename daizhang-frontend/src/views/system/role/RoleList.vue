<template>
  <div class="role-list-container">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>新增角色
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="roleCode" label="角色编码" width="160" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleAssignMenus(row)">分配菜单</el-button>
            <el-popconfirm
              title="确定要删除该角色吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单对话框 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单" width="500px" destroy-on-close>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ children: 'children', label: 'name' }"
        node-key="id"
        show-checkbox
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuSubmitLoading" @click="handleMenuSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type ElTree from 'element-plus/es/components/tree'
import { roleApi, menuApi } from '@/api/system'
import type { SysRoleVO, SysMenuVO } from '@/types/system'

const loading = ref(false)
const submitLoading = ref(false)
const menuSubmitLoading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const tableData = ref<SysRoleVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const isEdit = ref(false)
const editId = ref(0)
const formRef = ref<FormInstance>()
const form = reactive({
  roleName: '',
  roleCode: '',
  description: '',
  status: 1
})
const formRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

// 分配菜单
const menuDialogVisible = ref(false)
const menuTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
const menuTree = ref<SysMenuVO[]>([])
const currentRoleId = ref(0)
// 分配菜单对话框关闭后可能仍有未触发的 setTimeout,组件卸载时需清理
let menuTreeTimer: ReturnType<typeof setTimeout> | null = null

async function loadData() {
  loading.value = true
  try {
    const res = await roleApi.page({ pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.data.list
    total.value = res.data.total
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  Object.assign(form, { roleName: '', roleCode: '', description: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row: SysRoleVO) {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑角色'
  Object.assign(form, {
    roleName: row.roleName,
    roleCode: row.roleCode,
    description: row.description,
    status: row.status
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await roleApi.update(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await roleApi.create(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: SysRoleVO) {
  try {
    await roleApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handleAssignMenus(row: SysRoleVO) {
  currentRoleId.value = row.id
  try {
    const [treeRes, menuIdsRes] = await Promise.all([
      menuApi.tree(),
      roleApi.getMenuIds(row.id)
    ])
    menuTree.value = treeRes.data
    const checkedMenuIds = menuIdsRes.data
    menuDialogVisible.value = true
    // 清理旧的 timer,避免快速重复打开导致多个 setTimeout 堆积
    if (menuTreeTimer) {
      clearTimeout(menuTreeTimer)
      menuTreeTimer = null
    }
    menuTreeTimer = setTimeout(() => {
      menuTreeRef.value?.setCheckedKeys(checkedMenuIds)
      menuTreeTimer = null
    }, 100)
  } catch {
    // handled by interceptor
  }
}

async function handleMenuSubmit() {
  menuSubmitLoading.value = true
  try {
    const checkedKeys = menuTreeRef.value?.getCheckedKeys() || []
    const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
    const menuIds = [...checkedKeys, ...halfCheckedKeys] as number[]
    await roleApi.assignMenus(currentRoleId.value, menuIds)
    ElMessage.success('分配成功')
    menuDialogVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    menuSubmitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})

onBeforeUnmount(() => {
  // 清理可能挂起的 timer,防止组件卸载后回调执行报错
  if (menuTreeTimer) {
    clearTimeout(menuTreeTimer)
    menuTreeTimer = null
  }
})
</script>

<style scoped lang="scss">
.role-list-container {
  padding: 20px;
}

.table-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
