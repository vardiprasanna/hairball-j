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

  env: any;
  campaign: Campaign;
  campaign_original: Campaign;
  campaign_config_loaded = false;
  campaign_config_loaded_err: any;
  is_changed = false;

  constructor(private messageService: MessageService, private campaignService: CampaignService) {
    this.env = environment;
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
    if (this.validate()) {
      this.updateAll(function (thisObj) {
        thisObj.messageService.push(environment.geminiUpdateSuccessful, 'success', 5000);
      });
    }
    return false;
  }

  public updateStatus() {
    if (this.validate()) {
      this.campaign.is_running = !this.campaign_original.is_running;

      this.updateAll(function (thisObj) {
        if (thisObj.campaign.is_running) {
          thisObj.messageService.push(environment.geminiStartSuccessful, 'success', 5000);
        } else {
          thisObj.messageService.push(environment.geminiStopSuccessful, 'success', 5000);
        }
      });
    }

    return false;
  }

  /**
   * Validate whether price and/or budget changes are valid
   * @returns true if the changes can proceed
   */
  private validate(): boolean {
    if (!this.campaign_original || !this.campaign) {
      return false; // cannot update because we're not even able to load the campaign
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
      // this.campaign.price = this.campaign_original.price;
      // this.campaign.budget = this.campaign_original.budget;
      this.campaign.is_running = this.campaign_original.is_running;
      this.messageService.push(errorMsg);
      this.change();
      return false;
    }

    return true;
  }

  /**
   * Update the price, and/or budget, and/or the status
   * @param okCallback is a callback function upon a successful update
   */
  private updateAll(okCallback) {
    /**
     * Update the price and/or budget
     */
    this.campaignService.updateCampaign(this.campaignId, this.campaign).then(() => {
      this.campaign_original.price = this.campaign.price;
      this.campaign_original.budget = this.campaign.budget;
      this.campaign_original.is_running = this.campaign.is_running;
      this.is_changed = false;
      okCallback(this);

    }, err => {
      const errorMsg = this.fetchErrorMessage(err);

      // this.campaign.price = this.campaign_original.price;
      // this.campaign.budget = this.campaign_original.budget;
      this.campaign.is_running = this.campaign_original.is_running;

      this.messageService.push(errorMsg, 'danger');
      this.change();
      console.log(err.message ? err.message : JSON.stringify(err));
    });
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
