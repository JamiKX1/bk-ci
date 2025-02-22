<script setup lang="ts">
import {
  ref,
  watch,
  onBeforeUnmount,
  computed,
  onMounted,
  getCurrentInstance,
} from 'vue';
import {
  EditLine,
} from 'bkui-vue/lib/icon';
import IAMIframe from './IAM-Iframe';
import { useI18n } from 'vue-i18n';
import { Message, Popover } from 'bkui-vue';
import http from '@/http/api';
import DialectPopoverTable from "@/components/dialectPopoverTable.vue";
const {
  t,
} = useI18n();
const emits = defineEmits(['change', 'approvedChange', 'initProjectForm']);

const props = defineProps({
  data: Object,
  type: String,
  isChange: Boolean,
});

const logoFiles = computed(() => {
  const { logoAddr } = projectData.value;
  const files = [];
  if (logoAddr) {
    files.push({
      url: logoAddr,
    });
  }
  return files;
});
const isRbac = computed(() => {
  return authProvider.value === 'rbac'
})
const authProvider = ref(window.top.BK_CI_AUTH_PROVIDER || '')
const projectForm = ref(null);
const iframeRef = ref(null);
const vm = getCurrentInstance();
const rules = {
  englishName: [
    {
      validator: value => /^[a-z][a-z0-9\-]{1,32}$/.test(value),
      message: t('项目ID必须由小写字母+数字+中划线组成，以小写字母开头，长度限制32字符！'),
      trigger: 'blur',
    },
  ],
  bgId: [
    {
      validator: () => projectData.value.bgId && projectData.value.deptId,
      message: t('请选择项目所属组织'),
      trigger: 'blur',
    },
  ],
  subjectScopes: [
    {
      validator: () => projectData.value.subjectScopes.length > 0,
      message: t('请选择项目项目最大可授权人员范围'),
      trigger: 'change',
    },
  ],
};

const projectTypeList = [
  {
    id: 1,
    name: t('手游'),
  },
  {
    id: 2,
    name: t('端游'),
  },
  {
    id: 3,
    name: t('页游'),
  },
  {
    id: 4,
    name: t('平台产品'),
  },
  {
    id: 5,
    name: t('支撑产品'),
  },
];

const projectData = ref<any>(props.data);

const deptLoading = ref({
  bg: false,
  dept: false,
  center: false,
});

const curDepartmentInfo = ref({
  bg: [],
  dept: [],
  center: [],
});

const showDialog = ref(false);

const getDepartment = async (type: string, id: any) => {
  deptLoading.value[type] = true;
  try {
    const res = await http.getOrganizations({
      type,
      id,
    });
    curDepartmentInfo.value[type] = [...res];
  } catch (err: any) {
    Message({
      message: err.message || err,
      theme: 'danger',
    });
    curDepartmentInfo.value[type] = [];
  } finally {
    deptLoading.value[type] = false;
  }
};

/**
 * 根据 type id 设置组织名称
 * @param {*} type 类型
 * @param {*} id 类型ID
 */
const setOrgName = (type: string, id: any) => {
  const item = curDepartmentInfo.value[type].find((item: { id: any; }) => item.id === id);
  if (item) {
    projectData.value[`${type}Name`] = item.name;
  }
};

const handleChangeBg = (type: string, id: any) => {
  handleChangeForm();
  projectData.value.deptId = '';
  projectData.value.deptName = '';
  projectData.value.centerId = '';
  projectData.value.centerName = '';
  curDepartmentInfo.value.dept = [];
  curDepartmentInfo.value.center = [];
  if (id) {
    setOrgName(type, id);
    getDepartment('dept', id);
  }
};

const handleChangeDept = (type: string, id: any) => {
  handleChangeForm();
  projectData.value.centerId = '';
  projectData.value.centerName = '';
  curDepartmentInfo.value.center = [];
  if (id) {
    setOrgName(type, id);
    getDepartment('center', id);
  }
};

const handleChangeCenter = (type: string, id: any) => {
  handleChangeForm();
  if (id) {
    setOrgName(type, id);
  };
};

const fetchDepartmentList = async () => {
  const { bgId, deptId, centerId } = projectData.value;
  await getDepartment('bg', 0);
  if (bgId) {
    await getDepartment('dept', bgId);
  }
  if (deptId) {
    await getDepartment('center', deptId);
  }
};

const handleChangeForm = () => {
  emits('change', true);
};

const handleUploadLogo = async (res: any) => {
  handleChangeForm();
  const { file } = res;
  if (file) {
    if (!(file.type === 'image/jpeg' || file.type === 'image/png')) {
      Message({
        theme: 'danger',
        message: t('只允许上传png、jpg'),
      });
    } else if (file.size > (2 * 1024 * 1024)) {
      Message({
        theme: 'danger',
        message: t('大小不超过2M'),
      });
    } else {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => {
        projectData.value.logoAddr = reader.result;
      };
    }
    const formData = new FormData();
    formData.append('logo', file);
    await http.uploadProjectLogo({
      formData,
    }).then((res) => {
      projectData.value.logoAddress = res;
    });
  }
};

const handleMessage = (event: any) => {
  const { data } = event;
  if (data.type === 'IAM') {
    switch (data.code) {
      case 'success':
        handleChangeForm();
        projectData.value.subjectScopes = data.data.subject_scopes;
        showDialog.value = false;
        break;
      case 'cancel':
        showDialog.value = false;
        break;
      case 'load':
        setTimeout(() => {
          // 回显数据
          vm?.refs?.iframeRef?.$el?.firstElementChild?.contentWindow?.postMessage?.(
            JSON.parse(JSON.stringify({
              subject_scopes: projectData.value.subjectScopes,
            })),
            window.BK_IAM_URL_PREFIX,
          );
        }, 0);
        break;
    }
  }
};

const fetchUserDetail = async () => {
  if (props.type !== 'apply') return;
  await http.getUserDetail().then((res) => {
    const { bgId, centerId, deptId } = res;
    projectData.value.bgId = bgId;
    projectData.value.centerId = centerId === '0' ? '' : centerId;
    projectData.value.deptId = deptId;
  });
};

const showMemberDialog = () => {
  showDialog.value = true;
};

const validateProjectNameTips = ref('');
watch(() => projectData.value.projectName, (val) => {
  if (props.type === 'apply' && val) {
    http.validateProjectName(val)
      .then(() => {
        validateProjectNameTips.value = '';
      })
      .catch(() => {
        projectForm.value.clearValidate();
        validateProjectNameTips.value = t('项目名称已存在');
      });
  } else if (!val) {
    validateProjectNameTips.value = '';
  }
}, {
  deep: true,
});

const validateEnglishNameTips = ref('');
watch(() => projectData.value.englishName, (val) => {
  if (props.type === 'apply' && val && /^[a-z][a-z0-9\-]{1,32}$/.test(val)) {
    http.validateEnglishName(val)
      .then(() => {
        validateEnglishNameTips.value = '';
      })
      .catch(() => {
        projectForm.value.clearValidate();
        validateEnglishNameTips.value = t('项目ID已存在');
      });
  } else if (!val || !/^[a-z][a-z0-9\-]{1,32}$/.test(val)) {
    validateEnglishNameTips.value = '';
  }
}, {
  deep: true,
});

watch(() => [projectData.value.authSecrecy, projectData.value.projectType, projectData.value.subjectScopes], () => {
  projectForm.value.validate();
  emits('approvedChange', true);
}, {
  deep: true,
});

onMounted(async () => {
  await fetchUserDetail();
  // await fetchDepartmentList();
  emits('initProjectForm', projectForm.value);
  window.addEventListener('message', handleMessage);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage);
});
</script>

<template>
  <bk-form
    ref="projectForm"
    :rules="rules"
    :model="projectData"
    :label-width="216"
  >
    <div class="project-tab advanced">
      <p class="title">{{t('基础信息')}}</p>
      <bk-form-item :label="t('项目名称')" property="projectName" :required="true">
        <bk-input
          v-model="projectData.projectName"
          :placeholder="t('请输入1-32字符的项目名称')"
          :maxlength="32"
          @change="handleChangeForm"
        ></bk-input>
        <div class="error-tips" v-if="validateProjectNameTips">
          {{ validateProjectNameTips }}
        </div>
      </bk-form-item>
      <bk-form-item :label="t('项目ID')" property="englishName" :required="true">
        <bk-input
          v-model="projectData.englishName"
          :disabled="type === 'edit'"
          :maxlength="32"
          :placeholder="t('请输入2-32 字符的项目ID，由小写字母、数字、中划线组成，以小写字母开头。提交后不可修改。')"
        ></bk-input>
        <div class="error-tips" v-if="validateEnglishNameTips">
          {{ validateEnglishNameTips }}
        </div>
      </bk-form-item>
      <bk-form-item :label="t('项目描述')" property="description" :required="true">
        <bk-input
          v-model="projectData.description"
          class="textarea"
          type="textarea"
          :rows="3"
          :maxlength="255"
          :placeholder="t('请输入项目描述')"
          @change="handleChangeForm"
        ></bk-input>
      </bk-form-item>
      <bk-form-item :label="t('项目LOGO')">
        <bk-upload
          theme="picture"
          :files="logoFiles"
          with-credentials
          :multiple="false"
          :custom-request="handleUploadLogo"
        />
        <span class="logo-upload-tip">{{ t('只允许上传png、jpg，大小不超过 2M')}}</span>
      </bk-form-item>
      <!-- <bk-form-item :label="t('项目所属组织')" property="bgId" :required="true">
        <div class="bk-dropdown-box">
          <bk-select
            v-model="projectData.bgId"
            placeholder="BG"
            name="bg"
            :loading="deptLoading.bg"
            filterable
            @change="id => handleChangeBg('bg', id)"
          >
            <bk-option
              v-for="bg in curDepartmentInfo.bg"
              :value="bg.id"
              :key="bg.id"
              :label="bg.name"
            />
          </bk-select>
        </div>
        <div class="bk-dropdown-box">
          <bk-select
            v-model="projectData.deptId"
            :placeholder="t('部门')"
            name="dept"
            :loading="deptLoading.dept"
            filterable
            @change="id => handleChangeDept('dept', id)"
          >
            <bk-option
              v-for="bg in curDepartmentInfo.dept"
              :value="bg.id"
              :key="bg.id"
              :label="bg.name"
            />
          </bk-select>
        </div>
        <div class="bk-dropdown-box">
          <bk-select
            v-model="projectData.centerId"
            :placeholder="t('中心')"
            name="center"
            :loading="deptLoading.center"
            filterable
            @change="id => handleChangeCenter('center', id)"
          >
            <bk-option
              v-for="center in curDepartmentInfo.center"
              :value="center.id"
              :key="center.id"
              :label="center.name"
            />
          </bk-select>
        </div>
      </bk-form-item> -->
      <bk-form-item :label="t('项目类型')" property="projectType" :required="true">
        <bk-select
          v-model="projectData.projectType"
          :placeholder="t('选择项目类型')"
          name="center"
          searchable
          @change="handleChangeForm"
        >
          <bk-option
            v-for="type in projectTypeList"
            :value="type.id"
            :key="type.id"
            :label="type.name"
          />
        </bk-select>
      </bk-form-item>
      <bk-form-item
        v-if="isRbac"
        :label="t('项目性质')"
        property="authSecrecy"
        :required="true"
      >
        <bk-radio-group
          v-model="projectData.authSecrecy"
          @change="handleChangeForm"
        >
          <bk-radio class="mr10" :label="0">
            <Popover :content="t('`项目最大可授权人员范围`内的用户可以主动申请加入项目')">
              <span class="authSecrecy-item">{{ t('私有项目') }}</span>
            </Popover>
          </bk-radio>
          <bk-radio :label="1">
            <Popover :content="t('拥有项目/资源管理权限的成员才可以主动添加用户')">
              <span class="authSecrecy-item">{{ t('保密项目') }}</span>
            </Popover>
          </bk-radio>
        </bk-radio-group>
      </bk-form-item>
    </div>
    <div class="project-tab">
      <p class="title">{{t('高级信息')}}</p>
      <div v-if="isRbac">
        <div class="sub-title">{{ t('权限')  }}</div>
        <bk-form-item
          :label="t('项目最大可授权人员范围')"
          :description="t('该设置表示可以加入项目的成员的最大范围，范围内的用户才可以成功加入项目下的任意用户组')"
          property="subjectScopes"
          :required="true">
          <bk-tag
            v-for="(subjectScope, index) in projectData.subjectScopes"
            :key="index"
          >
            {{ subjectScope.id === '*' ? t('全员') : subjectScope.name }}
          </bk-tag>
          <EditLine
            class="edit-line ml5"
            @click="showMemberDialog"
          />
        </bk-form-item>
      </div>
      <div v-if="projectData.properties">
        <div class="sub-title">{{ t('流水线')  }}</div>
        <bk-form-item
          property="pipelineDialect"
        >
          <template #label>
            <dialect-popover-table />
          </template>
          <bk-radio-group
            v-model="projectData.properties.pipelineDialect"
            @change="handleChangeForm"
          >
            <bk-radio label="CLASSIC">
              <span>{{ t('CLASSIC') }}</span>
            </bk-radio>
            <bk-radio label="CONSTRAINED">
              <span>{{ t('CONSTRAINED') }}</span>
            </bk-radio>
          </bk-radio-group>
        </bk-form-item>
        <bk-form-item
          :label="t('命名规范提示')"
          :description="t('开启后，需填写流水线命名规范提示说明。规范提示说明将展示在「创建流水线」页面进行提示。')"
        >
          <bk-switcher
            v-model="projectData.properties.enablePipelineNameTips"
            size="large"
            theme="primary"
          />
          <bk-input
            class="textarea"
            v-show="projectData.properties.enablePipelineNameTips"
            v-model="projectData.properties.pipelineNameFormat"
            :placeholder="t('请输入流水线命名规范提示说明')"
            :rows="3"
            :maxlength="200"
            type="textarea"
          >
          </bk-input>
        </bk-form-item>
        <bk-form-item
          :label="t('构建日志归档阈值')"
          property="loggingLineLimit"
          :description="t('单个步骤(Step)日志达到阈值时，将压缩并归档到日志仓库。可下载日志文件到本地查看。')"
        >
          <bk-input
            v-model="projectData.properties.loggingLineLimit"
            class="log-line-limit-input"
            type="number"
            :showControl="false"
            :min="1"
            :max="100"
            :suffix="t('万行')"
            :placeholder="t('缺省时默认为10')"
          >
          </bk-input>
        </bk-form-item>
      </div>
    </div>
  </bk-form>

  <bk-dialog
    :title="t('设置项目最大可授权人员范围')"
    width="900"
    size="large"
    dialog-type="show"
    :is-show="showDialog"
    @closed="() => showDialog = false"
  >
    <IAMIframe
      ref="iframeRef"
      class="member-iframe"
      path="add-member-boundary"
      :query="{
        search_sence: 'add'
      }"
    />
  </bk-dialog>
</template>

<style lang="postcss" scoped>
  .textarea {
    :deep(textarea) {
        width: auto;
    }
    margin-top: 10px;
  }
  :deep(.bk-form-label) {
    font-size: 12px;
  }
  .logo-upload-tip {
    font-size: 12px;
    color: #979BA5;
  }
  .edit-line {
    cursor: pointer;
  }
  .member-iframe {
    height: 600px;
  }
  .bk-dropdown-box {
    width: 200px;
    margin-right: 12px;
    display: inline-block;
    vertical-align: middle;
  }
  .authSecrecy-item {
    border-bottom: 1px dashed #979ba5;
  }
  .error-tips {
    color: #ff5656;
    font-size: 12px;
    position: absolute;
    top: 26px;
  }
  .text-link {
    font-size: 12px;
    color: #3c96ff;
  }
  .log-line-limit-input {
    width: 150px;
  }
</style>

<style lang="postcss">
  .dark {
    background: #26323d !important;
  }
  .bk-form-error {
    white-space: nowrap;
  }
  .project-tab {
    width: 100%;
    padding: 20px 30px;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 #00000026;
    &.advanced {
      margin-bottom: 24px;
    }
    .title {
      margin-bottom: 16px;
      font-weight: 700;
      font-size: 14px;
      color: #63656E;
    }
    .sub-title {
      font-size: 14px;
      border-bottom: 2px solid #DCDEE5;
      margin-bottom: 15px;
    }
    .conventions-input {
      margin-top: 10px;
      max-width: 1000px;
    }
  }
</style>
