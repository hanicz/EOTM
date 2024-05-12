import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { Globals } from './util/global';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { HomeComponent } from './home/home.component';
import { MenuComponent } from './menu/menu.component';
import { StockComponent } from './stock/stock.component';
import { HoldingComponent } from './stock/holding/holding.component';
import { PositionComponent } from './stock/position/position.component';
import { InvestmentComponent } from './stock/investment/investment.component';
import { CryptoComponent } from './crypto/crypto.component';
import { TransactionComponent } from './crypto/transaction/transaction.component';
import { CryptopositionComponent } from './crypto/cryptoposition/cryptoposition.component';
import { CryptoholdingComponent } from './crypto/cryptoholding/cryptoholding.component';
import { WatchlistComponent } from './watchlist/watchlist.component';
import { SearchComponent } from './search/search.component';
import { ForexComponent } from './forex/forex.component';
import { DividendComponent } from './stock/dividend/dividend.component';
import { NewsComponent } from './news/news.component';
import { LandingComponent } from './landing/landing.component';

import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { MenubarModule } from 'primeng/menubar';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { DropdownModule } from 'primeng/dropdown';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { FileUploadModule } from 'primeng/fileupload';
import { DialogModule } from 'primeng/dialog';
import { CalendarModule } from 'primeng/calendar';
import { DividerModule } from 'primeng/divider';
import { FieldsetModule } from 'primeng/fieldset';
import { AccordionModule } from 'primeng/accordion';
import { InputMaskModule } from 'primeng/inputmask';
import { CardModule } from 'primeng/card';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ImageModule } from 'primeng/image';
import { ChipModule } from 'primeng/chip';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SkeletonModule } from 'primeng/skeleton';
import { DataViewModule } from 'primeng/dataview';
import { NgApexchartsModule } from 'ng-apexcharts';
import { EtfComponent } from './etf/etf.component';
import { EtfinvestmentComponent } from './etf/etfinvestment/etfinvestment.component';
import { EtfholdingComponent } from './etf/etfholding/etfholding.component';
import { EtfpositionComponent } from './etf/etfposition/etfposition.component';
import { EtfdividendComponent } from './etf/etfdividend/etfdividend.component';
import { ForextransactionComponent } from './forex/forextransaction/forextransaction.component';
import { ForexholdingComponent } from './forex/forexholding/forexholding.component';
import { AlertComponent } from './alert/alert.component';
import { AlertTypePipe } from './util/pipe';
import { InputNumberModule } from 'primeng/inputnumber';
import { DatePipe } from '@angular/common';
import { SettingsComponent } from './settings/settings.component';


@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HomeComponent,
    MenuComponent,
    StockComponent,
    HoldingComponent,
    PositionComponent,
    InvestmentComponent,
    CryptoComponent,
    TransactionComponent,
    CryptopositionComponent,
    CryptoholdingComponent,
    WatchlistComponent,
    SearchComponent,
    ForexComponent,
    DividendComponent,
    NewsComponent,
    LandingComponent,
    EtfComponent,
    EtfinvestmentComponent,
    EtfholdingComponent,
    EtfpositionComponent,
    EtfdividendComponent,
    ForextransactionComponent,
    ForexholdingComponent,
    AlertComponent,
    AlertTypePipe,
    SettingsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    HttpClientModule,
    ReactiveFormsModule,
    ToastModule,
    BrowserAnimationsModule,
    MenubarModule,
    PanelModule,
    TableModule,
    DropdownModule,
    TabViewModule,
    TagModule,
    ToolbarModule,
    FileUploadModule,
    DialogModule,
    CalendarModule,
    DividerModule,
    FieldsetModule,
    AccordionModule,
    InputMaskModule,
    CardModule,
    NgApexchartsModule,
    SelectButtonModule,
    ImageModule,
    ChipModule,
    ProgressSpinnerModule,
    SkeletonModule,
    InputNumberModule,
    DataViewModule
  ],
  providers: [MessageService, Globals, DatePipe],
  bootstrap: [AppComponent]
})
export class AppModule { }
