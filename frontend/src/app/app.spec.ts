import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('shows_product_name_in_toolbar', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const toolbar = (fixture.nativeElement as HTMLElement).querySelector('mat-toolbar');

    expect(toolbar?.textContent).toContain('ACME Salary Management');
  });

  it('hosts_routed_pages_in_router_outlet', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const outlet = (fixture.nativeElement as HTMLElement).querySelector('main router-outlet');

    expect(outlet).not.toBeNull();
  });
});
