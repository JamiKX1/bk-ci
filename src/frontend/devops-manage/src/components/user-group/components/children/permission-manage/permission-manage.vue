<template>
  <section class="permission-manage">
    <group-aside
      :show-create-group="showCreateGroup"
      :resource-type="resourceType"
      :resource-code="resourceCode"
      :project-code="projectCode"
      :ajax-prefix="ajaxPrefix"
      @choose-group="handleChooseGroup"
      @create-group="handleCreateGroup"
      @close-manage="handleCloseManage"
      @change-group-detail-tab="handleChangeGroupDetailTab"
    />
    <iam-iframe
      v-if="path && !isAllMember"
      :path="path"
    />
    <ManageAll v-else />
  </section>
</template>

<script>
import GroupAside from './group-aside.vue';
import IamIframe from './iam-Iframe.vue';
import ManageAll from './manage-all.vue';

export default {
  name: 'permission-manage',

  components: {
    GroupAside,
    IamIframe,
    ManageAll,
  },

  props: {
    resourceType: {
      type: String,
      default: '',
    },
    resourceCode: {
      type: String,
      default: '',
    },
    projectCode: {
      type: String,
      default: '',
    },
    showCreateGroup: {
      type: Boolean,
      default: true,
    },
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  emits: ['close-manage'],

  data() {
    return {
      path: '',
      activeIndex: '',
      tabName: '',
      isAllMember: false,
    };
  },

  methods: {
    handleChangeGroupDetailTab(payload) {
      this.tabName = payload;
    },
    handleChooseGroup(payload) {
      if (!payload.projectMemberGroup) {
        this.isAllMember = false;
        this.path = `user-group-detail/${payload.groupId}?role_id=${payload.managerId}&tab=${this.tabName}`;
      } else {
        this.isAllMember = true;
      }
    },
    handleCreateGroup() {
      this.activeIndex = '';
      this.isAllMember = false;
      this.path = 'create-user-group';
    },
    handleCloseManage() {
      this.$emit('close-manage');
    },
  },
};
</script>

<style lang="scss" scoped>
.permission-manage {
    display: flex;
    height: 100%;
    box-shadow: 0 2px 2px 0 #00000026;
}
</style>
