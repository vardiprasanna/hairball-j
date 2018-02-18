import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { CampaignService } from '../../services/campaign.service';
import { MessageService } from '../../services/message.service';
import { Account } from '../../model/account';
import 'rxjs/add/operator/takeWhile';

@Component({
  selector: 'app-shopify',
  templateUrl: './shopify.component.html',
  styleUrls: ['./shopify.component.css']
})
export class ShopifyComponent implements OnInit {
  shopify_loaded = false;
  shopify_loaded_err: string;

  constructor(private router: Router, private route: ActivatedRoute, private messageService: MessageService, private campaignService: CampaignService) {
  }

  ngOnInit() {
    const subscription = this.route
      .queryParams
      .takeWhile(() => this.shopify_loaded === false)
      .subscribe(params => {
        console.log('next query in shopify.component: ' + JSON.stringify(params));

        if (params == null /*|| !params['shop']*/) {
          this.shopify_loaded = true;
          this.shopify_loaded_err = 'incorrect query'; // TODO - pass this error to login page
          this.router.navigateByUrl('f/login');
        }

        this.route.data.forEach(d => {
          console.log('shopify reason: ' + JSON.stringify(d));

          switch (d.reason) {
            case 'welcome':
              this.welcome(params);
              break;
            case 'home':
              this.home(params);
              break;
            case 'yauth':
              this.yoauth(params);
              break;
          }
        });
      });
  }

  /**
   * Lands here when a Shopify user installs or views our app
   */
  private welcome(params: Params) {
    if (params['hmac']) {
      if (this.campaignService.isAccountReady()) {
        this.shopify_loaded = true;
        this.redirectForShopifyAccess();
      } else {
        // This happens when the call orignates from Shopify
        this.afterWelcome(params, this.campaignService.queryShopify(params));
      }
    } else {
      this.router.navigateByUrl('f/login', {skipLocationChange: true});
    }
  }

  /**
   * Lands here when a Shopify user grants or denies our app the access of Shopify during installation
   */
  private home(params: Params) {
    if (params['hmac']) {
      if (this.campaignService.isAccountReady()) {
        this.shopify_loaded = true;
        this.router.navigateByUrl('f/campaign', {skipLocationChange: true});
      } else {
        // This happens when the call orignates from Shopify
        this.afterHome(params, this.campaignService.signInShopify(params));
      }
    } else {
      this.router.navigateByUrl('f/login', {skipLocationChange: true});
    }
  }

  /**
   * Lands here when a Shopify user grants or denies our app the access of Gemini during installation
   */
  private yoauth(params: Params) {
    if (params['error']) {
      this.shopify_loaded_err = params['error']; // TODO - pass this error to login page
      this.router.navigateByUrl('f/login', {skipLocationChange: true});
    } else if (params['code']) {
      // This happens when user grants Gemini access
      this.afterYAuth(params, this.campaignService.signInYahoo(params));
    } else {
      this.router.navigateByUrl('f/login', {skipLocationChange: true});
    }
  }

  /**
   * Onboard step 1 - post operation after user lands here
   */
  private afterWelcome(params: Params, invoker: Promise<Account>): void {
    invoker.then(acct => {
      this.campaignService.account = acct;
    }, err => {
      console.log('afterWelcome err: ' + JSON.stringify(err));
      this.shopify_loaded_err = (err.message ? err.message : JSON.stringify(err));
    }).then(() => {
      this.shopify_loaded = true;

      // User may install and uninstall our app n-number of times. So the account may come from a previous installation, so we should ask user's permission
      if (this.campaignService.isAccountReady()) {
        this.redirectForShopifyAccess();
      } else {
        this.router.navigateByUrl('f/login', {skipLocationChange: true});
      }
    });
  }

  /**
   * Onboard step 2 - post operation after ask user to sign in Yahoo OAuth
   */
  private afterYAuth(params: Params, invoker: Promise<Account>): void {
    invoker.then(acct => {
      this.campaignService.account = acct;
    }, err => {
      console.log('afterYAuth err: ' + JSON.stringify(err));
      this.shopify_loaded_err = (err.message ? err.message : JSON.stringify(err));
      this.messageService.push(err.error);
    }).then(() => {
      const acct = this.campaignService.account;
      this.shopify_loaded = true;

      console.log('afterYAuth with acct: ' + JSON.stringify(acct));

      // User may install and uninstall our app n-number of times. So the account may come from a previous installation, so we should ask user's permission
      if (acct && acct.hasValidYahooToken()) {
        this.redirectForShopifyAccess();
      } else {
        this.router.navigateByUrl('f/login', {skipLocationChange: true});
      }
    });
  }

  /**
   * Onboard step 3 - post operation after ask user for the permisson of accessing Shopify products, etc
   */
  private afterHome(params: Params, invoker: Promise<Account>): void {
    invoker.then(acct => {
      this.campaignService.account = acct;
    }, err => {
      console.log('afterHome err: ' + JSON.stringify(err));
      this.shopify_loaded_err = (err.message ? err.message : JSON.stringify(err));
    }).then(() => {
      const acct = this.campaignService.account;
      this.shopify_loaded = true;

      console.log('afterHome with acct: ' + JSON.stringify(acct));
      const loc = (this.campaignService.isAccountReady() ? 'f/campaign' : 'f/login');
      this.router.navigateByUrl(loc, {skipLocationChange: true});
    });
  }

  /**
   * Upon successful grant of the access, Shopify will redirect to 'f/shopify/home'
   */
  private redirectForShopifyAccess() {
    const acct = this.campaignService.account;
    console.log('look good, and ask shopify for access: ' + acct.store_auth_uri);

    // window.location.href = redirect;
    if (acct && acct.store_auth_uri) {
      window.open(acct.store_auth_uri, '_self');
    } else if (this.campaignService.isAccountReady()) {
      this.router.navigateByUrl('f/campaign', {skipLocationChange: true});
    } else {
      this.shopify_loaded_err = 'missing of shopify auth url';
      this.router.navigateByUrl('f/login', {skipLocationChange: true});
    }
  }
}
