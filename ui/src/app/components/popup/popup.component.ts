import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { MAT_DIALOG_DATA } from '@angular/material';

export interface ButtonConfig {
  label: string;
  value: string;
}

export interface MessageConfig {
  content: string;
  buttons?: ButtonConfig[];
  title?: string;
  alertType?: string; // info, warning, error
}

/**
 * @see https://material.angular.io/
 */
@Component({
  selector: 'app-popup',
  templateUrl: './popup.component.html',
  styleUrls: ['./popup.component.css']
})
export class PopupComponent implements OnInit {
  display = 'none';

  constructor(private dialogRef: MatDialogRef<any>, @Inject(MAT_DIALOG_DATA) public messageConfig: MessageConfig) {
  }

  openModal(event: Event) {
    this.display = 'block';
    return false;
  }

  onCloseHandled() {
    this.display = 'none';
    return false;
  }

  ngOnInit() {
  }
}
