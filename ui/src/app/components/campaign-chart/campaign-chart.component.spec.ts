import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CampaignChartComponent } from './campaign-chart.component';

describe('CampaignChartComponent', () => {
  let component: CampaignChartComponent;
  let fixture: ComponentFixture<CampaignChartComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CampaignChartComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CampaignChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
