import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonDirective } from '../../../shared/ui/button.directive';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, ButtonDirective],
  templateUrl: './home-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePage {}
