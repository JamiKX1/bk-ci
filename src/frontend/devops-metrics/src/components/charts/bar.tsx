import {
  defineComponent,
  PropType,
  watch,
  onMounted,
  onBeforeUnmount,
  ref,
} from 'vue';
import BKChart from '@blueking/bkcharts';

export interface IData {
  label: string,
  list: Array<string | number>,
  backgroundColor?: string
}

export default defineComponent({
  props: {
    data: Array as PropType<Array<IData>>,
    labels: Array,
    title: String,
  },

  setup(props) {
    const canvasRef = ref(null);
    let chart;

    const destoryChart = () => {
      chart?.destroy();
    };
    const draw = () => {
      destoryChart();
      const { data, labels, title } = props;
      chart = new BKChart(canvasRef.value, {
        type: 'bar',
        data: {
          labels,
          datasets: data.map(item => ({
            label: item.label,
            backgroundColor: item.backgroundColor || 'rgba(43, 124, 255,0.3)',
            borderSkipped: 'bottom',
            borderWidth: 1,
            data: [...item.list],
          })),
        },
        options: {
          maintainAspectRatio: false,
          responsive: true,
          plugins: {
            tooltip: {
              mode: 'x',
              intersect: false,
              enableItemActive: true,
              singleInRange: true,
            },
            legend: {
              position: 'bottom',
              legendIcon: 'arc',
              align: 'center',
              labels: {
                padding: 10,
                usePointStyle: true,
                pointStyle: 'dash',
              },
            },
          },
          scales: {
            y: {
              stacked: true,
              title: {
                display: true,
                text: title,
                align: 'start',
              },
              grid: {
                drawTicks: false,
                borderDash: [5, 5],
              },
              min: 0,
            },
            x: {
              stacked: true,
              grid: {
                drawTicks: false,
                display: false,
              },
            },
          },
        },
      });
    };

    watch(
      props,
      draw,
    );

    onMounted(draw);

    onBeforeUnmount(destoryChart);

    return () => (
      <div class="canvas-wrapper">
        <canvas class="bar" ref={canvasRef}></canvas>
      </div>
    );
  },
});
