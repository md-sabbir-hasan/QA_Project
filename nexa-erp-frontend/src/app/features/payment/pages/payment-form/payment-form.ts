import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import {
  FormArray,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { Invoice } from '../../../invoice/models/invoice.model';
import { InvoiceService } from '../../../invoice/services/invoice.service';
import { Party } from '../../../party/models/party.model';
import { PartyService } from '../../../party/services/party.service';
import { VendorBill } from '../../../vendor-bill/models/vendor-bill.model';
import { VendorBillService } from '../../../vendor-bill/services/vendor-bill.service';
import {
  PaymentAllocationRequest,
  PaymentMethod,
  PaymentReferenceType,
  PaymentRequest,
  PaymentType,
} from '../../models/payment.model';
import { PaymentService } from '../../services/payment.service';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './payment-form.html',
  styleUrl: './payment-form.scss',
})
export class PaymentForm implements OnInit {
  readonly submitting = signal(false);
  readonly parties = signal<Party[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly dueInvoices = signal<Invoice[]>([]);
  readonly dueBills = signal<VendorBill[]>([]);

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

  readonly allocationTotal = computed(() =>
    this.allocations.controls.reduce(
      (sum, ctrl) => sum + Number(ctrl.get('allocatedAmount')?.value ?? 0),
      0,
    ),
  );

  remainingDue(
    dueAmount: number,
    allocatedAmount: number
  ): number {

    return Math.max(
      Number(dueAmount) - Number(allocatedAmount),
      0
    );

  }

  isAllocationInvalid(index: number): boolean {
    const group = this.allocations.at(index) as FormGroup;
    const referenceId = Number(group.get('referenceId')?.value);
    const allocated = Number(group.get('allocatedAmount')?.value ?? 0);

    const type = this.form.get('paymentType')?.value as PaymentType;

    const due =
      type === 'RECEIPT'
        ? Number(this.getInvoiceById(referenceId)?.dueAmount ?? 0)
        : Number(this.getBillById(referenceId)?.dueAmount ?? 0);

    return allocated > due;
  }

  hasInvalidAllocation(): boolean {
    return this.allocations.controls.some((_, index) =>
      this.isAllocationInvalid(index)
    );
  }

  readonly unallocatedAmount = computed(() =>
    Math.max(Number(this.form.get('amount')?.value ?? 0) - this.allocationTotal(), 0),
  );

  readonly form: FormGroup;

  constructor(
    private fb: NonNullableFormBuilder,
    private paymentService: PaymentService,
    private partyService: PartyService,
    private accountService: AccountService,
    private invoiceService: InvoiceService,
    private vendorBillService: VendorBillService,
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
      allocations: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    const today = new Date().toISOString().substring(0, 10);
    this.form.patchValue({ paymentDate: today });

    this.loadParties();
    this.loadAccounts();

    this.form.get('paymentType')?.valueChanges.subscribe(() => {
      this.form.patchValue({ partyId: null });
      this.dueInvoices.set([]);
      this.dueBills.set([]);
      this.allocations.clear();
      this.loadParties();
    });

    this.form.get('autoAllocate')?.valueChanges.subscribe((auto) => {
      if (auto) {
        this.allocations.clear();
      } else {
        this.loadDueDocuments();
      }
    });
  }

  get allocations(): FormArray {
    return this.form.get('allocations') as FormArray;
  }

  createAllocation(
    referenceType: PaymentReferenceType,
    referenceId: number,
    allocatedAmount = 0,
  ): FormGroup {
    return this.fb.group({
      referenceType: [referenceType, [Validators.required]],
      referenceId: [referenceId, [Validators.required]],
      allocatedAmount: [allocatedAmount, [Validators.min(0)]],
    });
  }

  loadParties(): void {
    const type = this.form.get('paymentType')?.value as PaymentType;
    const partyType = type === 'RECEIPT' ? 'CUSTOMER' : 'VENDOR';

    this.partyService.getByType(partyType).subscribe({
      next: (res) => this.parties.set(res.data.filter((p) => p.isActive)),
      error: () => this.alert.error('Failed to load parties'),
    });
  }

  loadAccounts(): void {
    this.accountService.search('', 'ASSET', true).subscribe({
      next: (res) => this.accounts.set(res.data),
      error: () => this.alert.error('Failed to load payment accounts'),
    });
  }

  onPartyChange(): void {
    const partyId = Number(this.form.get('partyId')?.value);
    const party = this.parties().find((p) => p.id === partyId);

    if (party) {
      this.form.patchValue({ currencyCode: party.currency ?? 'BDT' });
    }

    if (!this.form.get('autoAllocate')?.value) {
      this.loadDueDocuments();
    }
  }

  loadDueDocuments(): void {
    const partyId = Number(this.form.get('partyId')?.value);
    const type = this.form.get('paymentType')?.value as PaymentType;

    this.allocations.clear();
    this.dueInvoices.set([]);
    this.dueBills.set([]);

    if (!partyId) return;

    if (type === 'RECEIPT') {
      this.invoiceService.getByParty(partyId).subscribe({
        next: (res) => {
          const due = res.data.filter((invoice) =>
            invoice.status !== 'CANCELLED' && Number(invoice.dueAmount ?? 0) > 0
          );

          this.dueInvoices.set(due);

          due.forEach((invoice) => {
            this.allocations.push(
              this.createAllocation('INVOICE', invoice.id, 0),
            );
          });
        },
        error: () => this.alert.error('Failed to load due invoices'),
      });
    } else {
      this.vendorBillService.getByParty(partyId).subscribe({
        next: (res) => {
          const due = res.data.filter((bill) =>
            bill.status !== 'CANCELLED' && Number(bill.dueAmount ?? 0) > 0
          );

          this.dueBills.set(due);

          due.forEach((bill) => {
            this.allocations.push(
              this.createAllocation('VENDOR_BILL', bill.id, 0),
            );
          });
        },
        error: () => this.alert.error('Failed to load due vendor bills'),
      });
    }
  }

  getInvoiceById(id: number): Invoice | undefined {
    return this.dueInvoices().find((invoice) => invoice.id === id);
  }

  getBillById(id: number): VendorBill | undefined {
    return this.dueBills().find((bill) => bill.id === id);
  }

  getPaymentTitle(): string {
    return this.form.get('paymentType')?.value === 'RECEIPT'
      ? 'Customer Receipt'
      : 'Vendor Payment';
  }

  getPartyLabel(): string {
    return this.form.get('paymentType')?.value === 'RECEIPT'
      ? 'Customer'
      : 'Vendor';
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

    const raw = this.form.getRawValue();

    const allocations: PaymentAllocationRequest[] = raw.autoAllocate
      ? []
      : raw.allocations
        .filter((a: PaymentAllocationRequest) => Number(a.allocatedAmount) > 0)
        .map((a: PaymentAllocationRequest) => ({
          referenceType: a.referenceType,
          referenceId: Number(a.referenceId),
          allocatedAmount: Number(a.allocatedAmount),
        }));


    const totalAllocated = allocations.reduce(
      (sum, a) => sum + Number(a.allocatedAmount),
      0,
    );

    if (!raw.autoAllocate && this.hasInvalidAllocation()) {
      this.alert.error('Allocation amount cannot exceed due amount');
      return;
    }

    if (!raw.autoAllocate && totalAllocated > Number(raw.amount)) {
      this.alert.error('Allocated amount cannot exceed payment amount');
      return;
    }

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
      allocations,
    };

    this.submitting.set(true);

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