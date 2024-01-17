import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { of as observableOf } from 'rxjs';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { ProjectSearchResultListElementComponent } from './project-search-result-list-element.component';
import { Item } from '../../../../../core/shared/item.model';
import { TruncatePipe } from '../../../../../shared/utils/truncate.pipe';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { DSONameService } from '../../../../../core/breadcrumbs/dso-name.service';
import { DSONameServiceMock } from '../../../../../shared/mocks/dso-name.service.mock';
import { By } from '@angular/platform-browser';
import { APP_CONFIG } from '../../../../../../config/app-config.interface';
import { ThemeService } from '../../../../../shared/theme-support/theme.service';
import { getMockThemeService } from '../../../../../shared/mocks/theme-service.mock';
import { mockTruncatableService } from '../../../../../shared/mocks/mock-trucatable.service';
import { ThumbnailComponent } from '../../../../../thumbnail/thumbnail.component';
import { TruncatableComponent } from '../../../../../shared/truncatable/truncatable.component';
import { ThemedBadgesComponent } from '../../../../../shared/object-collection/shared/badges/themed-badges.component';
import { ActivatedRoute } from '@angular/router';
import { ActivatedRouteStub } from '../../../../../shared/testing/active-router.stub';

let projectListElementComponent: ProjectSearchResultListElementComponent;
let fixture: ComponentFixture<ProjectSearchResultListElementComponent>;

const mockItemWithMetadata: ItemSearchResult = Object.assign(
  new ItemSearchResult(),
  {
    indexableObject: Object.assign(new Item(), {
      bundles: observableOf({}),
      metadata: {
        'dc.title': [
          {
            language: 'en_US',
            value: 'This is just another title'
          }
        ],
        // 'project.identifier.status': [
        //   {
        //     language: 'en_US',
        //     value: 'A status about the project'
        //   }
        // ]
      }
    })
  });

const mockItemWithoutMetadata: ItemSearchResult = Object.assign(
  new ItemSearchResult(),
  {
    indexableObject: Object.assign(new Item(), {
      bundles: observableOf({}),
      metadata: {
        'dc.title': [
          {
            language: 'en_US',
            value: 'This is just another title'
          }
        ]
      }
    })
  });

const environmentUseThumbs = {
  browseBy: {
    showThumbnails: true
  }
};

const enviromentNoThumbs = {
  browseBy: {
    showThumbnails: false
  }
};

describe('ProjectSearchResultListElementComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
    imports: [TruncatePipe],
    declarations: [ProjectSearchResultListElementComponent],
    providers: [
        { provide: ActivatedRoute, useValue: new ActivatedRouteStub() },
        { provide: TruncatableService, useValue: mockTruncatableService },
        { provide: DSONameService, useClass: DSONameServiceMock },
        { provide: APP_CONFIG, useValue: environmentUseThumbs },
        { provide: ThemeService, useValue: getMockThemeService() },
    ],
    schemas: [NO_ERRORS_SCHEMA]
}).overrideComponent(ProjectSearchResultListElementComponent, {
      add: { changeDetection: ChangeDetectionStrategy.Default },
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(ProjectSearchResultListElementComponent);
    projectListElementComponent = fixture.componentInstance;

  }));

  describe('with environment.browseBy.showThumbnails set to true', () => {
    beforeEach(() => {
      projectListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });
    it('should set showThumbnails to true', () => {
      expect(projectListElementComponent.showThumbnails).toBeTrue();
    });

    it('should add thumbnail element', () => {
      const thumbnailElement = fixture.debugElement.query(By.css('ds-thumbnail'));
      expect(thumbnailElement).toBeTruthy();
    });
  });

  // describe('When the item has a status', () => {
  //   beforeEach(() => {
  //     projectListElementComponent.item = mockItemWithMetadata;
  //     fixture.detectChanges();
  //   });
  //
  //   it('should show the status span', () => {
  //     const statusField = fixture.debugElement.query(By.css('span.item-list-status'));
  //     expect(statusField).not.toBeNull();
  //   });
  // });
  //
  // describe('When the item has no status', () => {
  //   beforeEach(() => {
  //     projectListElementComponent.item = mockItemWithoutMetadata;
  //     fixture.detectChanges();
  //   });
  //
  //   it('should not show the status span', () => {
  //     const statusField = fixture.debugElement.query(By.css('span.item-list-status'));
  //     expect(statusField).toBeNull();
  //   });
  // });
});

describe('ProjectSearchResultListElementComponent', () => {

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
    imports: [TruncatePipe],
    declarations: [ProjectSearchResultListElementComponent],
    providers: [
        { provide: TruncatableService, useValue: mockTruncatableService },
        { provide: DSONameService, useClass: DSONameServiceMock },
        { provide: APP_CONFIG, useValue: enviromentNoThumbs },
        { provide: ThemeService, useValue: getMockThemeService() },
        { provide: ActivatedRoute, useValue: new ActivatedRouteStub() },
    ],
    schemas: [NO_ERRORS_SCHEMA]
}).overrideComponent(ProjectSearchResultListElementComponent, {
      set: {changeDetection: ChangeDetectionStrategy.Default}
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(ProjectSearchResultListElementComponent);
    projectListElementComponent = fixture.componentInstance;
  }));

  describe('with environment.browseBy.showThumbnails set to false', () => {
    beforeEach(() => {

      projectListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should not add thumbnail element', () => {
      const thumbnailElement = fixture.debugElement.query(By.css('ds-thumbnail'));
      expect(thumbnailElement).toBeFalsy();
    });
  });
});
