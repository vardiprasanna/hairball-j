import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Params } from '@angular/router';
import { Account } from '../model/account';
import { Campaign } from '../model/campaign';
import { environment } from '../../environments/environment';

@Injectable()
export class CampaignService {
  private _account: Account;
  private base_uri: string;

  constructor(private http: HttpClient) {
    this.base_uri = environment.ewsBaseUrl;

    // Figure out the base from where this app is running
    if (!this.base_uri) {
      const href = window.location.href;
      const base_len = href.indexOf('/', 8);
      this.base_uri = href.substr(0, base_len);
    } else if (this.base_uri.endsWith('/')) {
      this.base_uri = this.base_uri.substr(0, this.base_uri.length - 1);
    }
  }

  set account(acct: Account) {
    // if (!this.isAccountChanged(acct)) {
    //   return;
    // }
    this._account = new Account(acct);
    if (this.isAccountReady()) {
      acct.last_access = new Date();
    }
    if (acct) {
      const account = JSON.stringify(acct);

      if (window.sessionStorage) {
        window.sessionStorage.setItem('geminiDpaAccount', account);
      }
      // if (window.localStorage) {
      //   console.log('ready to persist acct to local storage');
      //   window.localStorage.setItem('geminiDpaAccount', account);
      // }
    }
  }

  get account(): Account {
    if (!this._account && window.sessionStorage) {
      const cachedAcct = window.sessionStorage.getItem('geminiDpaAccount');
      if (cachedAcct) {
        try {
          this._account = new Account(JSON.parse(cachedAcct));
        } catch (err) {
          console.log('cached acct: ' + err);
        }
      }
    }

    return this._account;
  }

  isAccountReady() {
    return this._account && this._account.hasValidTokens() && this._account.adv_id;
  }

  queryShopify(query: Params): Promise<Account> {
    const path = '/g/shopify/query';
    return this.http
      .get<Account>(this.base_uri + path, {params: query})
      .toPromise();
  }

  signInYahoo(query: Params): Promise<Account> {
    const path = '/g/shopify/yauth';
    return this.http
      .get<Account>(this.base_uri + path, {params: query})
      .toPromise();
  }

  signInShopify(query: Params): Promise<Account> {
    const path = '/g/shopify/sauth';
    return this.http
      .get<Account>(this.base_uri + path, {params: query})
      .toPromise();
  }

  getAccount(id: number, query?: string): Promise<Account> {
    const path = '/g/ui/account/' + id + this.appendTokens();
    return this.http
      .get<Account>(this.base_uri + path)
      .toPromise();
  }

  getCampaign(id: number, query?: string): Promise<any> {
    const path = '/g/ui/campaign/' + id + this.appendTokens();
    return this.http
      .get<Campaign>(this.base_uri + path)
      .toPromise();
  }

  getMetric(id: number, query: string): Promise<any> {
    const path = '/g/ui/reporting/' + id + this.appendTokens();
    return this.http.post(this.base_uri + path, query)
      .toPromise();
  }

  updateCampaign(id: number, cmp: Campaign): Promise<any> {
    const path = '/g/ui/campaign/' + id;
    return this.http
      .put<Campaign>(this.base_uri + path, cmp)
      .toPromise();
  }

  appendTokens(): string {
    const acct = this.account;
    let query = '';

    if (acct) {
      if (acct.store_access_token) {
        query += '&st=' + acct.store_access_token;
      }
      if (acct.yahoo_access_token) {
        query += '&yt=' + acct.yahoo_access_token;
      }
      if (acct.shop) {
        query += '&shop=' + acct.shop;
      }
    }
    if (query.startsWith('&')) {
      query = '?' + query.substring(1);
    }
    return query;
  }

  isAccountChanged(acct: Account): boolean {
    if (!acct || !this._account) {
      console.log('account is changed due to nullness');
      return true;
    }
    for (const name in acct) {
      if (!acct.hasOwnProperty(name) || name === 'last_access') {
        continue;
      }
      if (acct[name] !== this._account[name]) {
        console.log('account is changed due to "' + name + '"=' + acct[name] + ' vs ' + this._account[name]);
        return true;
      }
    }
    return false;
  }
}
