export type PartyType = 'CUSTOMER' | 'VENDOR' | 'BOTH';

export interface Party {
  id: number;
  code: string;
  name: string;
  type: PartyType;
  isActive: boolean;
  paymentTerms: number;
  currency: string;
  email: string | null;
  phone: string;
}
