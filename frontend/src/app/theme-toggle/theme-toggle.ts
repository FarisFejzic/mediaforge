import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-theme-toggle',
  templateUrl: './theme-toggle.html',
  styleUrl: './theme-toggle.css'
})
export class ThemeToggle {
  protected readonly theme = signal<'light' | 'dark'>('light');

  constructor() {
    const saved = localStorage.getItem('theme') as 'light' | 'dark' | null;
    const initial = saved ?? 'light';
    this.applyTheme(initial);
  }

  toggle() {
    const next = this.theme() === 'light' ? 'dark' : 'light';
    this.applyTheme(next);
  }

  private applyTheme(theme: 'light' | 'dark') {
    this.theme.set(theme);
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }
}
