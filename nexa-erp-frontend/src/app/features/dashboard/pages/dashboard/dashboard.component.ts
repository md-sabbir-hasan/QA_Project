import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardSummary } from '../../models/dashboard.model';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  readonly summary = signal<DashboardSummary | null>(null);
  readonly loading = signal(false);

  readonly maxTrendValue = computed(() => {
    const data = this.summary();
    if (!data) return 0;

    const revenueMax = Math.max(0, ...data.business.revenueTrend.map((t) => t.amount));
    const expenseMax = Math.max(0, ...data.business.expenseTrend.map((t) => t.amount));

    return Math.max(revenueMax, expenseMax, 1); // avoid divide-by-zero
  });

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);

    this.dashboardService.getSummary().subscribe({
      next: (res) => {
        this.summary.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  barHeight(amount: number): number {
    return Math.max(4, Math.round((amount / this.maxTrendValue()) * 100));
  }
}
