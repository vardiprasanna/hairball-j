import { Component, OnInit, Input } from '@angular/core';
import { CampaignService } from '../../services/campaign.service';
import { Campaign } from '../../model/campaign';

@Component({
  selector: 'app-campaign-config',
  templateUrl: './campaign-config.component.html',
  styleUrls: ['./campaign-config.component.css']
})
export class CampaignConfigComponent implements OnInit {
  @Input()
  campaignId: number;
  @Input()
  advertiserId: number;

  campaign: Campaign;
  campaign_original: Campaign;
  campaign_config_loaded = false;
  campaign_config_loaded_err: any;
  is_changed = false;

  constructor(private campaignService: CampaignService) {
    this.campaign = new Campaign();
  }

  ngOnInit() {
    let query = window.location.search;
    if (query.charAt(0) !== '?') {
      query = '?' + query;
    }

    this.campaignService.getCampaign(this.campaignId, query).then(cmp => {
      this.campaign_original = {
        budget: cmp.budget,
        cpc: cmp.price,
        start_date: new Date(cmp.startDateInMilli),
        end_date: new Date(cmp.endDateInMilli),
        is_running: (cmp.status === 'ACTIVE')
      };

      Object.assign(this.campaign, this.campaign_original);
      this.campaign_config_loaded = true;
      this.campaign_config_loaded_err = null;
    }, err => {
      this.campaign_config_loaded = true;
      this.campaign_config_loaded_err = (err.message ? err.message : JSON.stringify(err));
    });
  }

  change(): void {
    if (!this.campaign_original) {
      this.is_changed = true;
    }
    for (const name of Object.getOwnPropertyNames(this.campaign)) {
      if (this.campaign[name] !== this.campaign_original[name]) {
        this.is_changed = true;
        return;
      }
    }

    this.is_changed = false;
  }

  public updateCost() {
    console.log(event + ', ' + JSON.stringify(this.campaign));
    return false;
  }

  public updateStatus() {
    return false;
  }
}
