import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { Party } from '../../../party/models/party.model';
import { PartyService } from '../../../party/services/party.service';
import { PaymentMethod, PaymentRequest, PaymentType } from '../../models/payment.model';
import { PaymentService } from '../../services/payment.service';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './payment-form.html',
  styleUrl: './payment-form.scss',
})
export class PaymentForm implements OnInit {
  readonly loading = signal(false);
  readonly submitting = signal(false);

  readonly parties = signal<Party[]>([]);
  readonly accounts = signal<Account[]>([]);

  readonly paymentTypes: PaymentType[] = ['RECEIPT', 'PAYMENT'];
  readonly paymentMethods: PaymentMethod[] = [
    'CASH',
    'BANK_TRANSFER',
    'CHEQUE',
    'BKASH',
    'NAGAD',
    'ROCKET',
    'CARD',
  ];

  readonly form: FormGroup;

  constructor(
    private fb: NonNullableFormBuilder,
    private paymentService: PaymentService,
    private partyService: PartyService,
    private accountService: AccountService,
    private router: Router,
    private alert: AlertService,
  ) {
    this.form = this.fb.group({
      paymentType: ['RECEIPT' as PaymentType, [Validators.required]],
      partyId: [null as number | null, [Validators.required]],
      accountId: [null as number | null, [Validators.required]],
      paymentDate: ['', [Validators.required]],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      currencyCode: ['BDT', [Validators.required]],
      paymentMethod: ['BANK_TRANSFER' as PaymentMethod, [Validators.required]],
      transactionRef: [''],
      notes: [''],
      autoAllocate: [true],
    });
  }

  ngOnInit(): void {
    const today = new Date().toISOString().substring(0, 10);
    this.form.patchValue({ paymentDate: today });

    this.loadParties();
    this.loadAccounts();

    this.form.get('paymentType')?.valueChanges.subscribe(() => {
      this.form.patchValue({ partyId: null });
      this.loadParties();
    });
  }

  loadParties(): void {
    const type = this.form.get('paymentType')?.value as PaymentType;
    const partyType = type === 'RECEIPT' ? 'CUSTOMER' : 'VENDOR';

    this.partyService.getByType(partyType).subscribe({
      next: (res) => {
        this.parties.set(res.data.filter((p) => p.isActive));
      },
      error: () => {
        this.alert.error('Failed to load parties');
      },
    });
  }

  loadAccounts(): void {
    this.accountService.search('', 'ASSET', true).subscribe({
      next: (res) => {
        this.accounts.set(res.data);
      },
      error: () => {
        this.alert.error('Failed to load payment accounts');
      },
    });
  }

  onPartyChange(): void {
    const partyId = this.form.get('partyId')?.value;
    const party = this.parties().find((p) => p.id === Number(partyId));

    if (party) {
      this.form.patchValue({
        currencyCode: party.currency ?? 'BDT',
      });
    }
  }

  getPaymentTitle(): string {
    return this.form.get('paymentType')?.value === 'RECEIPT'
      ? 'Customer Receipt'
      : 'Vendor Payment';
  }

  getPartyLabel(): string {
    return this.form.get('paymentType')?.value === 'RECEIPT' ? 'Customer' : 'Vendor';
  }

  getMethodLabel(method: string): string {
    switch (method) {
      case 'BANK_TRANSFER':
        return 'Bank Transfer';
      case 'BKASH':
        return 'bKash';
      case 'NAGAD':
        return 'Nagad';
      default:
        return method.replace('_', ' ');
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.error('Please complete required fields');
      return;
    }

    this.submitting.set(true);

    const raw = this.form.getRawValue();

    const request: PaymentRequest = {
      partyId: raw.partyId,
      accountId: raw.accountId,
      paymentDate: raw.paymentDate,
      paymentType: raw.paymentType,
      amount: Number(raw.amount),
      currencyCode: raw.currencyCode,
      paymentMethod: raw.paymentMethod,
      transactionRef: raw.transactionRef ?? '',
      notes: raw.notes ?? '',
      autoAllocate: Boolean(raw.autoAllocate),
      allocations: [],
    };

    this.paymentService.create(request).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.alert.success('Payment saved as draft');
        this.router.navigate(['/payment', res.data.id]);
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save payment');
      },
    });
  }
}
