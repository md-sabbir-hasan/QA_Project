import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AlertService } from '../../../../core/services/alert.service';
import { CreditNoteReason, CreditNoteRequest } from '../../models/credit-note.model';
import { InvoiceItemOption, InvoiceOption } from '../../models/invoice-option.model';
import { CreditNoteInvoiceService } from '../../services/credit-note-invoice.service';
import { CreditNoteService } from '../../services/credit-note.service';

@Component({
  selector: 'app-credit-note-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './credit-note-form.html',
  styleUrl: './credit-note-form.scss',
})
export class CreditNoteForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alert = inject(AlertService);
  private readonly service = inject(CreditNoteService);
  private readonly invoiceService = inject(CreditNoteInvoiceService);
  readonly invoices = signal<InvoiceOption[]>([]);
  readonly selectedInvoice = signal<InvoiceOption | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly reasons: CreditNoteReason[] = [
    'SALES_RETURN',
    'PRICE_ADJUSTMENT',
    'POST_INVOICE_DISCOUNT',
    'BILLING_ERROR',
    'VAT_ADJUSTMENT',
    'OTHER',
  ];
  readonly form = this.fb.nonNullable.group({
    invoiceId: [0, [Validators.required, Validators.min(1)]],
    creditNoteDate: [this.today(), Validators.required],
    postingDate: [this.today(), Validators.required],
    reason: ['SALES_RETURN' as CreditNoteReason, Validators.required],
    reference: [''],
    notes: [''],
    items: this.fb.array([]),
  });
  get itemsArray() {
    return this.form.controls.items as FormArray;
  }
  readonly total = computed(() =>
    this.controls().reduce((sum, c, i) => sum + this.lineTotal(i), 0),
  );
  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.editingId.set(Number.isFinite(id) && id > 0 ? id : null);
    this.load();
  }
  load() {
    this.loading.set(true);
    const id = this.editingId();
    if (id === null) {
      this.invoiceService.getAll().subscribe({
        next: (r) => {
          this.invoices.set((r.data ?? []).filter((x) => ['POSTED', 'PARTIAL'].includes(x.status)));
          this.loading.set(false);
        },
        error: (e) => {
          this.loading.set(false);
          this.alert.error(e?.error?.message ?? 'Could not load invoices');
        },
      });
      return;
    }
    forkJoin({ invoices: this.invoiceService.getAll(), note: this.service.getById(id) }).subscribe({
      next: ({ invoices, note }) => {
        this.invoices.set(
          (invoices.data ?? []).filter((x) => ['POSTED', 'PARTIAL'].includes(x.status)),
        );
        const n = note.data;
        if (!n) {
          this.router.navigate(['/credit-notes']);
          return;
        }
        this.form.patchValue({
          invoiceId: n.invoiceId,
          creditNoteDate: n.creditNoteDate,
          postingDate: n.postingDate,
          reason: n.reason,
          reference: n.reference ?? '',
          notes: n.notes ?? '',
        });
        this.selectedInvoice.set(this.invoices().find((x) => x.id === n.invoiceId) ?? null);
        this.itemsArray.clear();
        n.items.forEach((x) => this.itemsArray.push(this.itemGroup(x.invoiceItemId, x.quantity)));
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.alert.error(e?.error?.message ?? 'Could not load credit note');
      },
    });
  }
  onInvoiceChange() {
    const id = Number(this.form.controls.invoiceId.value);
    const inv = this.invoices().find((x) => x.id === id) ?? null;
    this.selectedInvoice.set(inv);
    this.itemsArray.clear();
    inv?.items.forEach((x) => this.itemsArray.push(this.itemGroup(x.id, 0)));
  }
  controls() {
    return this.itemsArray.controls as any[];
  }
  item(index: number): InvoiceItemOption | null {
    const id = Number(this.controls()[index]?.controls.invoiceItemId.value);
    return this.selectedInvoice()?.items.find((x) => x.id === id) ?? null;
  }
  lineTotal(index: number) {
    const c = this.controls()[index],
      item = this.item(index);
    if (!c || !item) return 0;
    const q = Number(c.controls.quantity.value || 0),
      sub = q * Number(item.unitPrice),
      disc = (sub * Number(item.discountPercent ?? 0)) / 100,
      after = sub - disc,
      vat = (after * Number(item.vatRate ?? 0)) / 100;
    return after + vat;
  }
  reasonLabel(v: string) {
    return v.replaceAll('_', ' ');
  }
  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.warning('Complete required fields');
      return;
    }
    const items = this.controls()
      .map((c) => ({
        invoiceItemId: Number(c.controls.invoiceItemId.value),
        quantity: Number(c.controls.quantity.value),
      }))
      .filter((x) => x.quantity > 0);
    if (!items.length) {
      this.alert.warning('Enter at least one credit quantity');
      return;
    }
    const v = this.form.getRawValue();
    const body: CreditNoteRequest = {
      invoiceId: Number(v.invoiceId),
      creditNoteDate: v.creditNoteDate,
      postingDate: v.postingDate || null,
      reason: v.reason,
      reference: v.reference.trim() || null,
      notes: v.notes.trim() || null,
      items,
    };
    this.saving.set(true);
    const id = this.editingId();
    const req = id === null ? this.service.create(body) : this.service.update(id, body);
    req.subscribe({
      next: (r) => {
        this.saving.set(false);
        this.alert.success(r.message || 'Credit note saved');
        this.router.navigate(['/credit-notes']);
      },
      error: (e) => {
        this.saving.set(false);
        this.alert.error(e?.error?.message ?? 'Could not save credit note');
      },
    });
  }
  private itemGroup(id: number, q: number) {
    return this.fb.nonNullable.group({
      invoiceItemId: [id, [Validators.required, Validators.min(1)]],
      quantity: [q, [Validators.required, Validators.min(0)]],
    });
  }
  private today() {
    return new Date().toISOString().slice(0, 10);
  }
}
