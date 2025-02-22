<template>
    <article class="trend-common-home">
        <header class="common-head">
            <bk-tab
                :active.sync="chartTab"
                type="unborder-card"
            >
                <bk-tab-panel
                    v-for="(panel, index) in storeChartTabs"
                    v-bind="panel"
                    :key="index"
                ></bk-tab-panel>
            </bk-tab>

            <bk-select
                class="common-time"
                v-model="time"
                :clearable="false"
            >
                <bk-option
                    v-for="item in timeList"
                    :key="item.value"
                    :id="item.value"
                    :name="item.name"
                ></bk-option>
            </bk-select>
        </header>

        <bk-exception
            class="exception-wrap-item exception-part"
            type="empty"
            v-show="isEmpty"
        ></bk-exception>
        <canvas
            class="store-chart"
            v-show="!isEmpty"
        ></canvas>
    </article>
</template>

<script>
    import BKChart from '@blueking/bkcharts'
    import dayjs from 'dayjs'
    import api from '@/api'

    function getTimeRange (time) {
        const now = dayjs(dayjs().format('YYYY-MM-DD')).subtract(1, 'days')
        const params = {
            endTime: now.format('YYYY-MM-DD HH:mm:ss'),
            startTime: now.subtract(1, time)
        }
        if (time === 'weeks') params.startTime = params.startTime.add(1, 'days')
        params.startTime = params.startTime.format('YYYY-MM-DD HH:mm:ss')
        return params
    }

    export default {
        props: {
            detail: Object,
            type: String
        },

        data () {
            return {
                storeChartTabs: [
                    { name: 'totalDownloads', label: this.$t('store.安装量'), count: 10 },
                    { name: 'execTrend', label: this.$t('store.执行趋势'), count: 20 },
                    { name: 'failDetail', label: this.$t('store.错误分析'), count: 30 }
                ],
                storeChart: {},
                timeList: [
                    { name: this.$t('store.周'), value: 'weeks' },
                    { name: this.$t('store.月'), value: 'months' },
                    { name: this.$t('store.年'), value: 'years' }
                ],
                time: 'weeks',
                chartTab: 'totalDownloads',
                chartData: {},
                isEmpty: false
            }
        },

        watch: {
            chartTab () {
                this.getChartData().then(this.paintAgain)
            },

            time () {
                this.getChartData().then(this.paintAgain)
            }
        },

        mounted () {
            this.getChartData().then(this.paintAgain)
        },

        methods: {
            getChartData () {
                return new Promise((resolve, reject) => {
                    const chartData = this.chartData[this.time]
                    if (chartData) {
                        resolve(chartData)
                    } else {
                        const code = this.detail.atomCode
                        const params = getTimeRange(this.time)
                        return api.requestStaticChartData(this.type.toUpperCase(), code, params).then((res) => {
                            this.chartData[this.time] = res
                            resolve(res)
                        }).catch((err) => {
                            resolve()
                            this.$bkMessage({ theme: 'error', message: err.message || err })
                        })
                    }
                })
            },

            paintAgain (data) {
                if (!data) return
                if (this.storeChart && this.storeChart.destroy) this.storeChart.destroy()
                let paintData = []
                let method = ''
                switch (this.chartTab) {
                    case 'totalDownloads':
                        paintData = data.dailyStatisticList || []
                        method = this.paintInstall
                        break
                    case 'execTrend':
                        paintData = data.dailyStatisticList || []
                        method = this.paintTrend
                        break
                    case 'failDetail':
                        for (const key in data.totalFailDetail || {}) {
                            paintData.push(data.totalFailDetail[key])
                        }
                        method = this.paintError
                        break
                }
                this.isEmpty = paintData.length <= 0
                if (!this.isEmpty) method(paintData)
            },

            paintInstall (dailyStatisticList) {
                const context = document.querySelector('.store-chart')
                this.storeChart = new BKChart(context, {
                    type: 'line',
                    data: {
                        labels: dailyStatisticList.map(x => x.statisticsTime),
                        datasets: [
                            {
                                label: this.$t('store.安装量'),
                                backgroundColor: 'rgba(43, 124, 255,0.3)',
                                borderColor: 'rgba(43, 124, 255,1)',
                                lineTension: 0,
                                borderWidth: 2,
                                pointRadius: 0,
                                pointHitRadius: 3,
                                pointHoverRadius: 3,
                                data: dailyStatisticList.map(x => x.dailyDownloads)
                            }
                        ]
                    },
                    options: {
                        maintainAspectRatio: false,
                        responsive: true,
                        plugins: {
                            tooltip: {
                                mode: 'x',
                                intersect: false,
                                singleInRange: true
                            },
                            legend: {
                                display: false
                            },
                            crosshair: {
                                enabled: true,
                                mode: 'x',
                                style: {
                                    x: {
                                        enabled: true,
                                        color: '#cde0ff',
                                        weight: 1,
                                        borderStyle: 'solid'
                                    },
                                    y: {
                                        enabled: false
                                    }
                                }
                            }
                        },
                        layout: {
                            padding: {
                                left: 0,
                                right: 0,
                                top: 20,
                                bottom: 0
                            }
                        },
                        scales: {
                            yAxes: {
                                scaleLabel: {
                                    display: true,
                                    padding: 0
                                },
                                gridLines: {
                                    drawTicks: false,
                                    borderDash: [5, 5]
                                },
                                ticks: {
                                    padding: 10
                                },
                                min: 0
                            },
                            xAxes: {
                                scaleLabel: {
                                    display: true,
                                    padding: 0
                                },
                                gridLines: {
                                    drawTicks: false,
                                    display: false
                                },
                                ticks: {
                                    padding: 10,
                                    sampleSize: 10,
                                    autoSkip: true,
                                    maxRotation: 0
                                }
                            }
                        }
                    }
                })
            },

            paintTrend (dailyStatisticList) {
                const context = document.querySelector('.store-chart')
                const successRate = this.$t('store.执行成功率')
                const failRate = this.$t('store.执行失败率')
                const successNum = this.$t('store.执行成功数')
                const failNum = this.$t('store.执行失败数')
                this.storeChart = new BKChart(context, {
                    type: 'line',
                    data: {
                        labels: dailyStatisticList.map(x => x.statisticsTime),
                        datasets: [
                            {
                                label: this.$t('store.执行成功率'),
                                backgroundColor: 'rgba(5, 155, 255, 0.3)',
                                borderColor: 'rgba(5, 155, 255, 1)',
                                lineTension: 0,
                                borderWidth: 2,
                                pointRadius: 2,
                                pointHitRadius: 3,
                                pointHoverRadius: 3,
                                data: dailyStatisticList.map(x => x.dailySuccessRate)
                            },
                            {
                                label: this.$t('store.执行失败率'),
                                backgroundColor: 'rgba(255, 24, 113, 0.3)',
                                borderColor: 'rgba(255, 24, 113, 1)',
                                lineTension: 0,
                                borderWidth: 2,
                                pointRadius: 2,
                                pointHitRadius: 3,
                                pointHoverRadius: 3,
                                data: dailyStatisticList.map(x => x.dailyFailRate)
                            }
                        ]
                    },
                    options: {
                        maintainAspectRatio: false,
                        responsive: true,
                        plugins: {
                            tooltip: {
                                mode: 'x',
                                intersect: false,
                                singleInRange: true,
                                callbacks: {
                                    label (context) {
                                        const index = context.dataIndex
                                        const curStatis = dailyStatisticList[index]
                                        let label = ''
                                        switch (context.dataset.label) {
                                            case successRate:
                                                label = `${successRate} / ${successNum}：${curStatis.dailySuccessRate}% / ${curStatis.dailySuccessNum}`
                                                break
                                            case failRate:
                                                label = `${failRate} / ${failNum}：${curStatis.dailyFailRate}% / ${curStatis.dailyFailNum}`
                                                break
                                        }
                                        return label
                                    }
                                }
                            },
                            legend: {
                                position: 'top',
                                legendIcon: 'arc',
                                align: 'start',
                                labels: {
                                    padding: 10,
                                    usePointStyle: true,
                                    pointStyle: 'dash'
                                }
                            },
                            crosshair: {
                                enabled: true,
                                mode: 'x',
                                style: {
                                    x: {
                                        enabled: true,
                                        color: '#cde0ff',
                                        weight: 1,
                                        borderStyle: 'solid'
                                    },
                                    y: {
                                        enabled: false
                                    }
                                }
                            }
                        },
                        scales: {
                            yAxes: {
                                scaleLabel: {
                                    display: true,
                                    padding: 0
                                },
                                gridLines: {
                                    drawTicks: false,
                                    borderDash: [5, 5]
                                },
                                ticks: {
                                    padding: 10
                                },
                                min: 0
                            },
                            xAxes: {
                                scaleLabel: {
                                    display: true,
                                    padding: 0
                                },
                                gridLines: {
                                    drawTicks: false,
                                    display: false
                                },
                                ticks: {
                                    padding: 10,
                                    sampleSize: 10,
                                    autoSkip: true,
                                    maxRotation: 0
                                }
                            }
                        }
                    }
                })
            },

            paintError (totalFailDetail) {
                const context = document.querySelector('.store-chart')
                this.storeChart = new BKChart(context, {
                    type: 'pie',
                    data: {
                        labels: totalFailDetail.map(x => x.name),
                        datasets: [
                            {
                                label: this.$t('store.错误分析'),
                                hoverOffset: 4,
                                data: totalFailDetail.map(x => x.failNum),
                                backgroundColor: [
                                    'rgba(51,157,255,1)',
                                    'rgba(59,206,149,1)',
                                    'rgba(255,156,74,1)',
                                    'rgba(255,111,114,1)',
                                    'rgba(248,211,15,1)'
                                ],
                                hoverBorderColor: 'white',
                                datalabels: {
                                    labels: {
                                        value: {
                                            align: 'bottom',
                                            backgroundColor: 'white',
                                            borderColor: 'white',
                                            borderWidth: 2,
                                            borderRadius: 4,
                                            color (ctx) {
                                                return ctx.dataset.backgroundColor
                                            },
                                            formatter (value, ctx) {
                                                return value
                                            },
                                            padding: 4
                                        }
                                    }
                                }
                            }
                        ]
                    },
                    options: {
                        maintainAspectRatio: false,
                        responsive: true,
                        plugins: {
                            legend: {
                                position: 'left',
                                legendIcon: 'arc',
                                align: 'center',
                                labels: {
                                    padding: 20,
                                    usePointStyle: true,
                                    pointStyle: 'dash'
                                }
                            }
                        },
                        layout: {
                            padding: {
                                left: 0,
                                right: 130,
                                top: 0,
                                bottom: 0
                            }
                        }
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .trend-common-home {
        margin-top: 5px;
        height: calc(100% - 39px);
        padding-bottom: 20px;
    }

    .common-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        .common-time {
            width: 250px;
        }
    }

    .store-chart {
        height: calc(100% - 32px);
    }

    ::v-deep .bk-tab-header {
        background-color: #fff;
        height: 32px;
        line-height: 32px;
        background-image: none;
        .bk-tab-label-wrapper .bk-tab-label-list {
            height: 32px;
            .bk-tab-label-item {
                line-height: 32px;
                color: #63656e;
                min-width: 36px;
                padding: 0;
                margin-right: 20px;
                &:last-child {
                    margin: 0;
                }
                &::after {
                    height: 2px;
                    left: 0px;
                    width: 100%;
                }
                &.active {
                    color: #3a84ff;
                }
            }
        }
        .bk-tab-header-setting {
            height: 32px;
            line-height: 32px;
        }
    }
    ::v-deep .bk-tab-section {
        padding: 0;
    }
    ::v-deep .bk-exception-text {
        margin-top: -40px;
    }
</style>
