import { ComponentFixture, TestBed } from '@angular/core/testing';

import {
  ItemAccessControlSelectBitstreamsModalComponent
} from './item-access-control-select-bitstreams-modal.component';

// TODO: enable this test suite and fix it
xdescribe('ItemAccessControlSelectBitstreamsModalComponent', () => {
  let component: ItemAccessControlSelectBitstreamsModalComponent;
  let fixture: ComponentFixture<ItemAccessControlSelectBitstreamsModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ItemAccessControlSelectBitstreamsModalComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemAccessControlSelectBitstreamsModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
