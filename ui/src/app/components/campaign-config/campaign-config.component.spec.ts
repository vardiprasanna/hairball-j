import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CampaignConfigComponent } from './campaign-config.component';

describe('CampaignConfigComponent', () => {
  let component: CampaignConfigComponent;
  let fixture: ComponentFixture<CampaignConfigComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CampaignConfigComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CampaignConfigComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
