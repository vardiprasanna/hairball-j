export class Campaign {
  adv_id: number;
  cmp_id: number;
  adv_name?: string;
  cmp_name?: string;
  cmp_status: string;
  adv_status: string;
  start_date?: Date;
  end_date?: Date;
  cr_date?: Date;
  upd_date?: Date;
  budget = 0;
  cpc = 0.0;

  get is_running() {
    return this.cmp_status && this.cmp_status === 'ACTIVE';
  }

  set is_running(running: boolean) {
    this.cmp_status = (running ? 'ACTIVE' : 'PAUSED');
  }

  constructor() {
  }
}
