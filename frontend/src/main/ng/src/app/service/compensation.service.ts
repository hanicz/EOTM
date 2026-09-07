import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CompensationDraft, CompensationItem, CompensationPackage } from '../model/compensation';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CompensationService {

  private helper = new ResourceHelper();

  private compensationUrl = `${environment.API_URL}/api/v1/history/compensation`;

  constructor(private http: HttpClient) { }

  getCurrentPackage() {
    return this.http.get<CompensationPackage>(this.compensationUrl, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  compare(draft: CompensationDraft) {
    const url = `${this.compensationUrl}/compare`;
    return this.http.post<CompensationPackage>(url, JSON.stringify(this.toDraftPayload(draft)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  create(item: CompensationItem) {
    return this.http.post<CompensationItem>(this.compensationUrl, JSON.stringify(this.toPayload(item)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  update(item: CompensationItem) {
    const url = `${this.compensationUrl}/${item.id}`;
    return this.http.put<CompensationItem>(url, JSON.stringify(this.toPayload(item)), {
      headers: this.helper.getHeadersWithToken()
    });
  }

  deleteByIds(ids: string) {
    const url = `${this.compensationUrl}?ids=${ids}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  private toDraftPayload(draft: CompensationDraft) {
    return {
      label: draft.label,
      baseAmount: draft.baseAmount,
      baseBasis: draft.baseBasis,
      currencyId: draft.currencyId,
      dependents: draft.dependents,
      items: draft.items.map(item => ({
        name: item.name.trim(),
        amountMode: item.amountMode,
        monthlyAmount: item.amountMode === 'MONTHLY_AMOUNT' ? item.monthlyAmount : null,
        percent: item.amountMode === 'PERCENT_OF_ANNUAL' ? item.percent : null,
        taxTreatment: item.taxTreatment
      }))
    };
  }

  private toPayload(item: CompensationItem) {
    return {
      name: item.name.trim(),
      amountMode: item.amountMode,
      monthlyAmount: item.amountMode === 'MONTHLY_AMOUNT' ? item.monthlyAmount : null,
      percent: item.amountMode === 'PERCENT_OF_ANNUAL' ? item.percent : null,
      taxTreatment: item.taxTreatment,
      note: item.note ? item.note : null
    };
  }
}
