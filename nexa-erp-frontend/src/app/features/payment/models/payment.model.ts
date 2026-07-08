export type PaymentType = 'RECEIPT' | 'PAYMENT';

export type PaymentStatus =
  | 'DRAFT'
  | 'POSTED'
  | 'CANCELLED';

export type PaymentMethod =
  | 'CASH'
  | 'BANK_TRANSFER'
  | 'CHEQUE'
  | 'BKASH'
  | 'NAGAD'
  | 'ROCKET'
  | 'CARD';

export type PaymentReferenceType =
  | 'INVOICE'
  | 'VENDOR_BILL';

export interface PaymentAllocation {
  id: number;
  referenceType: PaymentReferenceType;
  referenceId: number;
  allocatedAmount: number;
  createdAt: string;
}

export interface Payment {
  id: number;
  paymentNumber: string;
  paymentDate: string;
  paymentType: PaymentType;

  partyId: number;
  partyName: string;

  accountId: number;
  accountName: string;

  amount: number;
  allocatedAmount: number;
  unallocatedAmount: number;

  currencyCode: string;
  exchangeRate: number;

  paymentMethod: PaymentMethod;
  transactionRef: string | null;
  notes: string | null;

  status: PaymentStatus;

  postedAt: string | null;
  createdAt: string;
  updatedAt: string;

  allocations: PaymentAllocation[];
}

export interface PaymentAllocationRequest {
  referenceType: PaymentReferenceType;
  referenceId: number;
  allocatedAmount: number;
}

export interface PaymentRequest {
  partyId: number | null;
  accountId: number | null;
  paymentDate: string;
  paymentType: PaymentType;
  amount: number;
  currencyCode: string;
  paymentMethod: PaymentMethod;
  transactionRef: string;
  notes: string;
  autoAllocate: boolean;
  allocations: PaymentAllocationRequest[];
}