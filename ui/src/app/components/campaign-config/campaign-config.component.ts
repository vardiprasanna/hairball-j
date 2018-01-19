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
  campaign_loaded = false;

  constructor(private ewsService: CampaignService) {
    this.campaign = new Campaign();
  }

  ngOnInit() {
    let query = window.location.search;
    if (query.charAt(0) !== '?') {
      query = '?' + query;
    }

    this.ewsService.getCampaign(this.campaignId, query).then(cmp => {
      console.log('raw campaign: ' + JSON.stringify(cmp));

      this.campaign = {
        budget: cmp.budget,
        cpc: cmp.price,
        start_date: new Date(cmp.startDateInMilli),
        end_date: new Date(cmp.endDateInMilli),
        is_running: (cmp.status === 'ACTIVE')
      };
      this.campaign_loaded = true;
      console.log('campaign: ' + JSON.stringify(this.campaign));
    }); // .catch(this.handleError);
  }

  public updateCost($event) {
    console.log(event + ', ' + JSON.stringify(this.campaign));
  }
}
