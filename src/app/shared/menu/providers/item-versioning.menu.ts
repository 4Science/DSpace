/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { Injectable } from '@angular/core';
import { combineLatest, Observable, } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { FeatureID } from '../../../core/data/feature-authorization/feature-id';
import { Item } from '../../../core/shared/item.model';
import { DsoVersioningModalService } from '../../dso-page/dso-versioning-modal-service/dso-versioning-modal.service';
import { MenuItemType } from '../menu-item-type.model';
import { OnClickMenuItemModel } from '../menu-item/models/onclick.model';
import { PartialMenuSection } from '../menu-provider';
import { DSpaceObjectPageMenuProvider } from './helper-providers/dso.menu';

@Injectable()
export class VersioningMenuProvider extends DSpaceObjectPageMenuProvider {
  constructor(
    protected authorizationService: AuthorizationDataService,
    protected dsoVersioningModalService: DsoVersioningModalService,
  ) {
    super();
  }

  public getSectionsForContext(item: Item): Observable<PartialMenuSection[]> {


    return combineLatest([
      this.authorizationService.isAuthorized(FeatureID.CanCreateVersion, item.self),
      this.dsoVersioningModalService.isNewVersionButtonDisabled(item),
      this.dsoVersioningModalService.getVersioningTooltipMessage(item, 'item.page.version.hasDraft', 'item.page.version.create'),
    ]).pipe(
      map(([canCreateVersion, disableVersioning, versionTooltip]) => {

        return [
          {
            visible: canCreateVersion,
            model: {
              type: MenuItemType.ONCLICK,
              text: versionTooltip,
              disabled: disableVersioning,
              function: () => {
                this.dsoVersioningModalService.openCreateVersionModal(item);
              },
            } as OnClickMenuItemModel,
            icon: 'code-branch',
          },
        ] as PartialMenuSection[];
      }),
    );
  }
}
