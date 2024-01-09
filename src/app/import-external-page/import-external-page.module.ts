import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ImportExternalRoutingModule } from './import-external-routing.module';
import { SubmissionModule } from '../submission/submission.module';
import { ImportExternalPageComponent } from './import-external-page.component';
import { JournalEntitiesModule } from '../entity-groups/journal-entities/journal-entities.module';
import { ResearchEntitiesModule } from '../entity-groups/research-entities/research-entities.module';

@NgModule({
    imports: [
        CommonModule,
        ImportExternalRoutingModule,
        SubmissionModule,
        JournalEntitiesModule.withEntryComponents(),
        ResearchEntitiesModule.withEntryComponents(),
        ImportExternalPageComponent
    ]
})

/**
 * This module handles all components that are necessary for the submission external import page
 */
export class ImportExternalPageModule {

}
