import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import {
  ChangeDetectionStrategy,
  Component,
  Inject,
  Injector,
  NO_ERRORS_SCHEMA,
} from '@angular/core';
import { AbstractMenuSectionComponent } from './abstract-menu-section.component';
import { MenuService } from '../menu.service';
import { MenuServiceStub } from '../../testing/menu-service.stub';
import { of as observableOf } from 'rxjs';
import { LinkMenuItemComponent } from '../menu-item/link-menu-item.component';
import { MenuSection } from '../menu-section.model';

@Component({
  selector: 'ds-some-menu-section',
  template: '',
})
class SomeMenuSectionComponent extends AbstractMenuSectionComponent {
  constructor(
    @Inject('sectionDataProvider') protected section: MenuSection,
    protected menuService: MenuService,
    protected injector: Injector,
  ) {
    super(menuService, injector);
  }
}

describe('MenuSectionComponent', () => {
  let comp: AbstractMenuSectionComponent;
  let fixture: ComponentFixture<AbstractMenuSectionComponent>;
  let menuService: MenuService;
  let dummySection;

  beforeEach(waitForAsync(() => {
    dummySection = {
      id: 'section',
      visible: true,
      active: false
    } as any;
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      declarations: [AbstractMenuSectionComponent],
      providers: [
        { provide: Injector, useValue: {} },
        { provide: MenuService, useClass: MenuServiceStub },
        { provide: 'sectionDataProvider', useValue: dummySection },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(SomeMenuSectionComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SomeMenuSectionComponent);
    comp = fixture.componentInstance;
    menuService = (comp as any).menuService;
    spyOn(comp as any, 'getMenuItemComponent').and.returnValue(LinkMenuItemComponent);
    spyOn(comp as any, 'getItemModelInjector').and.returnValue(observableOf({}));
    fixture.detectChanges();
  });

  describe('toggleSection', () => {
    beforeEach(() => {
      spyOn(menuService, 'toggleActiveSection');
      comp.toggleSection(new Event('click'));
    });
    it('should trigger the toggleActiveSection function on the menu service', () => {
      expect(menuService.toggleActiveSection).toHaveBeenCalledWith(comp.menuID, dummySection.id);
    });
  });

  describe('activateSection', () => {
    beforeEach(() => {
      spyOn(menuService, 'activateSection');
      comp.activateSection(new Event('click'));
    });
    it('should trigger the activateSection function on the menu service', () => {
      expect(menuService.activateSection).toHaveBeenCalledWith(comp.menuID, dummySection.id);
    });
  });

  describe('deactivateSection', () => {
    beforeEach(() => {
      spyOn(menuService, 'deactivateSection');
      comp.deactivateSection(new Event('click'));
    });
    it('should trigger the deactivateSection function on the menu service', () => {
      expect(menuService.deactivateSection).toHaveBeenCalledWith(comp.menuID, dummySection.id);
    });
  });

});
