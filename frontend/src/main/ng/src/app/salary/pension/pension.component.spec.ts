import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SalaryPensionComponent } from './pension.component';
import { Salary } from '../../model/salary';
import { PensionProjection } from '../../model/pension-projection';
import { Globals } from '../../util/global';
import { environment } from '../../../environments/environment';

describe('SalaryPensionComponent', () => {
  let component: SalaryPensionComponent;
  let fixture: ComponentFixture<SalaryPensionComponent>;
  let http: HttpTestingController;

  const salaryUrl = `${environment.API_URL}/api/v1/history/salary`;
  const pensionUrl = `${environment.API_URL}/api/v1/history/salary/pension`;

  const thisYear = new Date().getFullYear();

  const salaries: Salary[] = [
    {
      id: 2,
      amount: 900000,
      basis: 'MONTHLY',
      currencyId: 'HUF',
      validFrom: `${thisYear - 3}-01-01`,
      validTo: null,
      dependents: 0,
      note: 'Sample employer',
      grossMonthly: 900000,
      netMonthly: 598500
    },
    {
      id: 1,
      amount: 500000,
      basis: 'MONTHLY',
      currencyId: 'HUF',
      validFrom: `${thisYear - 12}-06-01`,
      validTo: `${thisYear - 3}-01-01`,
      dependents: 0,
      note: 'Earlier employer',
      grossMonthly: 500000,
      netMonthly: 332500
    }
  ];

  const projection: PensionProjection = {
    currency: 'HUF',
    currentAge: 35,
    stopWorkingAge: 50,
    retirementAge: 65,
    retirementYear: thisYear + 30,
    gapYears: 15,
    serviceYears: 27,
    scalePct: 65,
    eligible: true,
    partialPension: false,
    degresszio: 'INDEXED',
    degressioLowerThreshold: 372000,
    degressioUpperThreshold: 421000,
    currentGrossMonthly: 900000,
    realWageGrowth: 1.5,
    valorizationUplift: 1.54,
    wageGrowthSensitivity: [
      { realWageGrowth: 0, valorizationUplift: 1, monthlyPension: 244000, selected: false },
      { realWageGrowth: 1.5, valorizationUplift: 1.54, monthlyPension: 376142, selected: true },
      { realWageGrowth: 3, valorizationUplift: 2.36, monthlyPension: 576000, selected: false }
    ],
    nationalAverageGrossMonthly: 754700,
    nationalAveragePension: 449162,
    nationalAverageMultiple: 2.02,
    salaryMultipleOfNationalAverage: 3.19,
    warnings: [],
    scenarios: [
      {
        name: 'Frozen (real)',
        type: 'REAL_FLAT',
        pensionBaseMonthly: 623475,
        degressedBaseMonthly: 578680,
        monthlyPension: 376142,
        annualPension: 4889846,
        finalGrossMonthly: 900000,
        finalNetMonthly: 598500,
        replacementRatePct: 62.85,
        minimumApplied: false,
        byStopAge: [
          { stopAge: 48, serviceYears: 25, scalePct: 63, monthlyPension: 364568 },
          { stopAge: 49, serviceYears: 26, scalePct: 64, monthlyPension: 370355 },
          { stopAge: 50, serviceYears: 27, scalePct: 65, monthlyPension: 376142 }
        ],
        timeline: [
          { scenario: 'Frozen (real)', year: thisYear, age: 35, working: true, counted: true, grossMonthly: 900000, netMonthly: 623475, valorizedNetMonthly: 623475 },
          { scenario: 'Frozen (real)', year: thisYear + 20, age: 55, working: false, counted: false, grossMonthly: 0, netMonthly: 0, valorizedNetMonthly: 0 }
        ]
      },
      {
        name: 'Nominal lock',
        type: 'NOMINAL_LOCK',
        pensionBaseMonthly: 480000,
        degressedBaseMonthly: 459200,
        monthlyPension: 298480,
        annualPension: 3880240,
        finalGrossMonthly: 577000,
        finalNetMonthly: 383705,
        replacementRatePct: 77.79,
        minimumApplied: false,
        byStopAge: [
          { stopAge: 48, serviceYears: 25, scalePct: 63, monthlyPension: 289296 },
          { stopAge: 49, serviceYears: 26, scalePct: 64, monthlyPension: 293888 },
          { stopAge: 50, serviceYears: 27, scalePct: 65, monthlyPension: 298480 }
        ],
        timeline: []
      }
    ]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalaryPensionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService, Globals]
    })
      .compileComponents();

    fixture = TestBed.createComponent(SalaryPensionComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  it('prefills the current gross and the years worked from the salary history', () => {
    http.expectOne(salaryUrl).flush(salaries);

    expect(component.currentGrossMonthly).toBe(900000);
    expect(component.overrideGross).toBe(false);
    expect(component.yearsAlreadyWorked).toBe(12);
  });

  it('asks for the gross by hand when no forint salary is recorded', () => {
    http.expectOne(salaryUrl).flush([{ ...salaries[0], currencyId: 'EUR' }]);

    expect(component.currentGrossMonthly).toBeNull();
    expect(component.overrideGross).toBe(true);
  });

  it('sends the enabled scenarios only', () => {
    http.expectOne(salaryUrl).flush(salaries);
    component.useGrowth = false;
    component.useSteps = false;

    component.calculate();
    const request = http.expectOne(pensionUrl);
    const body = JSON.parse(request.request.body);

    expect(body.scenarios.map((scenario: { type: string }) => scenario.type))
      .toEqual(['REAL_FLAT', 'NOMINAL_LOCK']);
    expect(body.stopWorkingAge).toBe(50);
    expect(body.degresszio).toBe('FROZEN');
    expect(body.realWageGrowth).toBe(1.5);
    expect(body.currentGrossMonthlyOverride).toBeNull();
    request.flush(projection);
  });

  it('builds one chart series per scenario', () => {
    http.expectOne(salaryUrl).flush(salaries);
    component.calculate();
    http.expectOne(pensionUrl).flush(projection);

    expect(component.chart!.series.length).toBe(2);
    expect(component.chart!.series[0].name).toBe('Frozen (real)');
    expect(component.chart!.series[0].path.startsWith('M')).toBe(true);
    expect(component.chart!.firstAge).toBe(48);
    expect(component.chart!.lastAge).toBe(50);
  });

  it('gives each scenario its own colour', () => {
    expect(component.scenarioColor(0)).not.toBe(component.scenarioColor(1));
  });

  it('works out what each extra year of work adds', () => {
    http.expectOne(salaryUrl).flush(salaries);
    component.calculate();
    http.expectOne(pensionUrl).flush(projection);

    expect(component.extraYearRows.length).toBe(2);
    expect(component.extraYearRows[0].fromAge).toBe(48);
    expect(component.extraYearRows[0].gain).toBeCloseTo(5787, 0);
  });

  it('counts the years between stopping work and the pension starting', () => {
    http.expectOne(salaryUrl).flush(salaries);
    component.currentAge = 35;
    component.stopWorkingAge = 50;
    component.retirementAge = 65;

    expect(component.gapYears).toBe(15);
    expect(component.serviceYears).toBe(27);
  });

  it('refuses to calculate without a gross or without a scenario', () => {
    http.expectOne(salaryUrl).flush([]);

    expect(component.canCalculate).toBe(false);

    component.customGross = 800000;
    expect(component.canCalculate).toBe(true);

    component.useFlat = false;
    component.useGrowth = false;
    component.useNominalLock = false;
    expect(component.canCalculate).toBe(false);
  });

  it('refuses to calculate when the pension would start before work stops', () => {
    http.expectOne(salaryUrl).flush(salaries);
    component.stopWorkingAge = 70;
    component.retirementAge = 65;

    expect(component.canCalculate).toBe(false);
    component.calculate();
    http.expectNone(pensionUrl);
  });

  it('reports a failed calculation without clearing the form', () => {
    http.expectOne(salaryUrl).flush(salaries);
    component.calculate();
    http.expectOne(pensionUrl).flush({ error: 'Nope' }, { status: 400, statusText: 'Bad Request' });

    expect(component.projection).toBeNull();
    expect(component.calculating).toBe(false);
  });
});
