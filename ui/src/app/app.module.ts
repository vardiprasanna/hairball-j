import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { MatButtonModule, MatCheckboxModule } from '@angular/material';
import { MatDialogModule } from '@angular/material/dialog';

import { CampaignService } from './services/campaign.service';
import { MessageService } from './services/message.service';
import { AppComponent } from './app.component';
import { CampaignChartComponent } from './components/campaign-chart/campaign-chart.component';
import { CampaignConfigComponent } from './components/campaign-config/campaign-config.component';
import { CampaignComponent } from './components/campaign/campaign.component';
import { LoginComponent } from './components/login/login.component';
import { ShopifyComponent } from './components/shopify/shopify.component';
import { PopupComponent } from './components/popup/popup.component';

@NgModule({

  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ],
  declarations: [
    AppComponent,
    CampaignChartComponent,
    CampaignConfigComponent,
    CampaignComponent,
    LoginComponent,
    ShopifyComponent,
    PopupComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,

    RouterModule.forRoot([
      {
        path: 'f/campaign',
        component: CampaignComponent,
      },
      {
        path: 'f/login',
        component: LoginComponent,
      },
      {
        path: 'f/install',
        component: LoginComponent,
        data: {
          installation: true
        }
      },
      {
        path: 'f/shopify/welcome',
        component: ShopifyComponent,
        data: {reason: 'welcome'}
      },
      {
        path: 'f/shopify/home',
        component: ShopifyComponent,
        data: {reason: 'home'}
      },
      {
        path: 'f/shopify/ews',
        component: ShopifyComponent,
        data: {reason: 'yauth'}
      },
      {
        path: 'index.html',
        redirectTo: 'f/campaign',
      },
      {
        path: '',
        redirectTo: 'f/campaign',
        pathMatch: 'full'
      }
    ], {enableTracing: false})
  ],
  exports: [
    RouterModule,
    MatButtonModule,
    MatCheckboxModule,
  ],
  providers: [
    CampaignService,
    MessageService
  ],
  bootstrap: [AppComponent],
  entryComponents: [PopupComponent]
})
export class AppModule {
}
