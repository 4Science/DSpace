/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { ActivatedRouteSnapshot, RouterStateSnapshot, } from '@angular/router';
import { Observable, of } from 'rxjs';
import { DSpaceObject } from '../../../../core/shared/dspace-object.model';
import { AbstractRouteContextMenuProvider } from './route-context.menu';
import { RemoteData } from '../../../../core/data/remote-data';
import { hasValue } from '../../../empty.util';

export abstract class DSpaceObjectPageMenuProvider extends AbstractRouteContextMenuProvider<DSpaceObject> {

  public getRouteContext(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<DSpaceObject | undefined> {
    const dsoRD: RemoteData<DSpaceObject> = route.data.dso;
    if (hasValue(dsoRD) && dsoRD.hasSucceeded && hasValue(dsoRD.payload)) {
      return of(dsoRD.payload);
    } else {
      return of(undefined);
    }
  }

  /**
   * Retrieve the dso or entity type for an object to be used in generic messages
   */
  protected getDsoType(dso: DSpaceObject) {
    const renderType = dso.getRenderTypes()[0];
    if (typeof renderType === 'string' || renderType instanceof String) {
      return renderType.toLowerCase();
    } else {
      return dso.type.toString().toLowerCase();
    }
  }
}
