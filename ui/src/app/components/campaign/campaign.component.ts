import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { CampaignService } from '../../services/campaign.service';
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
  campaign_loaded = false;
  campaign_loaded_err: any;

  constructor(private router: Router, private campaignService: CampaignService) {
  }

  ngOnInit() {
    if (!this.campaignService.isAccountReady()) {
      this.router.navigateByUrl('f/login');
      this.campaign_loaded_err = 'not signed in or account is invalid';
    } else {
      const acct = this.campaignService.account;
      this.advId = acct.adv_id; // 1643580;
      this.cmpId = acct.cmp_id; // 364710042;
    }

    this.campaign_loaded = true;
  }
}
