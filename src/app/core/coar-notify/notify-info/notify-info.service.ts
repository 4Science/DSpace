import { Injectable } from '@angular/core';
import { getFirstSucceededRemoteData, getRemoteDataPayload } from '../../shared/operators';
import { ConfigurationDataService } from '../../data/configuration-data.service';
import { map, Observable } from 'rxjs';
import { ConfigurationProperty } from '../../shared/configuration-property.model';

/**
 * Service to check COAR availability and LDN services information for the COAR Notify functionalities
 */
@Injectable({
    providedIn: 'root'
})
export class NotifyInfoService {

    /**
     *  The relation link for the inbox
     */
    private _inboxRelationLink = 'http://www.w3.org/ns/ldp#inbox';

    constructor(
        private configService: ConfigurationDataService,
    ) {}

    isCoarConfigEnabled(): Observable<boolean> {
        return this.configService.findByPropertyName('ldn.enabled').pipe(
            getFirstSucceededRemoteData(),
            map(response => {
                const booleanArrayValue = response.payload.values;
                return booleanArrayValue.length > 0 ? booleanArrayValue[0] === 'true' : false;
            })
        );
    }

    /**
     * Get the url of the local inbox from the REST configuration
     * @returns the url of the local inbox
     */
    getCoarLdnLocalInboxUrls(): Observable<string[]> {
      return this.configService.findByPropertyName('ldn.notify.inbox').pipe(
        getFirstSucceededRemoteData(),
        getRemoteDataPayload(),
        map((response: ConfigurationProperty) => {
          return response.values;
        })
      );
    }

    /**
     * Method to get the relation link for the inbox
     * @returns the relation link for the inbox
     */
    getInboxRelationLink(): string {
      return this._inboxRelationLink;
    }
}
