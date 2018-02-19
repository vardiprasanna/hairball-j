import { Injectable } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MessageConfig, PopupComponent } from '../components/popup/popup.component';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';

export interface Message {
  text: string;
  severity?: string;
  duration?: number;
}

@Injectable()
export class MessageService {
  dialogRef: any;
  messages: Message[];
  alertBroadcaster: Subject<Message>;

  constructor(private dialog: MatDialog) {
    this.alertBroadcaster = new Subject<Message>();
  }

  on(): Observable<Message> {
    return this.alertBroadcaster.asObservable();
  }

  isEmpty(): boolean {
    return !this.messages || this.messages.length === 0;
  }

  /**
   * A text message to be displayed
   *
   * @param {string} msg
   * @param {string} type indicates the seriousness of the message
   * @param {number} duration is how long the display lasts before it is closed automatically in milliseconds
   */
  push(msg: string, type?: string, duration?: number): void {
    if (!this.messages) {
      this.messages = [];
    }
    if (!type) {
      type = 'warning';
    }

    const typedMessage: Message = {
      text: msg,
      severity: type,
      duration: duration
    };
    this.messages.push(typedMessage);
    this.alertBroadcaster.next(typedMessage);
  }

  pop(): Message {
    return this.isEmpty() ? null : this.messages.pop();
  }

  /**
   * Open a popup to show a top message if no message data is passed in
   * @returns {MatDialogRef} if there is message to show; null otherwise
   */
  show(msg?: string, buttons?: any[], title?: string, alertType?: string): MatDialogRef<PopupComponent> {
    if (!msg) {
      const queuedMsg = this.pop();
      if (queuedMsg) {
        msg = queuedMsg.text;
      }
    }
    if (msg) {
      const messageConfig: MessageConfig = {
        content: msg
      };
      if (buttons) {
        messageConfig.buttons = buttons;
      }
      if (title) {
        messageConfig.title = title;
      }
      if (alertType) {
        messageConfig.alertType = alertType;
      }

      return this.dialogRef = this.dialog.open(PopupComponent, {
        width: '600px',
        data: messageConfig,
        panelClass: 'gemini-popup-warning',
        position: {top: '5em'},
      });
    }
    return null;
  }
}
