import { ItemVersionHistoryComponent } from './item-version-history.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../../shared/utils/var.directive';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { Item } from '../../../core/shared/item.model';
import { ActivatedRoute } from '@angular/router';
import { of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { ItemVersionsComponent } from '../../versions/item-versions.component';

describe('ItemVersionHistoryComponent', () => {
  let component: ItemVersionHistoryComponent;
  let fixture: ComponentFixture<ItemVersionHistoryComponent>;

  const item = Object.assign(new Item(), {
    uuid: 'item-identifier-1',
    handle: '123456789/1',
  });

  const activatedRoute = {
    parent: {
      parent: {
        data: observableOf({ dso: createSuccessfulRemoteDataObject(item) }),
      },
    },
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
    imports: [
      TranslateModule.forRoot(),
      RouterTestingModule.withRoutes([]),
      ItemVersionHistoryComponent,
      VarDirective
    ],
    providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
    ],
    schemas: [NO_ERRORS_SCHEMA],
})
      .overrideComponent(ItemVersionHistoryComponent, {
        remove: {imports: [ItemVersionsComponent]},
        add: { imports: [MockItemVersionsComponent]}
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemVersionHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize the itemRD$ from the route\'s data', (done) => {
    component.itemRD$.subscribe((itemRD) => {
      expect(itemRD.payload).toBe(item);
      done();
    });
  });
});

@Component({
  selector: 'ds-item-versions',
  template: '',
  standalone: true
})
class MockItemVersionsComponent {}
