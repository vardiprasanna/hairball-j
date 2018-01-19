import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent implements OnInit {
  cmpId: number;
  advId: number;

  constructor(private route: ActivatedRoute) {
  }

  ngOnInit() {
    this.cmpId = this.getCampaignIdParam();
    this.advId = this.getAdvertiserIdParam();
  }

  private getQueryParam(pattern) {

  }

  private getAdvertiserIdParam(): number {
    const reg = /[?/&;,]?adv=([0-9]+)[&;,]?.*$/
    const query = window.location.search;
    const advertiserIds = reg.exec(query);

    if (advertiserIds && advertiserIds.length >= 1) {
      return Number.parseInt(advertiserIds[1]);
    }
    return 1643580; // TODO
  }

  private getCampaignIdParam(): number {
    const reg = /[?/&;,]?cmp=([0-9]+)[&;,]?.*$/
    const query = window.location.search;
    const campaignIds = reg.exec(query);

    if (campaignIds && campaignIds.length >= 1) {
      return Number.parseInt(campaignIds[1]);
    }
    return 363491351; // TODO;
  }
}
