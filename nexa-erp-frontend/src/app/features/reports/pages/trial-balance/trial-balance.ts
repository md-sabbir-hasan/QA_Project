import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AlertService } from '../../../../core/services/alert.service';
import { TrialBalanceResponse } from '../../models/trial-balance.model';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-trial-balance',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './trial-balance.html',
  styleUrl: './trial-balance.scss',
})
export class TrialBalance implements OnInit {
  readonly loading = signal(false);
  readonly report = signal<TrialBalanceResponse | null>(null);

  asOfDate = '';

  constructor(
    private reportService: ReportService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.asOfDate = new Date().toISOString().substring(0, 10);
  }

  generateReport(): void {
    if (!this.asOfDate) {
      this.alert.error('Please select As Of Date');
      return;
    }

    this.loading.set(true);

    this.reportService.getTrialBalance(this.asOfDate).subscribe({
      next: (response) => {
        this.report.set(response.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to load trial balance');
      },
    });
  }

  clearReport(): void {
    this.report.set(null);
  }

  refreshReport(): void {
    if (this.report()) {
      this.generateReport();
    }
  }

  printReport(): void {
    // Coming in v1.1
  }

  exportReport(): void {
    this.alert.warning('Export feature will be available soon.');
  }
}
