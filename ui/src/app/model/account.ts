export class Account {
  constructor(account?: Account) {
    if (account) {
      Object.assign(this, account);
    }
  }

  adv_status?: string;
  adv_name?: string;
  adv_id?: number;
  cmp_id?: number;
  store_access_token?: string;
  store_auth_uri?: string;
  yahoo_access_token?: string;
  yahoo_auth_uri?: string;

  store_token_valid? = false;
  yahoo_token_valid? = false;
  billing_valid? = false;

  hasValidTokens(): boolean {
    return this.hasValidYahooToken() && this.hasValidStoreToken();
  }

  hasValidYahooToken(): boolean {
    return (this.yahoo_access_token != null && this.yahoo_token_valid);
  }

  hasValidStoreToken(): boolean {
    return (this.store_access_token != null && this.store_token_valid);
  }
}
