import { Component, OnInit, OnDestroy } from '@angular/core';
import { MessageService } from '../../services/message.service';
import { Subscription } from 'rxjs/Subscription';
import { TimerObservable } from 'rxjs/observable/TimerObservable';
import { MatDialogRef } from '@angular/material/dialog';
import { ButtonConfig } from '../popup/popup.component';

import { CampaignService } from '../../services/campaign.service';
import { Account } from '../../model/account';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy {
  login_loaded = false;
  login_loaded_err: string;
  oath_win_hdl: any;
  subscription: Subscription;

  constructor(private messageService: MessageService, private campaignService: CampaignService) {
  }

  ngOnInit() {
    if (this.campaignService.account && this.campaignService.account.yahoo_auth_uri) {
      console.log('yahoo_auth_uri: ' + this.campaignService.account.yahoo_auth_uri);

      const timer = TimerObservable.create(2000, 1000);
      this.subscription = timer.subscribe(() => {
        if (this.oath_win_hdl && this.oath_win_hdl.closed) {
          window.focus();
        }
      });
    } else {
      console.log('yauth_default: ' + environment.yauth_default);
      this.login_loaded_err = 'it seems that the link is not from Shopify directly';
      console.log(this.login_loaded_err);
    }
    this.login_loaded = true;
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
  }

  /**
   * Launch Yahoo OAuth in a new window if we are embedded; otherwise launch it in our own window
   * @param {Event} event
   * @returns {boolean}
   */
  signIn(event: Event): boolean {
    const buttons: ButtonConfig[] = [
      {
        label: 'Cancel',
        value: ''
      },
      {
        label: 'Confirm',
        value: 'confirm'
      }];

    const dialogRef: MatDialogRef<any> = this.messageService.show(environment.geminiSigInMessage, buttons);
    if (dialogRef) {
      dialogRef.afterClosed().subscribe(c => {
          if (c === 'confirm') {
            const isEmbedded = !(window === window.parent);
            let oauthUri: string = null;

            if (this.campaignService.account) {
              oauthUri = this.campaignService.account.yahoo_auth_uri;
            }
            if (!oauthUri) {
              oauthUri = environment.yauth_default; // likely user opens our app outside Shopify admin console
              console.log('use a default yauth url: ' + oauthUri);
            }

            if (isEmbedded) {
              // Due to the same-origin constraint, let's open a top level window
              this.oath_win_hdl = window.open(oauthUri, '_blank');
              this.oath_win_hdl.focus();
            } else {
              window.open(oauthUri, '_self');
            }
          }
        }
      );
    }
    return false;
  }

  /**
   * Launch Gemini in a new window for a user to create an account
   * @param {Event} event
   * @returns {boolean}
   */
  signUp(event: Event): boolean {
    const buttons: ButtonConfig[] = [
      {
        label: 'Cancel',
        value: ''
      },
      {
        label: 'Confirm',
        value: 'confirm'
      }];

    const dialogRef: MatDialogRef<any> = this.messageService.show(environment.geminiSigUpMessage, buttons);
    if (dialogRef) {
      dialogRef.afterClosed().subscribe(c => {
          if (c === 'confirm') {
            const gwin = window.open(environment.geminiHomeUrl, '_blank');
            if (gwin && gwin.focus) {
              gwin.focus();
            }

          }
        }
      );
    }
    return false;
  }
}

