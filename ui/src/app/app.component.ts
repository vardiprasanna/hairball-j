import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CampaignService } from './services/campaign.service';
import { Account } from './model/account';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent implements OnInit {
  app_loaded = false;

  constructor(private router: Router, private campaignService: CampaignService) {
  }

  static getQueryIntParam(reg): number {
    const query = window.location.search;
    const val = reg.exec(query);
    return (val && val.length >= 1) ? Number.parseInt(val[1]) : null;
  }

  ngOnInit() {
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
    return 1643580; // TODO
  }

  private getCampaignIdParam(): number {
    const campaignId = AppComponent.getQueryIntParam(/[?/&;,]?cmp=([0-9]+)[&;,]?.*$/);
    if (campaignId) {
      return campaignId;
    }
    return 363491351; // TODO;
  }
}
