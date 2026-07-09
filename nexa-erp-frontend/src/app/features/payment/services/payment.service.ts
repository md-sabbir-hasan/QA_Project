import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { PaymentRequest, PaymentResponse } from '../models/payment.model';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/payments`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<PaymentResponse[]>> {
    return this.http.get<ApiResponse<PaymentResponse[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<PaymentResponse>> {
    return this.http.get<ApiResponse<PaymentResponse>>(`${this.baseUrl}/${id}`);
  }

  getByParty(partyId: number): Observable<ApiResponse<PaymentResponse[]>> {
    return this.http.get<ApiResponse<PaymentResponse[]>>(
      `${this.baseUrl}/party/${partyId}`
    );
  }

  create(request: PaymentRequest): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(this.baseUrl, request);
  }

  post(id: number): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(
      `${this.baseUrl}/${id}/post`,
      {}
    );
  }

  cancel(id: number): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(
      `${this.baseUrl}/${id}/cancel`,
      {}
    );
  }

  downloadReceipt(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/receipt`, {
      responseType: 'blob',
    });
  }
}