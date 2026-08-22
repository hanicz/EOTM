import { Component, ChangeDetectorRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { BankTransaction } from '../../model/bankTransaction';
import { FinancialService } from '../../service/financial.service';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { FileUpload } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-financial-transaction',
    templateUrl: './transaction.component.html',
    styleUrls: ['./transaction.component.css'],
    imports: [Bind, Toolbar, PrimeTemplate, Toast, ButtonDirective, Ripple, Tooltip, FileUpload, TableModule,
        InputText, Select, Tag, FormsModule, CurrencyPipe, DatePipe]
})
export class FinancialTransactionComponent {

  @Output() dataChanged = new EventEmitter<void>();

  transactions: BankTransaction[] = [];
  selectedTransactions: BankTransaction[] = [];
  types: { label: string, value: string }[] = [];
  @ViewChild('fileUpload') fileUpload: any;


  constructor(private financialService: FinancialService, private cdr: ChangeDetectorRef,
    private messageService: MessageService) {
    this.fetchData();
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.financialService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data;
        this.types = [...new Set(data.map(t => t.type))].sort().map(type => ({ label: type, value: type }));
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  excludeClicked(excluded: boolean): void {
    const ids = this.selectedTransactions.map(t => t.id).join(',');
    this.setExcluded(ids, excluded);
  }

  setExcluded(ids: string, excluded: boolean): void {
    this.financialService.setExcluded(ids, excluded).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
        this.dataChanged.emit();
      },
      error: () => {
        this.messageService.add({ severity: 'error', detail: 'Could not update the exclusion.' });
      }
    });
  }

  taxableClicked(taxable: boolean): void {
    const ids = this.selectedTransactions.map(t => t.id).join(',');
    this.setTaxable(ids, taxable);
  }

  setTaxable(ids: string, taxable: boolean): void {
    this.financialService.setTaxable(ids, taxable).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
        this.dataChanged.emit();
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Could not update the taxable flag.' });
      }
    });
  }

  deleteClicked(): void {
    const ids = this.selectedTransactions.map(t => t.id).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string): void {
    this.financialService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedTransactions = [];
        this.fetchData();
        this.dataChanged.emit();
      }
    });
  }

  download(): void {
    this.financialService.download().subscribe({
      next: (data) => {
        const a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = 'bank_transactions.csv';
        a.click();
      }
    });
  }

  onUpload(event: any): void {
    for (let file of event.files) {
      this.financialService.uploadCSV(file).subscribe({
        next: (result) => {
          this.fetchData();
          this.fileUpload.clear();
          this.dataChanged.emit();
          this.messageService.add({
            severity: 'success',
            detail: `Import finished. ${result.created} added, ${result.updated} updated.`
          });
        },
        error: (error) => {
          this.fileUpload.clear();
          this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Import failed.' });
        }
      });
    }
  }
}
