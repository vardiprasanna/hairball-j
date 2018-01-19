interface KeyedRow {
  key: string;
  val: any[];
}

export class Metric {
  headers = ['Day'
    , 'Advertiser ID'
    , 'Campaign ID'
    , 'Impressions'
    , 'Clicks'
    , 'Conversions'
    , 'Spend'
    , 'Average CPC'
    , 'Average CPM'
    , 'Source'];
  dataRows: KeyedRow[] = [];
  rollupColumn = 0;

  constructor(private cube: string = 'performance_stats', headers: string[] = null) {
    // initialize a default header row
    if (headers) {
      this.setHeaders(headers);
    }
  }

  public static dateFormat(date: Date, has_yyyy: boolean=true): string {
    const yyyy = date.getFullYear();
    let mm: any = date.getUTCMonth() + 1;
    let dd: any = date.getUTCDate();

    if (mm < 10) {
      mm = '0' + mm;
    }
    if (dd < 10) {
      dd = '0' + dd;
    }
    return has_yyyy ? `${yyyy}-${mm}-${dd}` : `${mm}-${dd}`;
  }

  /**
   * Based on a 2-dimintional arrays, take a first row as a header row, and the rest as data row
   */
  public reset(rows: any[][]) {
    this.headers = [];
    this.dataRows = [];

    if (rows && rows.length >= 1) {
      this.setHeaders(rows[0]);
      for (let i = 1; i < rows.length; i++) {
        this.addRow(rows[i]);
      }
    }

    // sort the rollup column in ascending order
    this.dataRows.sort((a: KeyedRow, b: KeyedRow) => {
      return Date.parse(a.key) - Date.parse(b.key);
    });
  }

  /**
   * Fetch a column under a given heading
   */
  public fetchColumn(name: string): any[] {
    const i = this.findColumn(new RegExp(name, 'i'));
    const col = [];

    for (const next_row of this.dataRows) {
      col.push(next_row.val[i]);
    }
    return col;
  }

  /**
   * Prepare a query to be used to retrieve a Gemini report
   */
  public prepareQuery(advertiserId: number, campaignId: number,
                      rollup: string, startDate: Date, endDate: Date): string {
    return `{
      "cube": "${this.cube}",
      "fields": ${this.prepareQueryHeaders(rollup)},
      "filters": ${this.prepareQueryFilter(advertiserId, campaignId, rollup, startDate, endDate)}
    }`;
  }

  /**
   * Each row will be indexed with its day, week, or month field
   */
  private addRow(row: string[]): void {
    const elem = {
      key: row[this.rollupColumn],
      val: row
    };
    this.dataRows.push(elem);
  }

  /**
   * The header row is used to fetch data rows. The date (day, week or month) position is saved for a quick lookup in future
   */
  private setHeaders(headers: string[], pattern = /Day|Week|Month/i): void {
    this.headers = headers;
    this.rollupColumn = this.findColumn(pattern);
  }

  private findColumn(pattern = /Day|Week|Month/i): number {
    // console.log('the column match pattern: ' + pattern);
    for (let i = 0; i < this.headers.length; i++) {
      if (this.headers[i].match(pattern)) {
        return i;
      }
    }
    throw new Error('no column match');
  }

  private prepareQueryHeaders(rollup: string): string {
    const perfHeaders = [];
    this.headers[this.rollupColumn] = rollup;

    for (const h of this.headers) {
      const m = {field: null};
      m.field = h;
      perfHeaders.push(m);
    }
    return JSON.stringify(perfHeaders);
  }

  private prepareQueryFilter(advertiserId: number, campaignId: number, rollup: string, startDate: Date, endDate: Date): string {
    if (startDate.getMilliseconds() > endDate.getMilliseconds()) {
      throw new Error('the start date cannot be later than the end date');
    }

    return `[
      { "field": "Advertiser ID", "operator": "=", "value": ${advertiserId} },
      { "field": "Campaign ID", "operator": "IN", "values": [${campaignId}] },
      { "field": "${rollup}", "operator": "between", "from": "${Metric.dateFormat(startDate)}", "to": "${Metric.dateFormat(endDate)}" }
    ]`;
  }
}
