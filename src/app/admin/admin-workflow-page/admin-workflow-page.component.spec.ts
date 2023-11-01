import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { AdminWorkflowPageComponent } from './admin-workflow-page.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ConfigurationSearchPageComponent } from '../../search-page/configuration-search-page.component';

describe('AdminSearchPageComponent', () => {
  let component: AdminWorkflowPageComponent;
  let fixture: ComponentFixture<AdminWorkflowPageComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [AdminWorkflowPageComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(AdminWorkflowPageComponent, {
        remove: {
          imports: [
            ConfigurationSearchPageComponent
          ]
        }
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdminWorkflowPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
