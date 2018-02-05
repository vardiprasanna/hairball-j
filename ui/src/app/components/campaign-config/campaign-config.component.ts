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
      this.campaign_original = new Campaign();
      Object.assign(this.campaign_original, cmp);
      Object.assign(this.campaign, this.campaign_original);
    }, err => {
      this.campaign_config_loaded_err = (err.message ? err.message : JSON.stringify(err));
    }).then(() => {
      this.campaign_config_loaded = true;
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
    if (!this.campaign_original || !this.campaign) {
      return; // cannot update because we're not even able to load the campaign
    }
    if (this.campaign.price >= 0 && this.campaign.budget >= 0) {
      this.campaignService.updateCampaign(this.campaignId, this.campaign).then(() => {
        this.campaign_original.price = this.campaign.price;
        this.campaign_original.budget = this.campaign.budget;
      }, err => {
        this.campaign_config_loaded_err = (err.message ? err.message : JSON.stringify(err));
      });
    } else {
      console.log('invalid amount: ' + JSON.stringify(this.campaign));
    }
    return false;
  }

  public updateStatus() {
    if (!this.campaign_original) {
      return; // cannot update because we're not even able to load the campaign
    }
    const cmp: Campaign = new Campaign();
    cmp.is_running = !this.campaign_original.is_running;
    cmp.price = this.campaign_original.price;
    cmp.budget = this.campaign_original.budget;

    this.campaignService.updateCampaign(this.campaignId, cmp).then(() => {
      this.campaign_original.is_running = !this.campaign_original.is_running;
      this.campaign.is_running = this.campaign_original.is_running;
    }, err => {
      this.campaign_config_loaded_err = (err.message ? err.message : JSON.stringify(err));
    });
    return false;
  }
}
