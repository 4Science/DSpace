import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { AdminSearchPageComponent } from './admin-search-page.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ConfigurationSearchPageComponent } from '../../search-page/configuration-search-page.component';
import { ActivatedRoute } from '@angular/router';
import { ActivatedRouteStub } from '../../shared/testing/active-router.stub';

describe('AdminSearchPageComponent', () => {
  let component: AdminSearchPageComponent;
  let fixture: ComponentFixture<AdminSearchPageComponent>;

  beforeEach(waitForAsync(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminSearchPageComponent],
      providers: [
        { provide: ActivatedRoute, useValue: new ActivatedRouteStub() }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(AdminSearchPageComponent, {
      remove: {
        imports: [ConfigurationSearchPageComponent]
      }
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdminSearchPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
