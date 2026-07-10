import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';

import { AgingResponse } from '../../models/aging.model';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-aging-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './aging-report.html',
  styleUrl: './aging-report.scss',
})
export class AgingReport implements OnInit {
  readonly loading = signal(false);

  readonly report = signal<AgingResponse | null>(null);

  readonly asOfDate = signal('');

  readonly partyType = signal<'CUSTOMER' | 'VENDOR'>('CUSTOMER');

  readonly rowCount = computed(() => this.report()?.rows.length ?? 0);

  constructor(
    private readonly reportService: ReportService,
    private readonly alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.asOfDate.set(this.formatDate(new Date()));
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();

    const month = String(date.getMonth() + 1).padStart(2, '0');

    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  generateReport(): void {
    this.loading.set(true);

    this.report.set(null);

    this.reportService.getAgingReport(this.partyType(), this.asOfDate()).subscribe({
      next: (response) => {
        this.report.set(response.data);

        this.loading.set(false);
      },

      error: (error) => {
        this.loading.set(false);

        this.alert.error(error?.error?.message ?? 'Failed to generate Aging Report');
      },
    });
  }

  clearReport(): void {
    this.report.set(null);

    this.partyType.set('CUSTOMER');

    this.asOfDate.set(this.formatDate(new Date()));
  }

  printReport(): void {
    window.print();
  }
}
