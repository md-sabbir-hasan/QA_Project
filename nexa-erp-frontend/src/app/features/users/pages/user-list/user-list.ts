import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertService } from '../../../../core/services/alert.service';
import { User, UserStatus } from '../../models/user.model';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
})
export class UserList implements OnInit {
  readonly users = signal<User[]>([]);
  readonly loading = signal(false);

  search = '';
  status: UserStatus | '' = '';

  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;

  constructor(
    private userService: UserService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);

    this.userService
      .getUsers(this.page, this.size, this.search, this.status || undefined)
      .subscribe({
        next: (res) => {
          this.users.set(res.data.content);
          this.page = res.data.page;
          this.size = res.data.size;
          this.totalElements = res.data.totalElements;
          this.totalPages = res.data.totalPages;
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  onSearch(): void {
    this.page = 0;
    this.loadUsers();
  }

  clearSearch(): void {
    this.search = '';
    this.status = '';
    this.page = 0;
    this.loadUsers();
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) {
      this.page++;
      this.loadUsers();
    }
  }

  previousPage(): void {
    if (this.page > 0) {
      this.page--;
      this.loadUsers();
    }
  }

  async deactivateUser(user: User): Promise<void> {
    const confirmed = await this.alert.confirm(
      `Are you sure you want to deactivate ${user.name}?`
    );

    if (!confirmed) return;

    this.userService.deactivate(user.id).subscribe({
      next: () => {
        this.alert.success('User deactivated successfully');
        this.loadUsers();
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to deactivate user');
      },
    });
  }
}