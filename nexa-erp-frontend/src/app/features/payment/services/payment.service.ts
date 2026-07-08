import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';

import {
  Payment,
  PaymentRequest,
} from '../models/payment.model';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {

  private readonly baseUrl =
    `${APP_CONFIG.apiUrl}/payments`;

  constructor(
    private http: HttpClient
  ) {}

  getAll(): Observable<ApiResponse<Payment[]>> {
    return this.http.get<ApiResponse<Payment[]>>(
      this.baseUrl
    );
  }

  getById(id: number): Observable<ApiResponse<Payment>> {
    return this.http.get<ApiResponse<Payment>>(
      `${this.baseUrl}/${id}`
    );
  }

  getByParty(partyId: number): Observable<ApiResponse<Payment[]>> {
    return this.http.get<ApiResponse<Payment[]>>(
      `${this.baseUrl}/party/${partyId}`
    );
  }

  create(request: PaymentRequest): Observable<ApiResponse<Payment>> {
    return this.http.post<ApiResponse<Payment>>(
      this.baseUrl,
      request
    );
  }

  post(id: number): Observable<ApiResponse<Payment>> {
    return this.http.post<ApiResponse<Payment>>(
      `${this.baseUrl}/${id}/post`,
      {}
    );
  }

  cancel(id: number): Observable<ApiResponse<Payment>> {
    return this.http.post<ApiResponse<Payment>>(
      `${this.baseUrl}/${id}/cancel`,
      {}
    );
  }

}