import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { CampaignService } from '../../services/campaign.service';
import { CampaignChartComponent } from '../campaign-chart/campaign-chart.component';
import { CampaignConfigComponent } from '../campaign-config/campaign-config.component';
import { environment } from '../../../environments/environment';
import { MessageService } from '../../services/message.service';

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

  constructor(private router: Router, private campaignService: CampaignService, private messageService: MessageService) {
  }

  ngOnInit() {
    if (!this.campaignService.isAccountReady()) {
      const acct = this.campaignService.account;

      if (acct && acct.hasValidYahooToken() && !acct.adv_id) {
        // With a valid Yahoo OAuth token but not a Gemini account ID
        this.campaign_loaded_err = environment.geminiAcctInvalid;
      } else {
        this.router.navigateByUrl('f/login', {skipLocationChange: true});
        this.campaign_loaded_err = 'user has not logged in yet.';
        this.campaign_loaded = true;
        return;
      }
    } else {
      const acct = this.campaignService.account;
      this.advId = acct.adv_id; // 1643580;
      this.cmpId = acct.cmp_id; // 364710042;

      if (acct.adv_status !== 'ACTIVE') {
        this.campaign_loaded_err = environment.geminiAcctInactive;
      }
    }

    if (this.campaign_loaded_err) {
      console.log(this.campaign_loaded_err);
      this.router.navigateByUrl('f/login', {skipLocationChange: true});
      this.messageService.push(this.campaign_loaded_err, 'danger');
    }

    this.campaign_loaded = true;
  }
}
