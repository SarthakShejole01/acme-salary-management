import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ConfirmDialog, ConfirmDialogData } from './confirm-dialog';

describe('ConfirmDialog', () => {
  const close = vi.fn();
  const data: ConfirmDialogData = {
    title: 'Delete employee',
    message: 'Delete Priya Sharma (EMP-00001)? This cannot be undone.',
    confirmLabel: 'Delete',
  };

  async function render(): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [ConfirmDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: { close } },
      ],
    });
    const fixture = TestBed.createComponent(ConfirmDialog);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => close.mockReset());

  it('asks_the_question_with_a_named_confirm_button', async () => {
    const element = await render();

    expect(element.textContent).toContain(
      'Delete Priya Sharma (EMP-00001)? This cannot be undone.',
    );
    expect(element.querySelector('[data-testid="confirm"]')?.textContent).toContain('Delete');
  });

  it('closes_with_true_when_confirmed', async () => {
    const element = await render();

    (element.querySelector('[data-testid="confirm"]') as HTMLButtonElement).click();

    expect(close).toHaveBeenCalledWith(true);
  });

  it('closes_with_false_when_cancelled', async () => {
    const element = await render();

    (element.querySelector('[data-testid="cancel"]') as HTMLButtonElement).click();

    expect(close).toHaveBeenCalledWith(false);
  });
});
