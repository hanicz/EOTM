import { Component, ElementRef, inject, OnInit, Output, EventEmitter, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Input } from '@angular/core';
import { EMPTY_FILTER, PortfolioFilter, filterRows } from '../../util/tablefilter';
import { CsvDropDirective } from '../../util/csv-drop.directive';
import { Interest } from 'src/app/model/interest';
import { InterestService } from 'src/app/service/interest.service';
import { SecurityService } from 'src/app/service/security.service';
import { Globals } from '../../util/global';
import { Security } from 'src/app/model/security';
import { Bind } from 'primeng/bind';
import { Toolbar } from 'primeng/toolbar';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { Tooltip } from 'primeng/tooltip';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';
import { Dialog } from 'primeng/dialog';
import { FormsModule } from '@angular/forms';
import { Select } from 'primeng/select';
import { CurrencyPipe, DatePipe } from '@angular/common';

@Component({
    selector: 'app-interest',
    hostDirectives: [CsvDropDirective],
    templateUrl: './interest.component.html',
    imports: [Bind, Toolbar, PrimeTemplate, ButtonDirective, Ripple, Tooltip, TableModule, InputText, Dialog, FormsModule, Select, CurrencyPipe, DatePipe, Toast]
})
export class InterestComponent implements OnInit {
  visibleInterests: Interest[] = [];
  private activeFilter: PortfolioFilter = EMPTY_FILTER;

  @Input() set filter(value: PortfolioFilter) {
    this.activeFilter = value ?? EMPTY_FILTER;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.visibleInterests = filterRows(this.interests, this.activeFilter, ['securityName', 'securityId']);
  }


  interests: Interest[] = [];
  @Output() dataLoaded = new EventEmitter<Interest[]>();
  currencies: any[];
  selectedInterests: Interest[] = [];
  interestDialog: boolean = false;
  interest: Interest = {} as Interest;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  globals: Globals;
  securities: Security[] = [];
  selectedExistingSecurity: Security | null = null;

  constructor(private interestService: InterestService, private securityService: SecurityService, globals: Globals, private cdr: ChangeDetectorRef, private messageService: MessageService) {
    inject(CsvDropDirective).csvDropped.subscribe(event => this.onUpload(event));
    this.globals = globals;
    this.currencies = globals.currencies;

    this.securityService.getAllSecurities().subscribe({
      next: (data) => {
        this.securities = data;
        this.cdr.markForCheck();
      }
    });

    this.fetchData();
  }

  ngOnInit(): void {
  }

  refresh(): void {
    this.fetchData();
  }

  private fetchData(): void {
    this.interestService.getAllInterest().subscribe({
      next: (data) => {
        this.interests = data;
        this.applyFilter();
        this.dataLoaded.emit(this.interests);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.log(error);
      }
    });
  }

  openNew() {
    this.interest = {} as Interest;
    this.selectedExistingSecurity = null;
    this.interestDialog = true;
  }

  hideDialog() {
    this.interestDialog = false;
  }

  editInterest(interest: Interest) {
    this.interest = { ...interest };
    this.selectedExistingSecurity = this.securities.find(s => s.id === interest.securityId) ?? null;
    this.interestDialog = true;
  }

  existingSecurityChanged(): void {
    if (this.selectedExistingSecurity) {
      this.interest.securityId = this.selectedExistingSecurity.id;
      this.interest.securityName = this.selectedExistingSecurity.name;
    }
  }

  deleteClicked() {
    const ids = this.selectedInterests.map(i => i.interestId).join(',');
    this.deleteByIds(ids);
  }

  deleteByIds(ids: string) {
    this.interestService.deleteByIds(ids).subscribe({
      next: () => {
        this.selectedInterests = [];
        this.fetchData();
      }
    });
  }

  download() {
    this.interestService.download().subscribe({
      next: (data) => {
        let fileName = 'interest.csv';
        let a = document.createElement('a');
        a.href = window.URL.createObjectURL(data as Blob);
        a.download = fileName;
        a.click();
      }
    });
  }

  saveInterest() {
    if (this.interest.interestId === undefined) {
      this.interestService.create(this.interest).subscribe({
        next: () => {
          this.fetchData();
          this.interestDialog = false;
        }
      });
    } else {
      this.interestService.update(this.interest).subscribe({
        next: () => {
          this.fetchData();
          this.interestDialog = false;
        }
      });
    }
  }

  fileChosen(event: Event) {
    const files = Array.from((event.target as HTMLInputElement).files ?? []);
    if (files.length) this.onUpload({ files });
  }

  private clearFileInput() {
    if (this.fileInput) this.fileInput.nativeElement.value = '';
  }

  onUpload(event: any) {
    for (let file of event.files) {
      this.interestService.uploadCSV(file).subscribe({
        next: () => {
          this.fetchData();
          this.clearFileInput();
          this.messageService.add({ severity: 'success', detail: 'Import finished.' });
        },
        error: (error) => {
          this.clearFileInput();
          this.messageService.add({ severity: 'error', detail: error.error?.error ?? 'Import failed.' });
        }
      });
    }
  }
}
