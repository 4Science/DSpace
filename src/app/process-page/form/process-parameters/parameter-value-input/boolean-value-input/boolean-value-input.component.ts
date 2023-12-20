import { Component, OnInit, Optional } from '@angular/core';
import { ValueInputComponent } from '../value-input.component';
import { ControlContainer, NgForm } from '@angular/forms';
import { controlContainerFactory } from '../../../process-form.component';
import { TranslateModule } from '@ngx-translate/core';

/**
 * Represents the value of a boolean parameter
 */
@Component({
    selector: 'ds-boolean-value-input',
    templateUrl: './boolean-value-input.component.html',
    styleUrls: ['./boolean-value-input.component.scss'],
    imports: [TranslateModule],
    viewProviders: [{ provide: ControlContainer,
            useFactory: controlContainerFactory,
            deps: [[new Optional(), NgForm]] }],
    standalone: true
})
export class BooleanValueInputComponent extends ValueInputComponent<boolean> implements OnInit {
  ngOnInit() {
    this.updateValue.emit(true);
  }
}
