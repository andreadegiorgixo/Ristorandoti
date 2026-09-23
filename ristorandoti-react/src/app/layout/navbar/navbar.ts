import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
})
export class Navbar {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly menuOpen = signal(false);

  protected readonly links = [
    { path: '/', label: 'Home', exact: true },
    { path: '/mission', label: 'Obiettivo', exact: false },
    { path: '/about', label: 'Chi siamo', exact: false },
  ];

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected logout(): void {
    this.auth.logout();
    this.closeMenu();
    this.router.navigateByUrl('/');
  }
}
