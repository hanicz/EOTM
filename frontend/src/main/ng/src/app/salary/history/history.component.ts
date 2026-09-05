import { ChangeDetectorRef, Component } from '@angular/core';
import { Salary, SalaryBasis } from '../../model/salary';
import { SalaryService } from '../../service/salary.service';
import { Globals } from '../../util/global';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Dialog } from 'primeng/dialog';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { DeltaComponent } from '../../util/delta.component';

@Component({
    selector: 'app-salary-history',
    templateUrl: './history.component.html',
    styleUrls: ['./history.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, TableModule,
        InputText, Select, Dialog, FormsModule, CurrencyPipe, DatePipe, DeltaComponent]
})
export class SalaryHistoryComponent {

  salaries: Salary[] = [];
  selectedSalaries: Salary[] = [];
  salaryDialog: boolean = false;
  salary: Salary = this.emptySalary();
  currencies: any[];

  readonly bases: { label: string, value: SalaryBasis }[] = [
    { label: 'Monthly', value: 'MONTHLY' },
    { label: 'Annual', value: 'ANNUAL' }
  ];
  readonly noteMaxLength = 64;
  readonly maxDependents = 10;

  constructor(private salaryService: SalaryService, private cdr: ChangeDetectorRef,
    private messageService: MessageService, globals: Globals) {
    this.currencies = globals.currencies;
    this.fetchData();
  }

  private fetchData(): void {
    this.salaryService.getSalaries().subscribe({
      next: (data) => {
        this.salaries = this.withRaises(data);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  private withRaises(salaries: Salary[]): Salary[] {
    const chronological = [...salaries].sort((a, b) => a.validFrom.localeCompare(b.validFrom));

    for (let i = 0; i < chronological.length; i++) {
      const salary = chronological[i];
      const previous = i === 0 ? null : chronological[i - 1];
      salary.raiseAmount = null;
      salary.raisePercent = null;

      if (previous && previous.currencyId === salary.currencyId) {
        const before = previous.netMonthly ?? 0;
        const after = salary.netMonthly ?? 0;
        salary.raiseAmount = after - before;
        salary.raisePercent = before > 0 ? ((after - before) / before) * 100 : null;
      }
    }

    return salaries;
  }

  basisLabel(basis: SalaryBasis): string {
    return this.bases.find(option => option.value === basis)?.label ?? basis;
  }

  allowanceMissed(salary: Salary): boolean {
    return salary.dependents > 0 && !salary.familyAllowanceApplied;
  }

  openNew(): void {
    this.salary = this.emptySalary();
    this.salaryDialog = true;
  }

  editSalary(salary: Salary): void {
    this.salary = { ...salary, note: salary.note ?? '' };
    this.salaryDialog = true;
  }

  hideDialog(): void {
    this.salaryDialog = false;
  }

  saveSalary(): void {
    if (!this.salary.amount || !this.salary.validFrom) {
      return;
    }
    this.salary.note = this.salary.note?.trim() || null;
    this.salary.validTo = this.salary.validTo || null;

    const call = this.salary.id === undefined
      ? this.salaryService.create(this.salary)
      : this.salaryService.update(this.salary);

    call.subscribe({
      next: () => {
        this.salaryDialog = false;
        this.selectedSalaries = [];
        this.fetchData();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not save the salary.' });
      }
    });
  }

  deleteClicked(): void {
    const ids = this.selectedSalaries.map(s => s.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string): void {
    this.salaryService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedSalaries = [];
        this.fetchData();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not delete the salary.' });
      }
    });
  }

  private emptySalary(): Salary {
    return {
      amount: null,
      basis: 'MONTHLY',
      currencyId: 'HUF',
      validFrom: '',
      validTo: null,
      dependents: 0,
      note: ''
    };
  }
}
