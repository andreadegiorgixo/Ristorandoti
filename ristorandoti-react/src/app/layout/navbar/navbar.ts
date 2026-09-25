import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import { Avatar } from '../../shared/components/avatar/avatar';
import { GlobalSearch } from '../../shared/components/global-search/global-search';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, Avatar, GlobalSearch],
  templateUrl: './navbar.html',
})
export class Navbar {
  protected readonly auth = inject(AuthService);
  protected readonly profile = inject(ProfileService);
  private readonly router = inject(Router);

  protected readonly menuOpen = signal(false);

  private readonly publicLinks = [
    { path: '/', label: 'Home', exact: true },
    { path: '/mission', label: 'Obiettivo', exact: false },
    { path: '/about', label: 'Chi siamo', exact: false },
  ];

  private readonly memberLinks = [
    { path: '/feed', label: 'Feed', exact: true },
    { path: '/profile', label: 'Profilo', exact: false },
  ];

  /** Da loggato contano feed e profilo; le pagine informative restano nel footer. */
  protected readonly links = computed(() => (this.auth.isAuthenticated() ? this.memberLinks : this.publicLinks));

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
