import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Footer } from './layout/footer/footer';
import { Navbar } from './layout/navbar/navbar';
import { Toasts } from './shared/components/toasts/toasts';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, Footer, Toasts],
  templateUrl: './app.html',
})
export class App {}
