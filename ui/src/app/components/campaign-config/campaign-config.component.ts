import { Component, OnInit, Input } from '@angular/core';
import { CampaignService } from '../../services/campaign.service';
import { Campaign } from '../../model/campaign';
import { MessageService } from '../../services/message.service';
import { environment } from '../../../environments/environment';

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

  constructor(private messageService: MessageService, private campaignService: CampaignService) {
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

    let errorMsg: string;
    /**
     * Valid price and budget values
     */
    if (this.campaign.price < 0.05) {
      errorMsg = environment.geminiMinBidPrice;
    } else if (this.campaign.budget < 5) {
      errorMsg = environment.geminiMinBidBudget;
    } else if ((this.campaign.price * 50) > this.campaign.budget) {
      const price = this.campaign.budget / 50;
      errorMsg = environment.geminiMaxBidPrice.replace('\${price}', '' + price);
    }

    if (errorMsg) {
      this.campaign.price = this.campaign_original.price;
      this.campaign.budget = this.campaign_original.budget;
      this.messageService.push(errorMsg);
      this.is_changed = false;
      return false;
    }

    /**
     * Update the price and/or budget
     */
    this.campaignService.updateCampaign(this.campaignId, this.campaign).then(() => {
      this.campaign_original.price = this.campaign.price;
      this.campaign_original.budget = this.campaign.budget;
      this.is_changed = false;
      this.messageService.push(environment.geminiUpdateSuccessful, 'success', 8000);

    }, err => {
      errorMsg = this.fetchErrorMessage(err);

      this.campaign.price = this.campaign_original.price;
      this.campaign.budget = this.campaign_original.budget;
      this.messageService.push(errorMsg, 'danger');
      this.is_changed = false;
      console.log(err.message ? err.message : JSON.stringify(err));
    });

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

      if (this.campaign.is_running) {
        this.messageService.push(environment.geminiStartSuccessful, 'success', 8000);
      } else {
        this.messageService.push(environment.geminiStopSuccessful, 'success', 8000);
      }
    }, err => {
      const errorMsg = this.fetchErrorMessage(err);
      this.messageService.push(errorMsg, 'danger');
      console.log(err.message ? err.message : JSON.stringify(err));
    });
    return false;
  }

  private fetchErrorMessage(err: any) {
    if (err.error && err.error.message) {
      return err.error.message;
    }
    if (err.error && err.error.brief) {
      return err.error.brief;
    }
    return (err.message ? err.message : JSON.stringify(err));
  }
}
