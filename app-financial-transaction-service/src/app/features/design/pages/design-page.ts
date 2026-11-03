import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ThemeService } from '../../../core/theme/theme.service';
import { AlertComponent } from '../../../shared/ui/alert.component';
import { BalanceCardComponent } from '../../../shared/ui/balance-card.component';
import { DialogComponent } from '../../../shared/ui/dialog.component';
import { IconTileComponent, IconTileTone } from '../../../shared/ui/icon-tile.component';
import { SegmentedComponent, SegmentedOption } from '../../../shared/ui/segmented.component';
import { StepperComponent } from '../../../shared/ui/stepper.component';
import { ToastService } from '../../../shared/ui/toast.service';
import { ButtonDirective, ButtonVariant } from '../../../shared/ui/button.directive';
import { ChipComponent } from '../../../shared/ui/chip.component';
import { EmptyStateComponent } from '../../../shared/ui/empty-state.component';
import { FieldControlDirective, FormFieldComponent } from '../../../shared/ui/form-field';
import { KpiTileComponent } from '../../../shared/ui/kpi-tile.component';
import { LedgerRowComponent } from '../../../shared/ui/ledger-row.component';
import { MoneyInputComponent } from '../../../shared/ui/money-input.component';
import { SkeletonComponent } from '../../../shared/ui/skeleton.component';
import { TransactionType } from '../../../shared/ui/transaction-types';
import { formatBrl } from '../../../shared/util/money';

interface Swatch {
  readonly name: string;
  readonly background: string;
  readonly role: string;
}

// Full class names are listed so Tailwind can detect them.
const SWATCHES: readonly Swatch[] = [
  { name: 'surface', background: 'bg-surface', role: 'App background' },
  { name: 'surface-raised', background: 'bg-surface-raised', role: 'Cards, header' },
  { name: 'surface-sunken', background: 'bg-surface-sunken', role: 'Inputs, wells' },
  { name: 'surface-overlay', background: 'bg-surface-overlay', role: 'Menus, dialogs' },
  { name: 'brand', background: 'bg-brand', role: 'Rail and bottom bar' },
  { name: 'line', background: 'bg-line', role: 'Dividers (decorative)' },
  { name: 'line-input', background: 'bg-line-input', role: 'Control borders' },
  { name: 'content', background: 'bg-content', role: 'Text' },
  { name: 'content-muted', background: 'bg-content-muted', role: 'Secondary text' },
  { name: 'primary', background: 'bg-primary', role: 'Primary action (gradient start)' },
  { name: 'primary-strong', background: 'bg-primary-strong', role: 'Primary action (gradient end)' },
  { name: 'on-primary', background: 'bg-on-primary', role: 'Text on primary and on the balance panel' },
  { name: 'accent', background: 'bg-accent', role: 'Links, focus ring, selected chip' },
  { name: 'on-accent', background: 'bg-on-accent', role: 'Text on accent' },
  { name: 'income', background: 'bg-income', role: 'Money in' },
  { name: 'expense', background: 'bg-expense', role: 'Money out, errors' },
  { name: 'warning', background: 'bg-warning', role: 'Caution' },
  { name: 'violet', background: 'bg-violet', role: 'Recharge tile' },
];

@Component({
  selector: 'app-design-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AlertComponent,
    BalanceCardComponent,
    ButtonDirective,
    DialogComponent,
    IconTileComponent,
    SegmentedComponent,
    StepperComponent,
    ChipComponent,
    EmptyStateComponent,
    FieldControlDirective,
    FormFieldComponent,
    KpiTileComponent,
    LedgerRowComponent,
    MoneyInputComponent,
    SkeletonComponent,
  ],
  templateUrl: './design-page.html',
})
export class DesignPage {
  private readonly themeService = inject(ThemeService);
  private readonly toast = inject(ToastService);

  protected readonly formatBrl = formatBrl;
  protected readonly theme = this.themeService.theme;

  /** Token values are read from the live CSS variables, so they always match the active theme. */
  protected readonly swatches = computed(() => {
    this.theme();
    const style = getComputedStyle(document.documentElement);
    return SWATCHES.map((swatch) => ({ ...swatch, hex: style.getPropertyValue(`--${swatch.name}`).trim() || 'n/a' }));
  });

  protected readonly variants: readonly ButtonVariant[] = ['primary', 'secondary', 'ghost'];
  protected readonly chips = ['All', 'Deposit', 'Pix', 'Recharge', 'Withdrawal'];
  protected readonly selectedChip = signal('All');

  protected readonly keyTypes: readonly SegmentedOption[] = [
    { value: 'cpf', label: 'CPF' },
    { value: 'email', label: 'E-mail' },
    { value: 'phone', label: 'Mobile' },
  ];
  protected readonly keyType = signal('cpf');

  protected readonly steps = ['Key', 'Amount', 'Review'] as const;
  protected readonly step = signal(2);

  protected readonly dialogOpen = signal(false);
  protected readonly kinds: ReadonlyArray<{ label: string; tile: IconTileTone; icon: string }> = [
    { label: 'Deposit', tile: 'income', icon: 'ph-bold ph-bank' },
    { label: 'Pix', tile: 'accent', icon: 'ph-bold ph-diamonds-four' },
    { label: 'Recharge', tile: 'violet', icon: 'ph-bold ph-device-mobile' },
    { label: 'Withdrawal', tile: 'warning', icon: 'ph-bold ph-money' },
  ];

  protected readonly loading = signal(false);
  protected readonly amount = signal('');

  protected readonly sampleRows: ReadonlyArray<{ type: TransactionType; amount: number; date: string }> = [
    { type: 'DEPOSIT', amount: 1800, date: '2026-11-02T14:35:00-03:00' },
    { type: 'PIX', amount: 250, date: '2026-11-01T10:12:00-03:00' },
    { type: 'PHONE_RECHARGE', amount: 30, date: '2026-10-29T18:40:00-03:00' },
    { type: 'WITHDRAW', amount: 200, date: '2026-10-27T09:05:00-03:00' },
  ];

  protected showToast(): void {
    this.toast.show('Transaction id copied');
  }

  protected simulateLoading(): void {
    this.loading.set(true);
    setTimeout(() => this.loading.set(false), 1800);
  }
}
