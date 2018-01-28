import { Component, OnInit, OnDestroy } from '@angular/core';
import { CampaignService } from '../../services/campaign.service';
import { MessageService } from '../../services/message.service';
import { Subscription } from 'rxjs/Subscription';
import { TimerObservable } from 'rxjs/observable/TimerObservable';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { ButtonConfig } from '../popup/popup.component';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy {
  oauth_win_url = 'https://api.login.yahoo.com/oauth2/request_auth?response_type=code&language=en-us&client_id=' +
    'dj0yJmk9NEJVRHRaRnpWa09SJmQ9WVdrOVREQktiREUzTjJrbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iYQ--&redirect_uri=' +
    'https%3A%2F%2Fhairball.herokuapp.com%2Fg%2Fshopify%2Fews%3F_mc%3D0bc7f4694ab72663057da0eb5f14d52b%26shop%3Ddpa-bridge.myshopify.com';

  oath_win_hdl: any;
  tick: any;
  subscription: Subscription;
  name = 'dummy name';

  constructor(private campaignService: CampaignService, private messageService: MessageService, private dialog: MatDialog) {
  }

  ngOnInit() {
    const timer = TimerObservable.create(2000, 1000);
    this.subscription = timer.subscribe(t => {
      this.tick = t;
      if (this.oath_win_hdl) {
        this.tick += '' + (this.oath_win_hdl.closed ? ' closed' : ' opened');
        if (this.oath_win_hdl.closed) {
          window.focus();
        } else {

        }
      }
    });
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
    this.oath_win_hdl = window.open(this.oauth_win_url);
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
            window.open(environment.geminiHomeUrl, 'gemini_shopify_signup');
          }
        }
      );
    }
    return false;
  }
}

