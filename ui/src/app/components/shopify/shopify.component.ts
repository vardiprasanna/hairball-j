import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';

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
          console.log('next query: ' + JSON.stringify(params));

          if (!params['shop']) {
            this.shopify_loaded = true;
            this.router.navigateByUrl('f/login');
          }
          if (params['hmac']) {
            // This happens when the call orignates from Shopify
            this.routeShopify(this.campaignService.queryShopify(params));
          } else {
            // Yahoo oAuth will come here after either user grant or deny our Gemini access
            if (params['error']) {
              this.shopify_loaded_err = params['error']; // TODO - pass this error to login page
            } else if (params['code']) {
              // This happens when the call orignates from Shopify
              this.routeShopify(this.campaignService.loginShopify(params));
            }
          }
        }
      });

    subscription.unsubscribe();
  }

  private routeShopify(invoker: Promise<Account>): void {
    invoker.then(acct => {
      this.campaignService.account = acct;
    }, err => {
      this.shopify_loaded_err = (err.message ? err.message : JSON.stringify(err));
    }).then(() => {
      this.shopify_loaded = true;
      const loc = (this.campaignService.isAccountReady() ? 'f/campaign' : 'f/login');
      this.router.navigateByUrl(loc, {skipLocationChange: false});
    });
  }
}
