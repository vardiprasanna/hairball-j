import { AfterContentInit, Component, ElementRef, OnDestroy, OnInit, ViewChild, Input } from '@angular/core';
import { chart } from 'highcharts';

import { CampaignService } from '../../services/campaign.service';
import { Metric } from '../../model/metric';

const stats_options = {
  'Impressions': {
    decimalOnly: true
  },
  'Clicks': {
    decimalOnly: true
  },
  'Conversions': {
    decimalOnly: true
  },
  'Spend': {
    decimalOnly: false
  },
  'Average CPC': {
    decimalOnly: false
  },
  'Average CPM': {
    decimalOnly: false
  }
};

const report_choices = [
  {
    label: 'Today', selected: false
  }, {
    label: 'Yesterday', selected: true
  }, {
    label: 'This week (Mon - Today)', selected: false
  },
  {
    label: 'Last week (Mon - Sun)', selected: false
  },
  {
    label: 'Last 7 days', selected: false
  }, {
    label: 'Last 14 days', selected: false
  }, {
    label: 'This month', selected: false
  }, {
    label: 'Last month', selected: false
  }, {
    label: 'Last 30 days', selected: false
  }
];

const LIGHT_PINK = '#D050D0';
const LIGHT_BLUE = 'DeepSkyBlue';

interface ReportOption {
  report_choice_idx: number; // 0 offset of report_options
  stats_x_start: Date;
  stats_x_end: Date;
  stats_yaxis: string[]; // primary and secondary stats
  advertiserId?: number;
  campaignId?: number;
  rollup?: string;
}

@Component({
  selector: 'app-campaign-chart',
  templateUrl: './campaign-chart.component.html',
  styleUrls: ['./campaign-chart.component.css']
})

export class CampaignChartComponent implements OnInit, AfterContentInit, OnDestroy {
  @Input()
  campaignId: number;
  @Input()
  advertiserId: number;

  @ViewChild('chartTarget')
  chartTarget: ElementRef;
  chart: Highcharts.ChartObject;

  current_report: ReportOption;
  report_choices: any = report_choices; // define a local to work around angular's static var access
  report_loaded_err: any;
  report_loaded = false;
  report_empty = true;
  stats: Metric;

  constructor(private ewsService: CampaignService) {
  }

  private static subtractDays(srcDate: Date, days: number): Date {
    const milliseconds = days * 24 * 3600 * 1000;
    return new Date(srcDate.getTime() - milliseconds);
  }

  private static dateTicks(start_date: Date, end_date: Date): string[] {
    const days = [];
    const one_day = 24 * 3600 * 1000;
    for (let next_date = start_date.getTime(); next_date <= end_date.getTime(); next_date += one_day) {
      days.push(Metric.dateFormat(new Date(next_date)));
    }
    return days;
  }

  ngOnInit() {
    const yesterday = CampaignChartComponent.subtractDays(new Date(), 1);

    // Choose a default report - 'Yesterday'
    this.current_report = {
      report_choice_idx: 1,
      stats_x_start: yesterday,
      stats_x_end: yesterday,
      stats_yaxis: ['Spend', 'Average CPC'],
      advertiserId: this.advertiserId,
      campaignId: this.campaignId,
      rollup: 'Day'
    };

    this.stats = new Metric();
    const opt: ReportOption = this.current_report;
    const query = this.stats.prepareQuery(opt.advertiserId, opt.campaignId, opt.rollup, opt.stats_x_start, opt.stats_x_end);

    this.ewsService.getMetric(this.campaignId, query).then(rpt => {
      this.stats.reset(rpt);
      this.initChart(opt);

      this.report_empty = (this.stats.dataRows == null || this.stats.dataRows.length === 0);
      this.report_loaded = true;
      this.report_loaded_err = null;
    }, err => {
      this.report_empty = true;
      this.report_loaded = true;
      this.report_loaded_err = (err.message ? err.message : JSON.stringify(err));
    });
  }

  ngAfterContentInit() {
  }

  ngOnDestroy() {
    this.chart = null;
  }

  selectReport(event: Event) {
    const new_report_opt = event.srcElement.innerHTML.trim();
    let old_report_name = null;
    let new_report_index = 0;

    if (this.current_report.report_choice_idx >= 0 && this.current_report.report_choice_idx < report_choices.length) {
      old_report_name = report_choices[this.current_report.report_choice_idx].label;
    }
    if (old_report_name === new_report_opt) {
      console.log('no change');
      return; // no change
    }
    for (; new_report_index < report_choices.length; new_report_index++) {
      if (report_choices[new_report_index].label.match(new_report_opt)) {
        break;
      }
    }
    if (new_report_index === report_choices.length) {
      console.log('unknow report selection: ' + new_report_opt);
      return;
    }

    let end_date = new Date();
    let start_date = end_date;
    let day_of_week: number;
    let day_of_month: number;

    switch (new_report_opt) {
      case 'Today':
        break;

      case 'Yesterday':
        end_date = CampaignChartComponent.subtractDays(end_date, 1);
        start_date = end_date;
        break;

      case 'This week (Mon - Today)':
        day_of_week = end_date.getDay(); // 0 if date falls on Sunday
        if (day_of_week >= 1) {
          start_date = CampaignChartComponent.subtractDays(end_date, day_of_week - 1);
        } else {
          start_date = CampaignChartComponent.subtractDays(end_date, 6);
        }
        break;

      case 'Last week (Mon - Sun)':
        day_of_week = end_date.getDay(); // 0 if date falls on Sunday
        end_date = CampaignChartComponent.subtractDays(end_date, day_of_week);
        start_date = CampaignChartComponent.subtractDays(end_date, 6);
        break;

      case 'Last 7 days':
        start_date = CampaignChartComponent.subtractDays(end_date, 6);
        break;

      case 'Last 14 days':
        start_date = CampaignChartComponent.subtractDays(end_date, 13);
        break;

      case 'This month':
        day_of_month = end_date.getDate();
        start_date = CampaignChartComponent.subtractDays(end_date, day_of_month - 1);
        break;

      case 'Last month':
        day_of_month = end_date.getDate();
        end_date = CampaignChartComponent.subtractDays(end_date, day_of_month);

        day_of_month = end_date.getDate();
        start_date = CampaignChartComponent.subtractDays(end_date, day_of_month - 1);
        break;

      case 'Last 30 days':
        start_date = CampaignChartComponent.subtractDays(end_date, 29);
        break;
      default:
        break;
    }

    const new_report: ReportOption = Object.assign({}, this.current_report);
    new_report.report_choice_idx = new_report_index;
    new_report.stats_x_start = start_date;
    new_report.stats_x_end = end_date;

    const query = this.stats.prepareQuery(new_report.advertiserId, new_report.campaignId,
      new_report.rollup, new_report.stats_x_start, new_report.stats_x_end);

    // Load a new report, and update a current report options afterward
    this.ewsService.getMetric(this.campaignId, query).then(rpt => {
      this.stats.reset(rpt);
      this.updateChart(new_report);

      console.log('report data: ' + JSON.stringify(rpt));
      report_choices[this.current_report.report_choice_idx].selected = false;
      report_choices[new_report.report_choice_idx].selected = true;
      this.current_report = new_report;
    });
  }

  private updateChart(report_opt: ReportOption) {
    const x_axis: Highcharts.AxisOptions = this.initXAxis(report_opt.stats_x_start, report_opt.stats_x_end);
    const y_axis: Highcharts.AxisOptions[] = this.initYAxis(report_opt.stats_yaxis);
    const series: Highcharts.IndividualSeriesOptions[] = this.initSeries(
      report_opt.stats_yaxis, report_opt.stats_x_start, report_opt.stats_x_end);

    // console.log('new x-axis: ' + JSON.stringify(x_axis) + ', start=' + report_opt.stats_x_start + ', end=' + report_opt.stats_x_end);
    this.chart.xAxis[0].update(x_axis, false);
    for (let i = 0; i < y_axis.length; i++) {
      this.chart.yAxis[i].update(y_axis[i], false);
    }
    for (let i = 0; i < series.length; i++) {
      this.chart.series[i].update(series[i], false);
    }
    this.chart.redraw(true);
  }

  /**
   * Initialize a new chart
   */
  private initChart(report_opt: ReportOption) {
    const options: Highcharts.Options = {
      chart: {
        type: 'line',
        height: '300px',
        marginTop: 50,
        marginBottom: 50,
        alignTicks: false
      },
      legend: {
        enabled: false
      },
      tooltip: {
        shared: true
      },
      title: {
        text: null
      },
    };

    options.xAxis = this.initXAxis(report_opt.stats_x_start, report_opt.stats_x_end);
    options.yAxis = this.initYAxis(report_opt.stats_yaxis);
    options.series = this.initSeries(report_opt.stats_yaxis, report_opt.stats_x_start, report_opt.stats_x_end);

    this.chart = chart(this.chartTarget.nativeElement, options);
  }

  /**
   * Initialize the line drawing
   */
  private initSeries(stats_name: string[], start_date: Date, end_date: Date): Highcharts.IndividualSeriesOptions[] {
    const date_ticks = CampaignChartComponent.dateTicks(start_date, end_date);
    const stats_dates = this.fetchColumn('Day', 0);
    const series: Highcharts.IndividualSeriesOptions[] = [];

    for (let i = 0; i < stats_name.length; i++) {
      const stats_tmp: any[] = this.fetchColumn(stats_name[i]);
      let stats: number[] = [];

      // Fill the frontend any missing stats
      const stats_start_time = new Date(stats_dates[0]).getTime();
      for (let k = 0; k < date_ticks.length; k++) {
        if (new Date(date_ticks[k]).getTime() >= stats_start_time) {
          break;
        }
        stats.push(0);
      }

      // Copy non-empty stats, followed by filling the backend of any missing stats
      stats = stats.concat(stats_tmp);
      for (let j = stats.length; j < date_ticks.length; j++) {
        stats.push(0);
      }

      console.log('stats: ' + stats_name + ', data: ' + stats);
      series.push(
        {
          yAxis: i,
          name: stats_name[i],
          data: stats,
          color: (i === 0 ? LIGHT_PINK : LIGHT_BLUE)
        }
      );
    }
    return series;
  }

  private initXAxis(start_date: Date, end_date: Date): Highcharts.AxisOptions {
    let date_ticks = [];

    if (this.stats && this.stats.dataRows) {
      date_ticks = CampaignChartComponent.dateTicks(start_date, end_date);
    }
    return {
      categories: date_ticks,
      crosshair: {
        color: '#cccccc',
        width: 1
      },
      labels: {
        useHTML: true
      }
    };
  }

  /**
   * Initialize y-axis drawing
   */
  private initYAxis(stats_name: string[]): Highcharts.AxisOptions[] {
    const yaxis = [];

    for (let i = 0; i < stats_name.length; i++) {
      yaxis.push({
          title: {
            text: stats_name[i],
            style: {
              color: (i === 0 ? LIGHT_PINK : LIGHT_BLUE),
              fontSize: '125%'
            }
          },
          allowDecimals: !stats_options[stats_name[i]].decimalOnly,
          opposite: (i !== 0),
          gridLineWidth: (i !== 0 ? 0 : 1)
        }
      );
    }
    return yaxis;
  }

  private fetchColumn(stats_name: string, rounding: number = 100): any {
    const col_data = this.stats.fetchColumn(stats_name);

    if (rounding !== 0) {
      for (let i = 0; i < col_data.length; i++) {
        if (!Number.isNaN(col_data[i])) {
          col_data[i] = Math.round(col_data[i] * rounding) / rounding;
        }
      }
    }
    return col_data;
  }
}
