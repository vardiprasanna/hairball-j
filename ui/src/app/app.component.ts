import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CampaignService } from './services/campaign.service';
import { Account } from './model/account';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent implements OnInit, OnDestroy {
  subscription: any;
  app_loaded = false;

  constructor(private router: Router, private route: ActivatedRoute, private campaignService: CampaignService) {
  }

  static getQueryIntParam(reg): number {
    const query = window.location.search;
    const val = reg.exec(query);
    return (val && val.length >= 1) ? Number.parseInt(val[1]) : null;
  }

  ngOnInit() {
    this._ngOnInit();

    if (this.campaignService.isAccountReady()) {
      this.router.navigateByUrl('f/campaign');
      return;
    }

    this.subscription = this.route
      .queryParams
      .subscribe(params => {
        if (params != null) {
          console.log('next query in app.component: ' + JSON.stringify(params));
          let redirect = params['route'];
          // this.app_loaded = true;

          if (redirect) {
            let query: string = null;

            for (const name in params) {
              if (!params.hasOwnProperty(name)) {
                continue;
              }
              if (query) {
                query += '&' + name + '=' + params[name];
              } else {
                query = '?' + name + '=' + params[name];
              }
            }
            if (query) {
              redirect += query;
            }

            this.router.navigateByUrl(redirect);
          }
        }
      });
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  /**
   * TODO - use 'router' for parameters; and persist the account in storage
   */
  _ngOnInit() {
    let campaignId = this.getCampaignIdParam();
    let advertiserId = this.getAdvertiserIdParam();

    // Use a locally cached account if necessary
    if (!campaignId && !advertiserId) {
      let account: any;

      if (window.sessionStorage) {
        account = window.sessionStorage.getItem('geminiDpaAccount');
      }
      // if (!account && window.localStorage) {
      //   account = window.localStorage.getItem('geminiDpaAccount');
      // }
      if (account) {
        try {
          const acct: Account = JSON.parse(account);
          advertiserId = acct.adv_id;
          campaignId = acct.cmp_id;
        } catch (err) {
          console.log(err.getMessages());
        }
      }
    }

    if (campaignId && advertiserId) {
      // Check whether an account can be retrieved via URL parameters
      this.campaignService.getAccount(advertiserId).then(acct => {
        if (acct) {
          console.log('got acct: ' + JSON.stringify(acct));
          this.campaignService.account = acct;
        }
      }, err => {
        console.log(err.message ? err.message : JSON.stringify(err));
      }).then((() => {
        this.app_loaded = true;
      }));
    } else {
      this.app_loaded = true;
    }
  }

  private getAdvertiserIdParam(): number {
    const advertiserId = AppComponent.getQueryIntParam(/[?/&;,]?adv=([0-9]+)[&;,]?.*$/);
    if (advertiserId) {
      return advertiserId;
    }
    return null; // 1643580; // TODO
  }

  private getCampaignIdParam(): number {
    const campaignId = AppComponent.getQueryIntParam(/[?/&;,]?cmp=([0-9]+)[&;,]?.*$/);
    if (campaignId) {
      return campaignId;
    }
    return null; // 364670647; // TODO;
  }
}
