import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { BrowseByTitlePageComponent } from './browse-by-title-page.component';

/**
 * Themed wrapper for BrowseByTitlePageComponent
 */
@Component({
    selector: 'ds-themed-browse-by-title-page',
    styleUrls: [],
    templateUrl: '../../shared/theme-support/themed.component.html',
    standalone: true
})

export class ThemedBrowseByTitlePageComponent
  extends ThemedComponent<BrowseByTitlePageComponent> {
  protected getComponentName(): string {
    return 'BrowseByTitlePageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/browse-by/browse-by-title-page/browse-by-title-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./browse-by-title-page.component`);
  }
}
