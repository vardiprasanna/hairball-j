import { Component, OnInit } from '@angular/core';
import { CampaignService } from '../../services/campaign.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  constructor(private campaignService: CampaignService) { }

  ngOnInit() {
  }

}
