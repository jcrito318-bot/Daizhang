<template>
  <div class="subject-container">
    <el-card class="search-card">
      <el-form inline>
        <el-form-item label="账套">
          <el-select
            v-model="accountSetId"
            placeholder="请选择账套"
            style="width: 240px"
            @change="loadTree"
          >
            <el-option
              v-for="item in appStore.accountSetList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleCreate(null)">新增一级科目</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="tree-card">
      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="treeProps"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span class="node-code">{{ data.subjectCode }}</span>
            <span class="node-name">{{ data.subjectName }}</span>
            <el-tag size="small" :type="data.balanceDirection === 1 ? 'success' : 'warning'" class="node-tag">
              {{ data.balanceDirection === 1 ? '借' : '贷' }}
            </el-tag>
            <el-tag size="small" type="info" class="node-tag">{{ data.category }}</el-tag>
            <span class="node-actions">
              <el-button link type="primary" size="small" @click.stop="handleCreate(data)">新增下级</el-button>
              <el-button link type="primary" size="small" @click.stop="handleEdit(data)">编辑</el-button>
              <el-button link type="danger" size="small" @click.stop="handleDelete(data)">删除</el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="科目编码" prop="subjectCode">
          <el-input v-model="form.subjectCode" placeholder="请输入科目编码" />
        </el-form-item>
        <el-form-item label="科目名称" prop="subjectName">
          <el-input v-model="form.subjectName" placeholder="请输入科目名称" />
        </el-form-item>
        <el-form-item label="科目类别" prop="category">
          <el-select v-model="form.category" placeholder="请选择科目类别" style="width: 100%">
            <el-option label="资产类" value="资产类" />
            <el-option label="负债类" value="负债类" />
            <el-option label="共同类" value="共同类" />
            <el-option label="所有者权益类" value="所有者权益类" />
            <el-option label="成本类" value="成本类" />
            <el-option label="损益类" value="损益类" />
          </el-select>
        </el-form-item>
        <el-form-item label="余额方向" prop="balanceDirection">
          <el-radio-group v-model="form.balanceDirection">
            <el-radio :value="1">借方</el-radio>
            <el-radio :value="2">贷方</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="辅助核算">
          <el-switch v-model="form.auxiliaryAccounting" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="现金科目">
          <el-switch v-model="form.isCash" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="银行科目">
          <el-switch v-model="form.isBank" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { subjectApi } from '@/api/subject'
import { useAppStore } from '@/stores/app'
import type { SubjectVO, SubjectCreateRequest } from '@/types/subject'

const appStore = useAppStore()
const treeRef = ref()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const dialogTitle = ref('新增科目')
const accountSetId = ref<number>(0)
const treeData = ref<SubjectVO[]>([])

const treeProps = {
  children: 'children',
  label: 'name'
}

const form = reactive<SubjectCreateRequest>({
  accountSetId: 0,
  subjectCode: '',
  subjectName: '',
  category: '',
  parentId: 0,
  level: 1,
  balanceDirection: 1,
  auxiliaryAccounting: 0,
  isCash: 0,
  isBank: 0,
  isCurrent: 0
})

const formRules: FormRules = {
  subjectCode: [{ required: true, message: '请输入科目编码', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择科目类别', trigger: 'change' }],
  balanceDirection: [{ required: true, message: '请选择余额方向', trigger: 'change' }]
}

async function loadTree() {
  if (!accountSetId.value) return
  try {
    const res = await subjectApi.getTree(accountSetId.value)
    treeData.value = res.data
  } catch {
    // handled by interceptor
  }
}

function handleCreate(parentData: SubjectVO | null) {
  isEdit.value = false
  dialogTitle.value = parentData ? '新增下级科目' : '新增一级科目'
  Object.assign(form, {
    accountSetId: accountSetId.value,
    subjectCode: parentData ? parentData.subjectCode + '-' : '',
    subjectName: '',
    category: parentData?.category || '',
    parentId: parentData?.id || 0,
    level: parentData ? parentData.level + 1 : 1,
    balanceDirection: parentData?.balanceDirection || 1,
    auxiliaryAccounting: 0,
    isCash: 0,
    isBank: 0,
    isCurrent: 0
  })
  dialogVisible.value = true
}

function handleEdit(data: SubjectVO) {
  isEdit.value = true
  editId.value = data.id
  dialogTitle.value = '编辑科目'
  Object.assign(form, {
    accountSetId: data.accountSetId,
    subjectCode: data.subjectCode,
    subjectName: data.subjectName,
    category: data.category,
    parentId: data.parentId,
    level: data.level,
    balanceDirection: data.balanceDirection,
    auxiliaryAccounting: data.auxiliaryAccounting,
    isCash: data.isCash,
    isBank: data.isBank,
    isCurrent: data.isCurrent
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await subjectApi.update(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await subjectApi.create(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTree()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(data: SubjectVO) {
  if (data.children && data.children.length > 0) {
    ElMessage.warning('该科目存在下级科目,不能删除')
    return
  }
  await ElMessageBox.confirm(`确定要删除科目"${data.subjectName}"吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await subjectApi.delete(data.id)
    ElMessage.success('删除成功')
    loadTree()
  } catch {
    // handled by interceptor
  }
}

onMounted(async () => {
  try {
    const list = await appStore.loadAccountSetList()
    if (list.length > 0) {
      accountSetId.value = appStore.currentAccountSetId || list[0].id
      loadTree()
    }
  } catch {
    // handled by interceptor
  }
})
</script>

<style scoped lang="scss">
.subject-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.tree-card {
  .tree-node {
    display: flex;
    align-items: center;
    flex: 1;
    font-size: 14px;

    .node-code {
      margin-right: 8px;
      color: #409eff;
      font-weight: 500;
    }

    .node-name {
      margin-right: 8px;
    }

    .node-tag {
      margin-right: 4px;
    }

    .node-actions {
      margin-left: auto;
    }
  }
}
</style>
