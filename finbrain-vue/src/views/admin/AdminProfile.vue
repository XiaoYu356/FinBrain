<template>
  <div class="admin-profile">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <div class="user-info">
            <el-avatar :size="80" icon="User" />
            <h3>{{ userStore.userInfo?.username }}</h3>
            <el-tag type="danger">管理员</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>个人信息</span>
          </template>
          <el-form :model="form" label-width="100px">
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdate" :loading="updateLoading">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>安全设置</span>
          </template>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { changePassword, updateProfile } from '@/api/auth'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const updateLoading = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref()

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleUpdate = async () => {
  updateLoading.value = true
  try {
    await updateProfile({
      realName: form.realName,
      phone: form.phone,
      email: form.email
    })
    ElMessage.success('信息更新成功')
    userStore.fetchUserInfo()
  } catch (e) {
    console.error(e)
  } finally {
    updateLoading.value = false
  }
}

const handleChangePassword = async () => {
  await passwordFormRef.value.validate()
  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    userStore.logout()
  } catch (e) {
    console.error(e)
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => {
  if (userStore.userInfo) {
    Object.assign(form, userStore.userInfo)
  }
})
</script>

<style scoped>
.user-info {
  text-align: center;
  padding: 20px 0;
}

.user-info h3 {
  margin: 15px 0 10px;
  color: #303133;
}
</style>
