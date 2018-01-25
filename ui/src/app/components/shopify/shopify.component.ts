import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';

import { CampaignService } from '../../services/campaign.service';
import { Account } from '../../model/account';

@Component({
  selector: 'app-shopify',
  templateUrl: './shopify.component.html',
  styleUrls: ['./shopify.component.css']
})
export class ShopifyComponent implements OnInit {
  shopify_loaded = false;
  shopify_loaded_err: string;

  constructor(private router: Router, private route: ActivatedRoute, private campaignService: CampaignService) {
  }

  ngOnInit() {
    const subscription = this.route
      .queryParams
      .subscribe(params => {

        if (params != null) {
          // console.log('next query: ' + JSON.stringify(params));
          this.campaignService.queryAccount(params).then(acct => {
            this.campaignService.account  = acct;
          }, err => {
            this.shopify_loaded_err = (err.message ? err.message : JSON.stringify(err));
          }).then(() => {
            this.shopify_loaded = true;
            console.log('shopify account: ' + JSON.stringify(this.campaignService.account));

            // Redirect to login page if the account is invalid
            if (this.hasValidTokens()) {
              this.router.navigateByUrl('g/campaign');
            } else {
              this.router.navigateByUrl('g/login');
            }
          });
        }
      });

    subscription.unsubscribe();
  }

  hasValidTokens(): boolean {
    const acct: Account = this.campaignService.account;
    return (acct && acct.hasValidTokens());
  }
}
