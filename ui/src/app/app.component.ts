import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router, NavigationStart } from '@angular/router';
import { CampaignService } from './services/campaign.service';
import { MessageService } from './services/message.service';
import { Account } from './model/account';
import { environment } from '../environments/environment';
import 'rxjs/add/operator/take';
import { TimerObservable } from 'rxjs/observable/TimerObservable';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent implements OnInit, OnDestroy {
  messageService: MessageService;
  environment: any;
  subscription: any;
  app_loaded = false;
  alert_css: string;
  alert_msg: string;
  is_shopify = false;

  constructor(private router: Router, private route: ActivatedRoute, private campaignService: CampaignService, messageService: MessageService) {
    this.messageService = messageService; // workaround an angular bug by redefining this var locally
    this.environment = environment;

    this.messageService.on()
      .subscribe(msg => {
        this.messageService.pop();
        this.alert_css = 'alert app-alert alert-' + msg.severity;
        this.alert_msg = msg.text;

        if (msg.duration && msg.duration > 0) {
          // const wait = (msg.duration < 1000 ? 1000 : msg.duration);
          TimerObservable.create(msg.duration).subscribe(() => {
            this.alert_msg = null;
          });
        }
      });
  }

  static getQueryIntParam(reg): number {
    const query = window.location.search;
    const val = reg.exec(query);
    return (val && val.length >= 1) ? Number.parseInt(val[1]) : null;
  }

  ngOnInit() {
    this._ngOnInit().then(() => {
      if (this.campaignService.isAccountReady()) {
        this.router.navigateByUrl('f/campaign', {skipLocationChange: true});
        this.app_loaded = true;
        return;
      }

      this.subscription = this.route
        .queryParams
        .subscribe(params => {
          if (params != null) {
            console.log('next query in app.component: ' + JSON.stringify(params));
            let redirect = params['route'];

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

              this.is_shopify = redirect.startsWith('f/shopify/');
              this.router.navigateByUrl(redirect, {skipLocationChange: true});

              // A sub component may reroute to a non-shopify path, so we should update the flag accordingly
              if (this.is_shopify) {
                this.router.events.subscribe((event: NavigationStart) => {
                  if (event instanceof NavigationStart) {
                    const start = event.url.indexOf('f/shopify/');
                    this.is_shopify = (start === 0 || start === 1);
                  }
                });
              }
            }
          }
          this.app_loaded = true;
        });
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
  _ngOnInit(): Promise<boolean> {
    let campaignId = this.getCampaignIdParam();
    let advertiserId = this.getAdvertiserIdParam();
    let cachedAcct: Account = null;

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
          cachedAcct = JSON.parse(account);
          advertiserId = cachedAcct.adv_id;
          campaignId = cachedAcct.cmp_id;
        } catch (err) {
          console.log('cached acct: ' + err);
        }
      }
    }

    const promise: Promise<boolean> = new Promise((resolve, reject) => {
      if (campaignId && advertiserId) {
        // Check whether an account can be retrieved via URL parameters
        this.campaignService.getAccount(advertiserId).then(acct => {
          if (acct && cachedAcct && acct.cmp_id === cachedAcct.cmp_id && acct.adv_id === cachedAcct.adv_id) {
            console.log('got acct: ' + JSON.stringify(acct) + ', and its cached: ' + JSON.stringify(cachedAcct));
            this.campaignService.account = cachedAcct;
          }
        }, err => {
          if (this.campaignService.account) {
            this.campaignService.account.yahoo_token_valid = false;
          }
          console.log(err.message ? err.message : JSON.stringify(err));
        }).then((() => {
          resolve(true);
        }));
      } else {
        if (!this.campaignService.account && cachedAcct) {
          this.campaignService.account = cachedAcct;
          this.campaignService.account.adv_id = null;
          this.campaignService.account.cmp_id = null;
        }
        resolve(true);
      }
    });

    return promise;
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

  /**
   * Bring up a new window with a desired width and height
   */
  openwin(url, name, w, h) {
    const left = window.screenLeft + 100;
    const top = window.screenTop + 60;
    const availW = screen.availWidth - left - 20;
    const availH = screen.availHeight - top - 20;

    if (w >= availW) {
      w = availW;
    }
    if (h >= availH) {
      h = availH;
    }

    window.open(url, name, `left=${left},top=${top},width=${w},height=${h}`);
    return false;
  }

  hideAlert(): boolean {
    this.alert_msg = null;
    return false;
  }
}
