import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    TestBed.configureTestingModule({});
  });

  it('defaults to the dark theme and marks the document', () => {
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('toggles between dark and light and persists the choice', () => {
    const service = TestBed.inject(ThemeService);
    service.toggle();
    expect(service.theme()).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('app.theme')).toBe('light');
    service.toggle();
    expect(service.theme()).toBe('dark');
  });

  it('restores a stored theme', () => {
    localStorage.setItem('app.theme', 'light');
    expect(TestBed.inject(ThemeService).theme()).toBe('light');
  });

  it('ignores an invalid stored value', () => {
    localStorage.setItem('app.theme', 'purple');
    expect(TestBed.inject(ThemeService).theme()).toBe('dark');
  });

  it('keeps working when the storage throws', () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    const service = TestBed.inject(ThemeService);
    expect(() => service.toggle()).not.toThrow();
    expect(service.theme()).toBe('light');
    getItem.mockRestore();
    setItem.mockRestore();
  });
});
