import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { LedgerResponse } from '../models/ledger.model';
import { TrialBalanceResponse } from '../models/trial-balance.model';
import { PartyStatementResponse } from '../models/party-statement.model';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  // Ledger Report
  getLedger(
    accountId: number,
    fromDate: string,
    toDate: string,
  ): Observable<ApiResponse<LedgerResponse>> {
    const params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);

    return this.http.get<ApiResponse<LedgerResponse>>(`${this.baseUrl}/ledger/${accountId}`, {
      params,
    });
  }

  getTrialBalance(asOfDate: string): Observable<ApiResponse<TrialBalanceResponse>> {
    const params = new HttpParams().set('asOfDate', asOfDate);

    return this.http.get<ApiResponse<TrialBalanceResponse>>(`${this.baseUrl}/trial-balance`, {
      params,
    });
  }

  getPartyStatement(
    partyId: number,
    fromDate: string,
    toDate: string,
  ): Observable<ApiResponse<PartyStatementResponse>> {
    const params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);

    return this.http.get<ApiResponse<PartyStatementResponse>>(
      `${this.baseUrl}/party-statement/${partyId}`,
      { params },
    );
  }

  // Download Party Statement as PDF
  downloadPartyStatementPdf(partyId: number, fromDate: string, toDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('partyId', partyId.toString())
      .set('fromDate', fromDate)
      .set('toDate', toDate);

    return this.http.get(`${APP_CONFIG.apiUrl}/reports/party-statement/pdf`, {
      params,
      responseType: 'blob' as const,
    });
  }
}
