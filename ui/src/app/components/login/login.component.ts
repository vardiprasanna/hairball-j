import { Component, OnInit } from '@angular/core';
import { MessageService } from '../../services/message.service';
import { MatDialogRef } from '@angular/material/dialog';
import { ButtonConfig } from '../popup/popup.component';

import { CampaignService } from '../../services/campaign.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  login_loaded = false;
  login_loaded_err: string;

  constructor(private messageService: MessageService, private campaignService: CampaignService) {
  }

  ngOnInit() {
    if (!this.campaignService.account || !this.campaignService.account.yahoo_auth_uri) {
      console.log('yauth_default: ' + environment.yauth_default);
      this.login_loaded_err = 'it seems that the link is not from Shopify directly';
      console.log(this.login_loaded_err);
    }
    this.login_loaded = true;
  }

  /**
   * Launch Yahoo OAuth in a new window if we are embedded; otherwise launch it in our own window
   * @returns {boolean}
   */
  signIn(): boolean {
    const buttons: ButtonConfig[] = [
      {
        label: 'Cancel',
        value: ''
      },
      {
        label: 'Proceed',
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
              const win_hdl = window.open(oauthUri, '_blank');
              if (win_hdl && !win_hdl.closed) {
                win_hdl.focus();
              }
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
   * @returns {boolean}
   */
  signUp(): boolean {
    const buttons: ButtonConfig[] = [
      {
        label: 'Cancel',
        value: ''
      },
      {
        label: 'Proceed',
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

