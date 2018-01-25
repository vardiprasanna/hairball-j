export class Account {
  constructor(account?: Account) {
    if (account) {
      Object.assign(this, account);
    }
  }

  adv_status?: string;
  adv_name?: string;
  adv_id?: number;
  store_access_token?: string;
  yahoo_access_token?: string;

  store_token_valid? = false;
  yahoo_token_valid? = false;
  billing_valid? = false;

  hasValidTokens(): boolean {
    return this.store_token_valid && this.yahoo_token_valid;
  }
}
