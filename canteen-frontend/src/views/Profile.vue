<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 左：头像 + 导航（美团式窄栏） -->
      <el-col :span="6">
        <el-card shadow="never" class="page-card side-card">
          <div class="avatar-wrap">
            <el-upload :show-file-list="false" :http-request="doUploadAvatar" accept="image/*">
              <div class="avatar-box">
                <img v-if="form.avatar" :src="form.avatar" class="avatar-img" />
                <div v-else class="avatar-fallback">{{ avatarText }}</div>
                <div class="avatar-mask">更换头像</div>
              </div>
            </el-upload>
            <div class="user-name">{{ userStore.nickname }}</div>
            <el-tag :type="roleTagType" size="small" effect="light" round>{{ form.roleLabel }}</el-tag>
          </div>

          <div class="side-menu">
            <div
              v-for="m in sideMenus"
              :key="m.key"
              class="side-item"
              :class="{ active: activeMenu === m.key }"
              @click="activeMenu = m.key"
            >
              {{ m.label }}
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右：内容区 -->
      <el-col :span="18">
        <!-- 基本资料 -->
        <el-card v-show="activeMenu === 'base'" shadow="never" class="page-card">
          <template #header><span style="font-weight: 600">基本资料</span></template>
          <el-form label-position="top" style="max-width: 460px">
            <el-form-item label="姓名">
              <el-input v-model="form.realName" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item v-if="!isMerchant" label="绑定邮箱">
              <el-input v-model="form.email" maxlength="100" placeholder="用于接收食堂通知与安全验证" />
            </el-form-item>
            <el-form-item label="绑定电话">
              <el-input v-model="form.phone" maxlength="20" placeholder="手机号或座机" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 账号安全 -->
        <el-card v-show="activeMenu === 'security'" shadow="never" class="page-card">
          <template #header><span style="font-weight: 600">账号安全</span></template>
          <div class="sec-item">
            <div class="sec-info">
              <div class="sec-name">登录密码</div>
              <div class="sec-desc">定期更换密码可以让账号更安全</div>
            </div>
            <el-button type="primary" plain @click="openPwdDialog">修改密码</el-button>
          </div>
          <div class="sec-item">
            <div class="sec-info">
              <div class="sec-name">绑定邮箱</div>
              <div class="sec-desc">{{ form.email ? '已绑定：' + form.email : '未绑定，绑定后可用于安全验证' }}</div>
            </div>
            <el-tag :type="form.email ? 'success' : 'info'" size="small" effect="plain">
              {{ form.email ? '已绑定' : '未绑定' }}
            </el-tag>
          </div>
          <div class="sec-item">
            <div class="sec-info">
              <div class="sec-name">绑定电话</div>
              <div class="sec-desc">{{ form.phone ? '已绑定：' + form.phone : '未绑定，绑定后可用于安全验证' }}</div>
            </div>
            <el-tag :type="form.phone ? 'success' : 'info'" size="small" effect="plain">
              {{ form.phone ? '已绑定' : '未绑定' }}
            </el-tag>
          </div>
        </el-card>

        <!-- 我的证照（商家） -->
        <el-card v-show="activeMenu === 'cert'" shadow="never" class="page-card">
          <template #header><span style="font-weight: 600">我的证照</span></template>
          <el-row :gutter="24">
            <el-col :span="12">
              <div class="cert-title">健康证</div>
              <el-upload :show-file-list="false" :http-request="opt => doUploadCert(opt, 'health')" accept="image/*">
                <div class="cert-box">
                  <img v-if="cert.healthCertUrl" :src="cert.healthCertUrl" class="cert-img" />
                  <div v-else class="cert-empty"><span style="font-size: 26px">＋</span><span>上传健康证</span></div>
                  <div class="cert-mask">点击更换</div>
                </div>
              </el-upload>
            </el-col>
            <el-col :span="12">
              <div class="cert-title">营业执照</div>
              <el-upload :show-file-list="false" :http-request="opt => doUploadCert(opt, 'license')" accept="image/*">
                <div class="cert-box">
                  <img v-if="cert.licenseUrl" :src="cert.licenseUrl" class="cert-img" />
                  <div v-else class="cert-empty"><span style="font-size: 26px">＋</span><span>上传营业执照</span></div>
                  <div class="cert-mask">点击更换</div>
                </div>
              </el-upload>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="440px">
      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码（6-20位）">
          <el-input v-model="pwd.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwd.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item :label="`验证码（发送至 ${verifyTarget || '绑定的邮箱/电话'}）`">
          <div style="display: flex; gap: 10px; width: 100%">
            <el-input v-model="pwd.verifyCode" maxlength="6" placeholder="6 位验证码" />
            <el-button :disabled="countdown > 0" :loading="sending" style="width: 120px" @click="sendCode">
              {{ countdown > 0 ? countdown + 's 后重发' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPwd" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateProfile, changePassword as changePwdApi } from '@/api/auth'
import { uploadImage } from '@/api/sales'
import request from '@/api/request'

const userStore = useUserStore()
const isMerchant = computed(() => userStore.isMerchant)
const roleTagType = computed(() => (isMerchant.value ? 'warning' : userStore.isAdmin ? 'danger' : 'success'))
const avatarText = computed(() => (userStore.nickname || 'U').trim().slice(0, 1).toUpperCase())

const activeMenu = ref('base')
const sideMenus = computed(() => {
  const menus = [
    { key: 'base', label: '基本资料' },
    { key: 'security', label: '账号安全' }
  ]
  if (isMerchant.value) menus.push({ key: 'cert', label: '我的证照' })
  return menus
})

const saving = ref(false)
const changingPwd = ref(false)
const form = reactive({
  avatar: '',
  realName: '',
  email: '',
  phone: '',
  roleLabel: ''
})
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '', verifyCode: '' })
const cert = reactive({ healthCertUrl: '', licenseUrl: '' })
const pwdVisible = ref(false)
const countdown = ref(0)
const sending = ref(false)
const verifyTarget = computed(() => form.email || form.phone || '')

function fillForm(profile) {
  Object.assign(form, {
    avatar: profile.avatar || '',
    realName: profile.realName || '',
    email: profile.email || '',
    phone: profile.phone || '',
    roleLabel: profile.roleLabel || ''
  })
}

async function loadProfile() {
  const res = await request.get('/auth/profile')
  fillForm(res.data)
  userStore.userInfo = res.data
  localStorage.setItem('canteen_user', JSON.stringify(res.data))
}

async function loadCert() {
  if (!isMerchant.value) return
  const res = await request.get('/merchant/certificates')
  cert.healthCertUrl = res.data.healthCertUrl || ''
  cert.licenseUrl = res.data.licenseUrl || ''
}

async function doUploadAvatar(option) {
  const res = await uploadImage(option.file)
  form.avatar = res.data
  await updateProfile({ avatar: form.avatar })
  ElMessage.success('头像已更新')
  loadProfile()
}

async function saveProfile() {
  if (!form.realName.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  saving.value = true
  try {
    const body = { realName: form.realName.trim(), phone: form.phone }
    if (!isMerchant.value) body.email = form.email
    const res = await updateProfile(body)
    fillForm(res.data)
    userStore.userInfo = res.data
    localStorage.setItem('canteen_user', JSON.stringify(res.data))
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

function openPwdDialog() {
  pwd.oldPassword = pwd.newPassword = pwd.confirm = pwd.verifyCode = ''
  pwdVisible.value = true
}

async function sendCode() {
  if (!verifyTarget.value) {
    ElMessage.warning('请先在「基本资料」里绑定邮箱或电话')
    return
  }
  sending.value = true
  try {
    const res = await request.post('/auth/verify-code', { target: verifyTarget.value })
    // 演示模式：短信/邮件服务未接入，验证码直接返回
    ElMessage.success('验证码已发送（演示模式）：' + res.data)
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function savePassword() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (pwd.newPassword.length < 6 || pwd.newPassword.length > 20) {
    ElMessage.warning('新密码长度为 6-20 位')
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (!pwd.verifyCode.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  changingPwd.value = true
  try {
    await changePwdApi({
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword,
      verifyCode: pwd.verifyCode.trim()
    })
    ElMessage.success('密码已修改')
    pwdVisible.value = false
  } finally {
    changingPwd.value = false
  }
}

async function doUploadCert(option, type) {
  const res = await uploadImage(option.file)
  if (type === 'health') cert.healthCertUrl = res.data
  else cert.licenseUrl = res.data
  await request.put('/merchant/certificates', {
    healthCertUrl: cert.healthCertUrl,
    licenseUrl: cert.licenseUrl
  })
  ElMessage.success('证照已更新')
}

onMounted(() => {
  loadProfile()
  loadCert()
})
</script>

<style scoped>
.side-card :deep(.el-card__body) {
  padding: 20px 16px;
}

.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.avatar-box {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
}

.avatar-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  font-size: 11px;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  text-align: center;
  padding: 3px 0;
}

.user-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
}

.side-menu {
  padding-top: 8px;
}

.side-item {
  padding: 11px 14px;
  border-radius: 10px;
  font-size: 14px;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.18s ease;
}

.side-item:hover {
  background: #fff4ec;
  color: var(--brand-dark);
}

.side-item.active {
  background: linear-gradient(90deg, #fff1e6 0%, #fff9f5 100%);
  color: var(--brand-dark);
  font-weight: 600;
}

.sec-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 4px;
  border-bottom: 1px dashed var(--border);
}

.sec-item:last-child {
  border-bottom: none;
}

.sec-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.sec-desc {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}

.cert-title {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}

.cert-box {
  position: relative;
  width: 100%;
  height: 140px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border);
}

.cert-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cert-empty {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--text-3);
  border: 1px dashed var(--border);
}

.cert-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  text-align: center;
  padding: 4px 0;
}
</style>
