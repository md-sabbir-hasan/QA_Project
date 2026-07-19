import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { DepreciationMethod, FixedAsset, FixedAssetRequest } from '../../models/fixed-asset.model';
import { FixedAssetService } from '../../services/fixed-asset.service';

@Component({
  selector: 'app-fixed-asset-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe],
  templateUrl: './fixed-asset-list.html',
  styleUrl: './fixed-asset-list.scss',
})
export class FixedAssetList implements OnInit {
  readonly assets = signal<FixedAsset[]>([]);
  readonly accounts = signal<Account[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showRegisterModal = signal(false);
  readonly showRunAllModal = signal(false);

  readonly registerForm;
  readonly runAllDate = signal(this.today());

  constructor(
    private fixedAssetService: FixedAssetService,
    private accountService: AccountService,
    private alert: AlertService,
    private router: Router,
    private fb: NonNullableFormBuilder,
  ) {
    this.registerForm = this.fb.group({
      name: ['', [Validators.required]],
      description: [''],
      assetAccountId: [null as number | null, [Validators.required]],
      depreciationExpenseAccountId: [null as number | null, [Validators.required]],
      accumulatedDepreciationAccountId: [null as number | null, [Validators.required]],
      purchaseDate: [this.today(), [Validators.required]],
      purchaseCost: [0, [Validators.required, Validators.min(0.01)]],
      salvageValue: [0, [Validators.required, Validators.min(0)]],
      usefulLifeYears: [5, [Validators.required, Validators.min(1)]],
      depreciationMethod: ['STRAIGHT_LINE' as DepreciationMethod, [Validators.required]],
      reducingBalanceRate: [null as number | null],
    });
  }

  ngOnInit(): void {
    this.loadAssets();
    this.loadAccounts();
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  loadAssets(): void {
    this.loading.set(true);
    this.fixedAssetService.getAll().subscribe({
      next: (res) => {
        this.assets.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load fixed assets');
      },
    });
  }

  loadAccounts(): void {
    this.accountService.getAll().subscribe({
      next: (res) => this.accounts.set(res.data.filter((a) => a.isActive && !a.hasChildren)),
    });
  }

  get isReducingBalance(): boolean {
    return this.registerForm.controls.depreciationMethod.value === 'REDUCING_BALANCE';
  }

  get totals() {
    const list = this.assets();
    return {
      cost: list.reduce((sum, a) => sum + a.purchaseCost, 0),
      accumulatedDepreciation: list.reduce((sum, a) => sum + a.accumulatedDepreciation, 0),
      bookValue: list.reduce((sum, a) => sum + a.bookValue, 0),
    };
  }

  openRegisterModal(): void {
    this.registerForm.reset({
      name: '',
      description: '',
      assetAccountId: null,
      depreciationExpenseAccountId: null,
      accumulatedDepreciationAccountId: null,
      purchaseDate: this.today(),
      purchaseCost: 0,
      salvageValue: 0,
      usefulLifeYears: 5,
      depreciationMethod: 'STRAIGHT_LINE',
      reducingBalanceRate: null,
    });
    this.showRegisterModal.set(true);
  }

  closeRegisterModal(): void {
    this.showRegisterModal.set(false);
  }

  submitRegister(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const raw = this.registerForm.getRawValue();

    if (raw.depreciationMethod === 'REDUCING_BALANCE' && !raw.reducingBalanceRate) {
      this.alert.warning('Reducing balance rate is required for this method');
      return;
    }

    this.submitting.set(true);

    const request: FixedAssetRequest = {
      name: raw.name,
      description: raw.description || null,
      assetAccountId: raw.assetAccountId!,
      depreciationExpenseAccountId: raw.depreciationExpenseAccountId!,
      accumulatedDepreciationAccountId: raw.accumulatedDepreciationAccountId!,
      purchaseDate: raw.purchaseDate,
      purchaseCost: raw.purchaseCost,
      salvageValue: raw.salvageValue,
      usefulLifeYears: raw.usefulLifeYears,
      depreciationMethod: raw.depreciationMethod,
      reducingBalanceRate: raw.depreciationMethod === 'REDUCING_BALANCE' ? raw.reducingBalanceRate : null,
    };

    this.fixedAssetService.create(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showRegisterModal.set(false);
        this.alert.success('Fixed asset registered');
        this.loadAssets();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to register asset');
      },
    });
  }

  openRunAllModal(): void {
    this.runAllDate.set(this.today());
    this.showRunAllModal.set(true);
  }

  closeRunAllModal(): void {
    this.showRunAllModal.set(false);
  }

  submitRunAll(): void {
    this.submitting.set(true);
    this.fixedAssetService.runDepreciationForAll(this.runAllDate()).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.showRunAllModal.set(false);
        this.alert.success(`Depreciation posted for ${res.data.length} asset(s)`);
        this.loadAssets();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to run depreciation');
      },
    });
  }

  openAsset(asset: FixedAsset): void {
    this.router.navigate(['/fixed-assets', asset.id]);
  }
}
