import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import { definePreset } from '@primeng/themes';

const EotmPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#faeeda', 100: '#fac775', 200: '#ef9f27', 300: '#ef9f27',
      400: '#ef9f27', 500: '#ef9f27', 600: '#ba7517', 700: '#854f0b',
      800: '#633806', 900: '#412402', 950: '#412402',
    },
  },
  components: {
    menubar: {
      root: {
        background: '#1b1b1b',
        borderColor: '#1b1b1b',
        color: '#f1efe8',
      },
      item: {
        color: '#f1efe8',
        focusColor: '#f1efe8',
        activeColor: '#f1efe8',
        focusBackground: 'rgba(255, 255, 255, 0.08)',
        activeBackground: 'rgba(255, 255, 255, 0.08)',
        icon: {
          color: '#f1efe8',
          focusColor: '#f1efe8',
          activeColor: '#f1efe8',
        },
      },
    },
    togglebutton: {
      colorScheme: {
        light: {
          root: {
            checkedColor: '#f1efe8',
          },
          content: {
            checkedBackground: '#1b1b1b',
          },
          icon: {
            checkedColor: '#f1efe8',
          },
        },
      },
    },
  },
});
import { MessageService } from 'primeng/api';
import { DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MenubarModule } from 'primeng/menubar';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { FileUploadModule } from 'primeng/fileupload';
import { DialogModule } from 'primeng/dialog';
import { DatePickerModule } from 'primeng/datepicker';
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
import { InputNumberModule } from 'primeng/inputnumber';
import { PasswordModule } from 'primeng/password';
import { NgApexchartsModule } from 'ng-apexcharts';

import { Globals } from './util/global';
import { AuthInterceptor } from './util/auth.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptorsFromDi()),
    providePrimeNG({ ripple: true, theme: { preset: EotmPreset } }),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    MessageService,
    Globals,
    DatePipe,
    importProvidersFrom(
      FormsModule, ReactiveFormsModule,
      InputTextModule, ButtonModule, RippleModule, ToastModule, MenubarModule, PanelModule,
      TableModule, SelectModule, TabsModule, TagModule, ToolbarModule, FileUploadModule,
      DialogModule, DatePickerModule, DividerModule, FieldsetModule, AccordionModule,
      InputMaskModule, CardModule, SelectButtonModule, ImageModule, ChipModule,
      ProgressSpinnerModule, SkeletonModule, DataViewModule, InputNumberModule, PasswordModule,
      NgApexchartsModule,
    ),
  ]
};
