import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CampaignChartComponent } from '../campaign-chart/campaign-chart.component';
import { CampaignConfigComponent } from '../campaign-config/campaign-config.component';

@Component({
  selector: 'app-campaign',
  entryComponents: [CampaignChartComponent, CampaignConfigComponent],
  templateUrl: './campaign.component.html',
  styleUrls: ['./campaign.component.css'],
})
export class CampaignComponent implements OnInit {
  cmpId: number;
  advId: number;
  app_loaded = false;

  constructor(private router: Router) {
  }

  static getQueryIntParam(reg): number {
    const query = window.location.search;
    const val = reg.exec(query);
    return (val && val.length >= 1) ? Number.parseInt(val[1]) : null;
  }

  ngOnInit() {
    this.cmpId = this.getCampaignIdParam();
    this.advId = this.getAdvertiserIdParam();
    this.app_loaded = true;

    if (!(this.cmpId && this.advId)) {
      this.router.navigateByUrl('g/login');
    }
  }

  private getAdvertiserIdParam(): number {
    const advertiserId = CampaignComponent.getQueryIntParam(/[?/&;,]?adv=([0-9]+)[&;,]?.*$/);
    if (advertiserId) {
      return advertiserId;
    }
    return 1643580; // TODO
  }

  private getCampaignIdParam(): number {
    const campaignId = CampaignComponent.getQueryIntParam(/[?/&;,]?cmp=([0-9]+)[&;,]?.*$/);
    if (campaignId) {
      return campaignId;
    }
    return 363491351; // TODO;
  }
}
