import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Params } from '@angular/router';
import { Account } from '../model/account';
import { Campaign } from '../model/campaign';

@Injectable()
export class CampaignService {
  private _account: Account;
  private base_uri: string;

  constructor(private http: HttpClient) {
    console.log('campaign service creation');
    const href = window.location.href;
    const base_len = href.indexOf('/', 8);

    this.base_uri = href.substr(0, base_len);
    this.base_uri = 'http://localhost:4088'; // TODO
  }

  set account(acct: Account) {
    this._account = new Account(acct);
  }

  get account(): Account {
    return this._account;
  }

  isAccountReady() {
    return this._account && this._account.hasValidTokens();
  }

  queryShopify(query: Params): Promise<Account> {
    const path = '/g/shopify/query';
    return this.http
      .get<Account>(this.base_uri + path, {params: query})
      .toPromise();
  }

  loginShopify(query: Params): Promise<Account> {
    const path = '/g/shopify/login';
    return this.http
      .get<Account>(this.base_uri + path, {params: query})
      .toPromise();
  }

  getAccount(id: number, query?: string): Promise<Account> {
    const path = '/g/ui/account/' + id;
    return this.http
      .get<Account>(this.base_uri + path)
      .toPromise();
  }

  getCampaign(id: number, query?: string): Promise<any> {
    const path = '/g/ui/campaign/' + id;
    return this.http
      .get<Campaign>(this.base_uri + path)
      .toPromise();
  }

  getMetric(id: number, query: string): Promise<any> {
    const path = '/g/ui/reporting/' + id;
    return this.http.post(this.base_uri + path, query)
      .toPromise();
  }

  private handleError(error: any): Promise<any> {
    console.error('ERROR OCCURRED TALKING TO SERVER' + error);
    return Promise.reject(error.message || error);
  }
}
