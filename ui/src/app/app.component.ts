import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CampaignService } from './services/campaign.service';

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
    this.subscription.unsubscribe();
  }

  /**
   * TODO - use 'router' for parameters; and persist the account in storage
   */
  _ngOnInit() {
    const campaignId = this.getCampaignIdParam();
    const advertiserId = this.getAdvertiserIdParam();

    // Use a locally cached account if necessary
    if (!campaignId && !advertiserId) {
      let account: any;

      if (window.sessionStorage) {
        account = window.sessionStorage.getItem('geminiDpaApp');
      }
      if (!account && window.localStorage) {
        account = window.sessionStorage.getItem('geminiDpaApp');
      }
      if (account) {
        try {
          this.campaignService.account = JSON.parse(account);
        } catch (err) {
          console.log(err.getMessages());
        }
      }
      return this.loginIfRequired();
    }

    // Check whether an account can be retrieved via URL parameters
    this.campaignService.getAccount(advertiserId).then(acct => {
      if (acct) {
        console.log('got acct: ' + JSON.stringify(acct));
        this.campaignService.account = acct;
      }
      this.loginIfRequired();

    }, err => {
      this.loginIfRequired();
      console.log(err.message ? err.message : JSON.stringify(err));
    });
  }

  private loginIfRequired(): void {
    this.app_loaded = true;
    if (!this.campaignService.account) {
      //   this.router.navigateByUrl('login');
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
