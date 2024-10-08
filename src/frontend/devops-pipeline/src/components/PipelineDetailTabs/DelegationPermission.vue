<template>
    <main class="delegation-permission" v-bkloading="{ isLoading }">
        <section class="content-warpper">
            <header class="header">
                <logo class="mr5" name="help-document-fill" size="20" />
                {{ $t('delegationPermission') }}
            </header>
            <div class="content">
                <p>{{ $t('delegation.tips1') }}</p>
                <i18n
                    class="mt10"
                    tag="p"
                    path="delegation.tips2">
                    <span class="highlight">{{ $t('delegation.pipelineExecPermission') }}</span>
                </i18n>
                <i18n
                    tag="p"
                    path="delegation.tips3">
                    <span class="highlight">{{ 'BK_CI_AUTHORIZER' }}</span>
                </i18n>
                <p class="mt20">{{ $t('delegation.tips4') }}</p>
                <i18n
                    tag="p"
                    path="delegation.tips5">
                    <span class="highlight">{{ $t('delegation.newOperator') }}</span>
                </i18n>
            </div>
        </section>

        <section class="mt30">
            <div class="panel-content">
                <p>
                    <label class="block-row-label">{{ $t('delegation.proxyHolderForExecutionPermissions') }}</label>
                    <span
                        :class="{
                            'block-row-value': true,
                            'reset-row': !resourceAuthData.executePermission
                        }">
                        <span
                            :class="{
                                'name': true,
                                'not-permission': !resourceAuthData.executePermission
                            }"
                            v-bk-tooltips="{
                                content: $t('delegation.expiredTips'),
                                disabled: resourceAuthData.executePermission
                            }"
                        >
                            {{ resourceAuthData.handoverFrom }}
                        </span>
                        <bk-tag theme="danger" v-if="!resourceAuthData?.executePermission && !isLoading">{{ $t('delegation.expired') }}</bk-tag>
                        <span
                            class="refresh-auth"
                            v-perm="{
                                hasPermission: hasResetPermission,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipelineId,
                                    action: RESOURCE_ACTION.MANAGE
                                }
                            }"
                            @click="handleShowResetDialog">
                            <logo class="refresh-icon" name="refresh" size="14" />
                            {{ $t('delegation.resetAuthorization') }}
                        </span>
                    </span>
                </p>
                <p>
                    <label class="block-row-label">{{ $t('delegation.authTime') }}</label>
                    <span class="block-row-value">{{ convertTime(resourceAuthData.handoverTime) }}</span>
                </p>
            </div>
        </section>
        
        <bk-dialog
            ext-cls="reset-auth-dialog"
            :value="showResetDialog"
            :show-footer="false"
            @value-change="handleToggleShowResetDialog"
        >
            <span class="reset-dialog-title">
                {{ $t('delegation.confirmReset') }}
            </span>
            <span class="reset-dialog-tips">
                <i18n
                    tag="p"
                    path="delegation.resetAuthTips1">
                    <span class="highlight">{{ $t('delegation.yourPermission') }}</span>
                </i18n>
                <p>{{ $t('delegation.resetAuthTips2') }}</p>
            </span>
            <span class="reset-dialog-footer">
                <bk-button
                    class="mr10 btn"
                    theme="primary"
                    :loading="resetLoading"
                    @click="handleReset"
                >
                    {{ $t('delegation.reset') }}
                </bk-button>
                <bk-button
                    class="btn"
                    :loading="resetLoading"
                    @click="showResetDialog = !showResetDialog">
                    {{ $t('delegation.cancel') }}
                </bk-button>
            </span>
        </bk-dialog>

        <bk-dialog
            ext-cls="reset-auth-dialog"
            v-model="showFailedDialog"
            width="640"
            :show-footer="false"
            header-position="center"
        >
            <span class="reset-dialog-title">
                {{ $t('delegation.resetFailed') }}
            </span>
            <div class="reset-failed-item">
                <template v-for="(item, index) in failedArr">
                    <div :key="item">
                        <span v-if="index > 0">
                            {{ index }}.
                        </span>
                        <span v-html="item" />
                    </div>
                </template>
            </div>
            <span class="reset-dialog-footer">
                <bk-button
                    class="btn"
                    theme="primary"
                    :loading="resetLoading"
                    @click="showFailedDialog = !showFailedDialog">
                    {{ $t('delegation.confirm') }}
                </bk-button>
            </span>
        </bk-dialog>
    </main>
</template>

<script>
    import Logo from '@/components/Logo'
    import { mapActions, mapState } from 'vuex'
    import { convertTime } from '@/utils/util'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    export default {
        components: {
            Logo
        },
        data () {
            return {
                isLoading: false,
                showResetDialog: false,
                resetLoading: false,
                resourceAuthData: {},
                showFailedDialog: false,
                failedArr: []
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            hasResetPermission () {
                return this.pipelineInfo?.permissions.canManage
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            pipelineId () {
                return this.pipelineInfo.pipelineId
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            this.fetchResourceAuth()
        },
        methods: {
            convertTime,
            ...mapActions('pipelines', [
                'getResourceAuthorization',
                'resetPipelineAuthorization'
            ]),
            handleShowResetDialog () {
                this.showResetDialog = true
            },
            handleToggleShowResetDialog (val) {
                if (!val) {
                    this.showResetDialog = false
                    this.resetLoading = false
                }
            },
            async fetchResourceAuth () {
                try {
                    this.isLoading = true
                    this.resourceAuthData = await this.getResourceAuthorization({
                        projectId: this.projectId,
                        resourceType: 'pipeline',
                        resourceCode: this.pipelineId
                    })
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isLoading = false
                }
            },
            async handleReset () {
                this.resetLoading = true
                try {
                    this.resetLoading = false
                    const res = await this.resetPipelineAuthorization({
                        projectId: this.projectId,
                        params: {
                            projectCode: this.projectId,
                            resourceType: 'pipeline',
                            handoverChannel: 'OTHER',
                            resourceAuthorizationHandoverList: [
                                {
                                    projectCode: this.projectId,
                                    resourceType: 'pipeline',
                                    resourceName: this.resourceAuthData.resourceName,
                                    resourceCode: this.resourceAuthData.resourceCode,
                                    handoverFrom: this.resourceAuthData.handoverFrom,
                                    handoverTo: this.$userInfo.username
                                }
                            ]
                        }
                    })
                    this.showResetDialog = false
                    if (res?.FAILED?.length) {
                        const message = res.FAILED[0]?.handoverFailedMessage || ''
                        if (message.includes('<br/>')) {
                            this.failedArr = message.split('<br/>')
                            this.showFailedDialog = true
                        } else {
                            this.$bkMessage({
                                theme: 'error',
                                message
                            })
                        }
                    } else {
                        this.fetchResourceAuth()
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('delegation.resetSuc')
                        })
                    }
                } catch (e) {
                    this.resetLoading = false
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    .delegation-permission {
        padding: 24px;
        font-size: 12px;
        .content-warpper {
            border-radius: 2px;
            border: 1px solid #DCDEE5;
        }
        .header {
            display: flex;
            align-items: center;
            height: 40px;
            background-color: #F5F7FA;
            padding: 0 16px;
            font-size: 14px;
            font-weight: 700;
        }
        .content {
            padding: 20px 16px;
            line-height: 24px;
            color: #63656E;
            font-size: 12px;
        }
        .highlight {
            color: #FF9C01;
            font-weight: 700;
        }
        .reset-row {
            position: relative;
            top: -4px;
            .not-permission {
                color: #C4C6CC;
                text-decoration: line-through;
                margin-right: 5px;
            }
        }
    }
    .panel-content {
        display: grid;
        grid-gap: 16px;
        grid-template-rows: minmax(18px, auto);
        margin-bottom: 32px;
        p {
            display: grid;
            grid-auto-flow: column;
            grid-template-columns: 120px 1fr;
            align-items: flex-start;
            grid-gap: 10px;
            font-size: 12px;
            color: #63656e;

            >label {
                text-align: right;
                line-height: 18px;
                color: #979BA5;
            }
        }
        .refresh-auth {
            margin-left: 20px;
            cursor: pointer;
            color: #3A84FF;
        }
        .refresh-icon {
            position: relative;
            top: 2px;
            margin-right: 2px;
        }
    }
    .reset-auth-dialog {
        text-align: center;
        .bk-dialog-body {
            display: flex;
            flex-direction: column;
            align-items: center;
            max-height: calc(50vh - 50px);
        }
        
        .reset-dialog-title {
            font-size: 20px;
            color: #313238;
            margin-bottom: 15px;
        }
        
        .reset-dialog-tips {
            text-align: left;
            color: #63656E;
            font-size: 12px;
            padding: 10px 20px;
            background: #F5F7FA;
            span {
                color: #FF9C01;
            }
        }
        .reset-dialog-footer {
            margin-top: 24px;
            .btn {
                width: 88px;
            }
        }
        .reset-failed-item {
            text-align: left;
            max-height: 400px;
            overflow: auto;
            width: 100%;
            color: #63656E;
            font-size: 12px;
            padding: 10px 20px;
            background: #F5F7FA;
            a {
                color: #3A84FF;
            }
        }
    }
</style>
