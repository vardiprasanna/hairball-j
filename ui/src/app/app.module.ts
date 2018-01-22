import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { CampaignService } from './services/campaign.service';
import { AppComponent } from './app.component';
import { CampaignChartComponent } from './components/campaign-chart/campaign-chart.component';
import { CampaignConfigComponent } from './components/campaign-config/campaign-config.component';

@NgModule({
  declarations: [
    AppComponent,
    CampaignChartComponent,
    CampaignConfigComponent,
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    RouterModule.forRoot([

    ])
  ],
  providers: [
    CampaignService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
