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
  oath_win_hdl: any;
  subscription: Subscription;

  constructor(private campaignService: CampaignService, private messageService: MessageService, private dialog: MatDialog) {
  }

  ngOnInit() {
    const timer = TimerObservable.create(2000, 1000);
    this.subscription = timer.subscribe(() => {
        if (this.oath_win_hdl && this.oath_win_hdl.closed) {
          window.focus();
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

            if (isEmbedded) {
              // Due to the same-origin constraint, let's open a top level window
              this.oath_win_hdl = window.open(environment.oauthUrl, '_blank');
              this.oath_win_hdl.focus();
            } else {
              window.open(environment.oauthUrl, '_self');
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

