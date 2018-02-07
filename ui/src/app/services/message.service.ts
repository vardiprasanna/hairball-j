import { Injectable } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PopupComponent, MessageConfig } from '../components/popup/popup.component';

export interface Message {
  text: string;
  cssType?: string;
}

@Injectable()
export class MessageService {
  dialogRef: any;
  messages: Message[];

  constructor(private dialog: MatDialog) {
  }

  isEmpty(): boolean {
    return !this.messages || this.messages.length === 0;
  }

  push(msg: string, type?: string): void {
    if (!this.messages) {
      this.messages = [];
    }
    if (!type) {
      type = 'warning';
    }
    this.messages.push({
      text: msg,
      cssType: type
    });
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
