import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Account } from '../model/account';
import { Campaign } from '../model/campaign';

@Injectable()
export class CampaignService {
  account: Account;
  base_uri: string;

  constructor(private http: HttpClient) {
    console.log('campaign service creation');
    const href = window.location.href;
    const base_len = href.indexOf('/', 8);

    this.base_uri = href.substr(0, base_len);
    this.base_uri = 'http://localhost:4080'; // TODO
  }

  getAccount(id: number, query?: string): Promise<any> {
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
