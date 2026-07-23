import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardSummary } from '../../models/dashboard.model';
import { DashboardService } from '../../services/dashboard.service';

interface ChartPoint {
  x: number;
  y: number;
}

interface TrendChart {
  width: number;
  height: number;
  revenueLine: string;
  revenueArea: string;
  expenseLine: string;
  revenuePoints: ChartPoint[];
  expensePoints: ChartPoint[];
  months: string[];
  gridLines: { y: number; label: string }[];
  plotLeft: number;
  plotRight: number;
  plotBottom: number;
}

interface BudgetGauge {
  radius: number;
  circumference: number;
  dashOffset: number;
  pct: number;
  isOver: boolean;
  activeBudgetName: string | null;
  totalExpenseActualYtd: number;
  totalExpenseBudget: number;
  topAccounts: {
    accountName: string;
    budgetAmount: number;
    actualAmount: number;
    utilizationPercent: number;
  }[];
}

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
  readonly today = new Date();

  readonly trendChart = computed<TrendChart | null>(() => {
    const data = this.summary();
    if (!data) return null;

    const revenue = data.business.revenueTrend;
    const expense = data.business.expenseTrend;
    if (revenue.length === 0) return null;

    const allValues = [...revenue.map((r) => r.amount), ...expense.map((e) => e.amount)];
    const maxValue = Math.max(...allValues, 1);

    const width = 600;
    const height = 220;
    const paddingLeft = 52;
    const paddingRight = 12;
    const paddingTop = 16;
    const paddingBottom = 34;

    const plotWidth = width - paddingLeft - paddingRight;
    const plotHeight = height - paddingTop - paddingBottom;
    const stepX = revenue.length > 1 ? plotWidth / (revenue.length - 1) : 0;

    const toPoint = (value: number, index: number): ChartPoint => ({
      x: paddingLeft + stepX * index,
      y: paddingTop + plotHeight - (value / maxValue) * plotHeight,
    });

    const revenuePoints = revenue.map((r, i) => toPoint(r.amount, i));
    const expensePoints = expense.map((e, i) => toPoint(e.amount, i));

    const linePath = (points: ChartPoint[]) =>
      points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');

    const areaPath = (points: ChartPoint[]) => {
      if (points.length === 0) return '';
      const base = paddingTop + plotHeight;
      const first = points[0];
      const last = points[points.length - 1];
      return `${linePath(points)} L ${last.x.toFixed(1)} ${base} L ${first.x.toFixed(1)} ${base} Z`;
    };

    const gridLines = [0.25, 0.5, 0.75, 1].map((f) => ({
      y: paddingTop + plotHeight * (1 - f),
      label: this.formatCompact(maxValue * f),
    }));

    return {
      width,
      height,
      revenueLine: linePath(revenuePoints),
      revenueArea: areaPath(revenuePoints),
      expenseLine: linePath(expensePoints),
      revenuePoints,
      expensePoints,
      months: revenue.map((r) => r.month),
      gridLines,
      plotLeft: paddingLeft,
      plotRight: width - paddingRight,
      plotBottom: paddingTop + plotHeight,
    };
  });

  readonly budgetGauge = computed<BudgetGauge | null>(() => {
    const b = this.summary()?.budget;
    if (!b || !b.hasActiveBudget) return null;

    const radius = 72;
    const circumference = 2 * Math.PI * radius;
    const pct = Math.min(b.expenseUtilizationPercent ?? 0, 100);
    const dashOffset = circumference - (pct / 100) * circumference;

    return {
      radius,
      circumference,
      dashOffset,
      pct: b.expenseUtilizationPercent ?? 0,
      isOver: (b.expenseUtilizationPercent ?? 0) > 100,
      activeBudgetName: b.activeBudgetName,
      totalExpenseActualYtd: b.totalExpenseActualYtd,
      totalExpenseBudget: b.totalExpenseBudget,
      topAccounts: b.topAccounts ?? [],
    };
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

  minPct(value: number): number {
    return Math.min(value, 100);
  }

  activityColor(action: string): string {
    const map: Record<string, string> = {
      CREATED: 'green',
      POSTED: 'brass',
      ACTIVATED: 'green',
      APPROVED: 'green',
      CANCELLED: 'red',
      DELETED: 'red',
      REVERSED: 'red',
      DEACTIVATED: 'red',
      UPDATED: 'slate',
      CLOSED: 'slate',
    };
    return map[action] ?? 'slate';
  }

  private formatCompact(value: number): string {
    if (value >= 100000) return (value / 100000).toFixed(1) + 'L';
    if (value >= 1000) return (value / 1000).toFixed(0) + 'k';
    return value.toFixed(0);
  }

  healthClass(status: string): string {
    const s = (status ?? '').toUpperCase();
    return s.includes('UP') || s.includes('OK') || s.includes('HEALTHY') || s.includes('CONNECTED')
      ? 'ok'
      : 'down';
  }
}
