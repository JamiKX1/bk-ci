/// <reference path='./typings/index.d.ts' />

import 'core-js/es7/array'
import Vue from 'vue'

import createRouter from '@/router'
import store from '@/store'
import eventBus from '@/utils/eventBus'
import App from '@/views/App.vue'
import Logo from '@/components/Logo/index.vue'
import Icon from '@/components/Icon/index.vue'
import EmptyTips from '@/components/EmptyTips/index.vue'
import ShowTooltip from '@/components/ShowTooltip/index.vue'
import DevopsFormItem from '@/components/DevopsFormItem/index.vue'
import iframeUtil from '@/utils/iframeUtil'
import createLocale from '../../locale'

import VeeValidate from 'vee-validate'
import ExtendsCustomRules from './utils/customRules'
import validDictionary from './utils/validDictionary'
import showAskPermissionDialog from './components/AskPermissionDialog'
// 全量引入 bk-magic-vue
import bkMagic from 'bk-magic-vue'
// 全量引入 bk-magic-vue 样式
require('bk-magic-vue/dist/bk-magic-vue.min.css') // eslint-disable-line
import './assets/scss/index.scss'

declare module 'vue/types/vue' {
    interface Vue {
        $bkMessage: any
        $bkInfo: any
        $showAskPermissionDialog: any
        iframeUtil: any
    }
}

// @ts-ignore
Vue.use(VeeValidate, {
    fieldsBagName: 'veeFields',
    locale: 'cn'
})

VeeValidate.Validator.localize(validDictionary)
ExtendsCustomRules(VeeValidate.Validator.extend)

Vue.use(bkMagic)

Vue.component('Logo', Logo)
Vue.component('Icon', Icon)
Vue.component('EmptyTips', EmptyTips)
Vue.component('ShowTooltip', ShowTooltip)
Vue.component('DevopsFormItem', DevopsFormItem)

const { i18n, dynamicLoadModule, setLocale } = createLocale(require.context('@locale/nav/', false, /\.js/))
const router = createRouter(store, dynamicLoadModule)
window.eventBus = eventBus
Vue.prototype.iframeUtil = iframeUtil(router)
Vue.prototype.$showAskPermissionDialog = showAskPermissionDialog
Vue.prototype.$setLocale = setLocale

window.devops = new Vue({
    el: '#devops-root',
    i18n,
    router,
    store,
    render (h) {
        return h(App)
    }
})
